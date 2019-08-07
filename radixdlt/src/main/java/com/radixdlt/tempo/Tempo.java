package com.radixdlt.tempo;

import com.radixdlt.constraintmachine.CMError;
import com.radixdlt.engine.RadixEngineUtils;
import com.radixdlt.engine.RadixEngineUtils.CMAtomConversionException;
import com.radixdlt.utils.UInt384;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.radix.atoms.Atom;
import org.radix.atoms.AtomStore;
import org.radix.atoms.PreparedAtom;
import org.radix.exceptions.ValidationException;
import org.radix.modules.Module;
import org.radix.modules.Modules;
import org.radix.modules.Plugin;
import org.radix.modules.exceptions.ModuleException;
import org.radix.state.State;
import org.radix.state.StateDomain;
import org.radix.time.TemporalVertex;
import org.radix.time.Time;
import org.radix.universe.system.LocalSystem;
import org.radix.validation.ConstraintMachineValidationException;
import org.radix.validation.ValidationHandler;

import com.radixdlt.common.AID;
import com.radixdlt.common.EUID;
import com.radixdlt.common.Pair;
import com.radixdlt.constraintmachine.CMAtom;
import com.radixdlt.crypto.CryptoException;
import com.radixdlt.crypto.ECKeyPair;
import com.radixdlt.crypto.Hash;
import com.radixdlt.ledger.DuplicateIndexablesCreator;
import com.radixdlt.ledger.LedgerCursor;
import com.radixdlt.ledger.LedgerCursor.Type;
import com.radixdlt.ledger.LedgerIndexable;
import com.radixdlt.ledger.LedgerInterface;
import com.radixdlt.ledger.UniqueIndexablesCreator;

public final class Tempo extends Plugin implements LedgerInterface
{
	private UniqueIndexablesCreator uniqueIndexablesCreator;		// TODO not currently used by AtomStore to maintain legacy compatibility
	private DuplicateIndexablesCreator duplicateIndexablesCreator; 	// TODO not currently used by AtomStore to maintain legacy compatibility
	
	private final BlockingQueue<Atom> pollQueue = new LinkedBlockingQueue<Atom>();
	
	@Override
	public List<Class<? extends Module>> getDependsOn()
	{
		List<Class<? extends Module>> dependencies = new ArrayList<>();
		dependencies.add(AtomStore.class);
		return Collections.unmodifiableList(dependencies);
	}

	@Override
	public void start_impl() throws ModuleException
	{ 
		this.pollQueue.clear();
	}

	@Override
	public void stop_impl() throws ModuleException
	{ }

	@Override
	public String getName() 
	{ 
		return "Tempo"; 
	}
	
	@Override
	public void register(UniqueIndexablesCreator uniqueIndexablesCreator)
	{
		this.uniqueIndexablesCreator = Objects.requireNonNull(uniqueIndexablesCreator);
	}

	@Override
	public void register(DuplicateIndexablesCreator duplicateIndexablesCreator)
	{
		this.duplicateIndexablesCreator = Objects.requireNonNull(duplicateIndexablesCreator);
	}

	@Override
	public Atom poll()
	{
		return this.pollQueue.poll();
	}

	@Override
	public Atom poll(long duration, TimeUnit unit) throws InterruptedException
	{
		return this.pollQueue.poll(duration, unit);
	}

	@Override
	public Atom get(AID AID) throws IOException
	{
		return Modules.get(AtomStore.class).getAtom(AID);
	}

	@Override
	public List<Atom> delete(AID AID) throws IOException
	{
		return (List)Modules.get(AtomStore.class).deleteAtoms(AID).getObject();
	}

	/**
	 * TODO make this an AtomStore function that we can execute over a Transaction for safety
	 */
	@Override
	public List<Atom> replace(AID AID, Atom atom) throws IOException
	{
		if (Modules.get(AtomStore.class).hasAtom(atom.getAID()))
			return Collections.EMPTY_LIST;

		List<Atom> deletedAtoms = (List)Modules.get(AtomStore.class).deleteAtoms(AID).getObject();
		
		try
		{
			attestTo(atom);
		}
		catch (ValidationException | CryptoException ex)
		{
			throw new IOException(ex);
		}

		// TODO super hack, remove later! 
		final CMAtom cmAtom;
		try {
			cmAtom = RadixEngineUtils.toCMAtom(atom);
		} catch (CMAtomConversionException e) {
			throw new IllegalStateException();
		}
		Modules.get(AtomStore.class).storeAtom(new PreparedAtom(cmAtom, UInt384.ONE));

		return deletedAtoms;
	}

	@Override
	public boolean store(Atom atom) throws IOException
	{
		if (Modules.get(AtomStore.class).hasAtom(atom.getAID()))
			return false;
		
		try
		{
			attestTo(atom);
		}
		catch (ValidationException | CryptoException ex)
		{
			throw new IOException(ex);
		}

		final CMAtom cmAtom;
		try {
			cmAtom = RadixEngineUtils.toCMAtom(atom);
		} catch (CMAtomConversionException e) {
			throw new IllegalStateException();
		}
		return Modules.get(AtomStore.class).storeAtom(new PreparedAtom(cmAtom, UInt384.ONE)).isCompleted();
	}

	@Override
	public void resolve(Consumer<Atom> callback, Atom... atoms)
	{
		// TODO Auto-generated method stub
	}

	@Override
	public LedgerCursor search(Type type, LedgerIndexable indexable, SearchMode mode) throws IOException
	{
		return Modules.get(AtomStore.class).search(type, indexable, mode);
	}
	
	// TODO simple temporary function for attestation within this basic Tempo stub
	private void attestTo(Atom atom) throws IOException, ValidationException, CryptoException
	{
		TemporalVertex existingNIDVertex = atom.getTemporalProof().getVertexByNID(LocalSystem.getInstance().getNID());

		if (existingNIDVertex != null)
		{
			if (existingNIDVertex.getClock() > LocalSystem.getInstance().getClock().get())
				LocalSystem.getInstance().set(existingNIDVertex.getClock(), existingNIDVertex.getCommitment(), atom.getTimestamp());

			return;
		}

		Pair<Long, Hash> clockAndCommitment = LocalSystem.getInstance().update(atom.getAID(), Time.currentTimestamp());
		
		if (atom.getTemporalProof().isEmpty() == false)
		{
			TemporalVertex previousVertex = null;
			for (TemporalVertex vertex : atom.getTemporalProof().getVertices())
			{
				if (vertex.getNIDS().contains(LocalSystem.getInstance().getNID()))
				{
					previousVertex = vertex;
					break;
				}
				else if (previousVertex == null)
					previousVertex = vertex;
			}

			ECKeyPair nodeKey = LocalSystem.getInstance().getKeyPair();
			TemporalVertex vertex = new TemporalVertex(nodeKey.getPublicKey(),
														clockAndCommitment.getFirst(), Time.currentTimestamp(),
														clockAndCommitment.getSecond(),
	 					  							   	previousVertex.getHID(), Collections.EMPTY_SET);
			atom.getTemporalProof().add(vertex, nodeKey);
		}
		else
		{
			ECKeyPair nodeKey = LocalSystem.getInstance().getKeyPair();
			TemporalVertex vertex = new TemporalVertex(nodeKey.getPublicKey(),
														clockAndCommitment.getFirst(), Time.currentTimestamp(),
														clockAndCommitment.getSecond(),
 					  							   	   	EUID.ZERO, Collections.EMPTY_SET);
			atom.getTemporalProof().add(vertex, nodeKey);
		}

		atom.getTemporalProof().setState(StateDomain.VALIDATION, new State(State.COMPLETE));
	}
}