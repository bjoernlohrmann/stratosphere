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

package eu.stratosphere.nephele.streaming.listeners;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import eu.stratosphere.nephele.configuration.Configuration;
import eu.stratosphere.nephele.execution.Mapper;
import eu.stratosphere.nephele.io.GateID;
import eu.stratosphere.nephele.io.channels.AbstractOutputChannel;
import eu.stratosphere.nephele.io.channels.ChannelID;
import eu.stratosphere.nephele.io.channels.bytebuffered.AbstractByteBufferedOutputChannel;
import eu.stratosphere.nephele.streaming.StreamingTaskManagerPlugin;
import eu.stratosphere.nephele.streaming.actions.AbstractAction;
import eu.stratosphere.nephele.streaming.actions.ConstructStreamChainAction;
import eu.stratosphere.nephele.streaming.actions.LimitBufferSizeAction;
import eu.stratosphere.nephele.streaming.chaining.StreamChain;
import eu.stratosphere.nephele.streaming.types.profiling.ChannelLatency;
import eu.stratosphere.nephele.streaming.types.profiling.ChannelThroughput;
import eu.stratosphere.nephele.streaming.types.profiling.OutputBufferLatency;
import eu.stratosphere.nephele.streaming.wrappers.StreamingInputGate;
import eu.stratosphere.nephele.streaming.wrappers.StreamingOutputGate;
import eu.stratosphere.nephele.types.Record;

public final class StreamListener {

	private static final Log LOG = LogFactory.getLog(StreamListener.class);

	private final Configuration taskConfiguration;

	private StreamListenerContext listenerContext = null;

	private Map<GateID, StreamingOutputGate<? extends Record>> outputGateMap = new HashMap<GateID, StreamingOutputGate<? extends Record>>();

	private Map<ChannelID, AbstractOutputChannel<? extends Record>> outputChannelMap;

	private TaskLatencyReporter taskLatencyReporter;

	public StreamListener(final Configuration taskConfiguration) {

		if (taskConfiguration == null) {
			throw new IllegalArgumentException(
					"Argument taskConfiguration must not be null");
		}

		this.taskConfiguration = taskConfiguration;
	}

	/**
	 * Initializes the stream listener by retrieving the listener context from
	 * the task manager plugin.
	 */
	public void init() {

		final String listenerKey = this.taskConfiguration.getString(
				StreamListenerContext.CONTEXT_CONFIGURATION_KEY, null);

		if (listenerKey == null) {
			throw new RuntimeException(
					"Stream listener is unable to retrieve context key");
		}

		this.listenerContext = StreamingTaskManagerPlugin
				.getStreamingListenerContext(listenerKey);

		this.initOutputChannelMap();

		this.taskLatencyReporter = new TaskLatencyReporter(this.listenerContext);
	}

	private void initOutputChannelMap() {
		final Map<ChannelID, AbstractOutputChannel<? extends Record>> tmpMap = new HashMap<ChannelID, AbstractOutputChannel<? extends Record>>();

		final Iterator<StreamingOutputGate<? extends Record>> it = this.outputGateMap
				.values().iterator();
		while (it.hasNext()) {
			final StreamingOutputGate<? extends Record> outputGate = it.next();
			final int numberOfOutputChannels = outputGate
					.getNumberOfOutputChannels();
			for (int i = 0; i < numberOfOutputChannels; ++i) {
				final AbstractOutputChannel<? extends Record> outputChannel = outputGate
						.getOutputChannel(i);
				tmpMap.put(outputChannel.getID(), outputChannel);
			}
		}

		this.outputChannelMap = Collections.unmodifiableMap(tmpMap);
	}

	public void recordEmitted(final Record record) throws InterruptedException {

		this.taskLatencyReporter.processRecordEmitted();

		// Finally, check for pending actions
		this.checkForPendingActions();
	}

	public void recordReceived(final Record record) {
		this.taskLatencyReporter.processRecordReceived();
	}

	public void reportChannelThroughput(final ChannelID sourceChannelID,
			final double throughput) {
		this.listenerContext.getProfilingReporter().addToNextReport(
				new ChannelThroughput(sourceChannelID, throughput));
	}

	public void reportBufferLatency(final ChannelID sourceChannelID,
			final double bufferLatency) {
		this.listenerContext.getProfilingReporter().addToNextReport(
				new OutputBufferLatency(sourceChannelID, bufferLatency));
	}

	public void reportChannelLatency(ChannelLatency channelLatency) {
		this.listenerContext.getProfilingReporter().addToNextReport(
				channelLatency);
	}

	private void checkForPendingActions() throws InterruptedException {

		LinkedBlockingQueue<AbstractAction> pendingActions = this.listenerContext
				.getPendingActionsQueue();

		AbstractAction action;

		while ((action = pendingActions.poll()) != null) {
			if (action instanceof LimitBufferSizeAction) {
				this.limitBufferSize((LimitBufferSizeAction) action);
			} else if (action instanceof ConstructStreamChainAction) {
				this.constructStreamChain((ConstructStreamChainAction) action);
			} else {
				LOG.error("Ignoring unknown action of type "
						+ action.getClass());
			}
		}
	}

	private void constructStreamChain(final ConstructStreamChainAction csca)
			throws InterruptedException {

		final StreamChain streamChain = this.listenerContext
				.constructStreamChain(csca.getVertexIDs());
		streamChain.waitUntilFlushed();
	}

	private void limitBufferSize(final LimitBufferSizeAction bsla) {

		final ChannelID sourceChannelID = bsla.getSourceChannelID();
		final int bufferSize = bsla.getBufferSize();

		final AbstractOutputChannel<? extends Record> outputChannel = this.outputChannelMap
				.get(sourceChannelID);
		if (outputChannel == null) {
			LOG.error("Cannot find output channel with ID " + sourceChannelID);
			return;
		}

		if (!(outputChannel instanceof AbstractByteBufferedOutputChannel)) {
			LOG.error("Output channel with ID " + sourceChannelID
					+ " is not a byte-buffered channel");
			return;
		}

		final AbstractByteBufferedOutputChannel<? extends Record> byteBufferedOutputChannel = (AbstractByteBufferedOutputChannel<? extends Record>) outputChannel;

		LOG.info("Setting buffer size limit of output channel "
				+ sourceChannelID + " to " + bufferSize + " bytes");
		byteBufferedOutputChannel.limitBufferSize(bufferSize);
	}

	public void registerOutputGate(
			final StreamingOutputGate<? extends Record> outputGate) {

		this.outputGateMap.put(outputGate.getGateID(), outputGate);
	}

	public <I extends Record, O extends Record> void registerMapper(
			final Mapper<I, O> mapper, final StreamingInputGate<I> input,
			final StreamingOutputGate<O> output) {

		this.listenerContext.registerMapper(mapper, input, output);
	}

	public StreamListenerContext getContext() {
		return this.listenerContext;
	}
}
