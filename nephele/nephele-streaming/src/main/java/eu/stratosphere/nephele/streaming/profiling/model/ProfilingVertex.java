package eu.stratosphere.nephele.streaming.profiling.model;

import java.util.ArrayList;
import java.util.List;

import eu.stratosphere.nephele.executiongraph.ExecutionVertexID;
import eu.stratosphere.nephele.instance.InstanceConnectionInfo;

public class ProfilingVertex {

	private final ExecutionVertexID vertexID;

	private InstanceConnectionInfo profilingDataSource;

	/**
	 * A list of edges originating from this vertex.
	 */
	private List<ProfilingEdge> forwardEdges = new ArrayList<ProfilingEdge>();

	/**
	 * A list of edges arriving at this vertex.
	 */
	private List<ProfilingEdge> backwardEdges = new ArrayList<ProfilingEdge>();

	public ProfilingVertex(ExecutionVertexID vertexID) {
		this.vertexID = vertexID;
	}

	public ExecutionVertexID getID() {
		return vertexID;
	}

	public List<ProfilingEdge> getForwardEdges() {
		return forwardEdges;
	}

	public void addForwardEdge(ProfilingEdge forwardEdge) {
		this.forwardEdges.add(forwardEdge);
	}

	public List<ProfilingEdge> getBackwardEdges() {
		return backwardEdges;
	}

	public void addBackwardEdge(ProfilingEdge backwardEdge) {
		this.backwardEdges.add(backwardEdge);
	}

	public InstanceConnectionInfo getProfilingDataSource() {
		return profilingDataSource;
	}

	public void setProfilingDataSource(InstanceConnectionInfo profilingDataSource) {
		this.profilingDataSource = profilingDataSource;
	}
}
