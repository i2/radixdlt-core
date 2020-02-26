/*
 *  (C) Copyright 2020 Radix DLT Ltd
 *
 *  Radix DLT Ltd licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except in
 *  compliance with the License.  You may obtain a copy of the
 *  License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *  either express or implied.  See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package com.radixdlt.consensus;

import java.util.Objects;

import com.radixdlt.crypto.CryptoException;
import com.radixdlt.crypto.DefaultSignatures;
import com.radixdlt.crypto.ECKeyPair;
import com.radixdlt.crypto.Signature;

/**
 * Manages safety of the protocol.
 * TODO: Add storage of private key of node here
 */
public final class SafetyRules {

	private final ECKeyPair keyPair;

	public SafetyRules(ECKeyPair keyPair) {
		this.keyPair = Objects.requireNonNull(keyPair);
	}

	public Vote vote(Vertex vertex) {
		if (!this.keyPair.canProduceSignatureForScheme(DefaultSignatures.emptySignatures().signatureScheme())) {
			throw new IllegalStateException("Cannot produced a signature using the provided signing key.");
		}
		try {
			Signature signature = this.keyPair.sign(vertex.hash());
			return new Vote(vertex.getRound(), vertex.hash(), signature, this.keyPair.getPublicKey());
		} catch (CryptoException e) {
			throw new IllegalStateException("Should always be able to sign", e);
		}
	}
}