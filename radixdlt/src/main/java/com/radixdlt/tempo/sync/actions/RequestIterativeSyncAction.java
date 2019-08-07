package com.radixdlt.tempo.sync.actions;

import com.radixdlt.tempo.sync.SyncAction;
import org.radix.network.peers.Peer;

public class RequestIterativeSyncAction implements SyncAction {
	private final Peer peer;

	public RequestIterativeSyncAction(Peer peer) {
		this.peer = peer;
	}

	public Peer getPeer() {
		return peer;
	}
}
