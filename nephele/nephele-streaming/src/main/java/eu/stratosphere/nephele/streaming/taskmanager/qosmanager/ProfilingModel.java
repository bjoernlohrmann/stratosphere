package eu.stratosphere.nephele.streaming.taskmanager.qosmanager;

import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import eu.stratosphere.nephele.executiongraph.ExecutionVertexID;
import eu.stratosphere.nephele.io.DistributionPattern;
import eu.stratosphere.nephele.io.channels.ChannelID;
import eu.stratosphere.nephele.streaming.message.StreamChainAnnounce;
import eu.stratosphere.nephele.streaming.message.profiling.ChannelLatency;
import eu.stratosphere.nephele.streaming.message.profiling.OutputChannelStatistics;
import eu.stratosphere.nephele.streaming.message.profiling.TaskLatency;
import eu.stratosphere.nephele.streaming.taskmanager.qosmodel.EdgeCharacteristics;
import eu.stratosphere.nephele.streaming.taskmanager.qosmodel.ProfilingEdge;
import eu.stratosphere.nephele.streaming.taskmanager.qosmodel.ProfilingGroupVertex;
import eu.stratosphere.nephele.streaming.taskmanager.qosmodel.ProfilingSequence;
import eu.stratosphere.nephele.streaming.taskmanager.qosmodel.ProfilingVertex;
import eu.stratosphere.nephele.streaming.taskmanager.qosmodel.VertexLatency;

public class ProfilingModel {
	private static final Log LOG = LogFactory.getLog(ProfilingModel.class);

	private ProfilingSequence profilingGroupSequence;

	private HashMap<ExecutionVertexID, VertexLatency> vertexLatencies;

	private HashMap<ChannelID, EdgeCharacteristics> edgeCharacteristics;

	private int noOfProfilingSequences;

	public ProfilingModel(ProfilingSequence profilingGroupSequence) {
		this.profilingGroupSequence = profilingGroupSequence;
		this.initMaps();
		this.countProfilingSequences();
	}

	private void countProfilingSequences() {
		this.noOfProfilingSequences = -1;
		for (ProfilingGroupVertex groupVertex : this.profilingGroupSequence
				.getSequenceVertices()) {
			if (this.noOfProfilingSequences == -1) {
				this.noOfProfilingSequences = groupVertex.getGroupMembers()
						.size();
			} else if (groupVertex.getBackwardEdge().getDistributionPattern() == DistributionPattern.BIPARTITE) {
				this.noOfProfilingSequences *= groupVertex.getGroupMembers()
						.size();
			}
		}
		LOG.info(String.format("Profiling model with %d profiling sequences",
				this.noOfProfilingSequences));
	}

	private void initMaps() {
		this.vertexLatencies = new HashMap<ExecutionVertexID, VertexLatency>();
		this.edgeCharacteristics = new HashMap<ChannelID, EdgeCharacteristics>();

		for (ProfilingGroupVertex groupVertex : this.profilingGroupSequence
				.getSequenceVertices()) {
			for (ProfilingVertex vertex : groupVertex.getGroupMembers()) {
				VertexLatency vertexLatency = new VertexLatency(vertex);
				vertex.setVertexLatency(vertexLatency);
				this.vertexLatencies.put(vertex.getID(), vertexLatency);
				for (ProfilingEdge edge : vertex.getForwardEdges()) {
					EdgeCharacteristics currentEdgeChars = new EdgeCharacteristics(
							edge);
					edge.setEdgeCharacteristics(currentEdgeChars);
					this.edgeCharacteristics.put(edge.getSourceChannelID(),
							currentEdgeChars);
				}
			}
		}
	}

	public void refreshChannelLatency(long timestamp, ChannelLatency channelLatency) {
		this.edgeCharacteristics.get(channelLatency.getSourceChannelID())
				.addLatencyMeasurement(timestamp,
						channelLatency.getChannelLatency());
	}

	public void refreshTaskLatency(long timestamp, TaskLatency taskLatency) {
		this.vertexLatencies.get(taskLatency.getVertexID())
				.addLatencyMeasurement(timestamp, taskLatency.getTaskLatency());
	}

	public void refreshOutputChannelStatistics(long timestamp,
			OutputChannelStatistics channelStats) {
		
		this.edgeCharacteristics.get(channelStats.getSourceChannelID())
				.addOutputChannelStatisticsMeasurement(timestamp, channelStats);
	}

	public ProfilingSequenceSummary computeProfilingSummary() {
		return new ProfilingSequenceSummary(this.profilingGroupSequence);
	}

	public void announceStreamingChain(StreamChainAnnounce announce) {

		ProfilingVertex currentVertex = this.vertexLatencies.get(
				announce.getChainBeginVertexID()).getVertex();

		while (!currentVertex.getID().equals(announce.getChainEndVertexID())) {
			ProfilingEdge forwardEdge = currentVertex.getForwardEdges().get(0);
			forwardEdge.getEdgeCharacteristics().setIsInChain(true);
			currentVertex = forwardEdge.getTargetVertex();
		}
	}

	public ProfilingSequence getProfilingSequence() {
		return this.profilingGroupSequence;
	}
}
