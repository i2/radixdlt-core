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

package org.radix.network2.transport;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import nl.jqno.equalsverifier.EqualsVerifier;

public class DynamicTransportMetadataTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void equalsContract() {
	    EqualsVerifier.forClass(DynamicTransportMetadata.class).verify();
	}

	@Test
	public void testFromMap() {
		Map<String, Supplier<String>> metadata = new HashMap<>();
		metadata.put("a", () -> "b");
		DynamicTransportMetadata dtm = DynamicTransportMetadata.from(metadata);

		assertThat(dtm.get("a"), equalTo("b"));
		assertThat(dtm.get("b"), nullValue());
	}

	@Test
	public void testFromImmutableMap() {
		DynamicTransportMetadata dtm = DynamicTransportMetadata.from(
			ImmutableMap.of("a", () -> "b")
		);

		assertThat(dtm.get("a"), equalTo("b"));
		assertThat(dtm.get("b"), nullValue());
	}

	@Test
	public void testOfTwoArgs() {
		DynamicTransportMetadata dtm = DynamicTransportMetadata.of("a", () -> "b");

		assertThat(dtm.get("a"), equalTo("b"));
		assertThat(dtm.get("b"), nullValue());
	}

	@Test
	public void testOfFourArgs() {
		DynamicTransportMetadata dtm = DynamicTransportMetadata.of("a", () -> "b", "c", () -> "d");

		assertThat(dtm.get("a"), equalTo("b"));
		assertThat(dtm.get("c"), equalTo("d"));
		assertThat(dtm.get("e"), nullValue());
	}

	@Test
	public void testToString() {
		DynamicTransportMetadata dtm = DynamicTransportMetadata.of("a", () -> "b", "c", () -> "d");

		assertThat(dtm.toString(), containsString("a=b"));
		assertThat(dtm.toString(), containsString("c=d"));
	}
}
