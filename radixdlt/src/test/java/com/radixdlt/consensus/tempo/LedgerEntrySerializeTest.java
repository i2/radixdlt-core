package com.radixdlt.consensus.tempo;

import com.radixdlt.common.AID;
import com.radixdlt.crypto.Hash;
import com.radixdlt.store.LedgerEntry;
import org.radix.serialization.SerializeMessageObject;

public class LedgerEntrySerializeTest extends SerializeMessageObject<LedgerEntry> {
	public LedgerEntrySerializeTest() {
		super(LedgerEntry.class, () -> new LedgerEntry(
			"{\"test\":\"test\"}".getBytes(),
			AID.from(Hash.ZERO_HASH.toByteArray())
		));
	}
}