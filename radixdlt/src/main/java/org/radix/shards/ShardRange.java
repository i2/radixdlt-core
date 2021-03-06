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

package org.radix.shards;

import com.radixdlt.serialization.DsonOutput;
import com.radixdlt.serialization.SerializerConstants;
import com.radixdlt.serialization.SerializerDummy;
import com.radixdlt.serialization.SerializerId2;
import com.radixdlt.serialization.DsonOutput.Output;
import org.radix.utils.Range;

import com.fasterxml.jackson.annotation.JsonProperty;

@SerializerId2("radix.shards.range")
public final class ShardRange extends Range<Long>
{
	// Placeholder for the serializer ID
	@JsonProperty(SerializerConstants.SERIALIZER_NAME)
	@DsonOutput(Output.ALL)
	private SerializerDummy serializer = SerializerDummy.DUMMY;

	ShardRange()
	{
		// For serializer
	}

	public ShardRange(long low, long high)
	{
		super(low, high);
	}

	@JsonProperty("low")
	@DsonOutput(Output.ALL)
	long getJsonLow() {
		return this.getLow();
	}

	@JsonProperty("high")
	@DsonOutput(Output.ALL)
	long getJsonHigh() {
		return this.getHigh();
	}

	@JsonProperty("low")
	void setJsonLow(long low) {
		this.setLow(low);
	}

	@JsonProperty("high")
	void setJsonHigh(long high) {
		this.setHigh(high);
	}
}
