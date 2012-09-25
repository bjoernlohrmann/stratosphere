/***********************************************************************************************************************
 *
 * Copyright (C) 2010 by the Stratosphere project (http://stratosphere.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 **********************************************************************************************************************/

package eu.stratosphere.nephele.streaming;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import eu.stratosphere.nephele.io.channels.ChannelID;
import eu.stratosphere.nephele.types.Tag;

public final class StreamingTag implements Tag {

	private final ChannelID sourceChannelID;

	private long timestamp = 0L;

	public StreamingTag(final ChannelID sourceChannelID) {

		if (sourceChannelID == null) {
			throw new IllegalArgumentException("sourceChannelID must not be null");
		}

		this.sourceChannelID = sourceChannelID;
	}

	/**
	 * Default constructor for deserialization.
	 */
	public StreamingTag() {
		this.sourceChannelID = new ChannelID();
	}

	public void setTimestamp(final long timestamp) {
		this.timestamp = timestamp;
	}

	public ChannelID getSourceChannelID() {
		return this.sourceChannelID;
	}

	public long getTimestamp() {
		return this.timestamp;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void write(final DataOutput out) throws IOException {
		// TODO Auto-generated method stub

		this.sourceChannelID.write(out);
		out.writeLong(this.timestamp);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void read(final DataInput in) throws IOException {

		this.sourceChannelID.read(in);
		this.timestamp = in.readLong();
	}
}
