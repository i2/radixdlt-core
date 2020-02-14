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

import com.radixdlt.common.Atom;
import java.util.List;

/**
 * This is a temporary, rough interface representing the application-side part of the currently developing app/consensus interface.
 */
public interface MemPool {
	void addAtom(Atom atom);
	void removeCommittedAtom(Atom atom);
	void removeRejectedAtom(Atom atom);
	List<Atom> getAtoms(int count);
}