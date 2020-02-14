/*
 * (C) Copyright 2020 Radix DLT Ltd
 *
 * Radix DLT Ltd licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the License.
 */

package com.radixdlt.consensus;

import com.google.inject.Inject;
import org.radix.logging.Logger;
import org.radix.logging.Logging;

import java.util.Objects;

/**
 * A three-chain BFT
 */
public final class ChainedBFT {
	private static final Logger log = Logging.getLogger("bft");

	private final Pacemaker pacemaker;

	@Inject
	public ChainedBFT(
		EventCoordinator eventCoordinator,
		Network network,
		Pacemaker pacemaker
	) {
		Objects.requireNonNull(eventCoordinator);
		Objects.requireNonNull(pacemaker);

		this.pacemaker = pacemaker;

		pacemaker.addCallback(v -> eventCoordinator.newRound());
		network.addCallback(proposal -> eventCoordinator.processProposal(proposal));
	}

	// TODO: Add cleanup
	public void start() {
		this.pacemaker.start();
	}
}