package org.sofosim.graph;

import java.util.ArrayList;
import java.util.HashMap;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseMultigraph;

public class GraphRegistry<V> {

	private GraphHandler handler = null;
	/** structure holding references to graph initializers for processing visualization */
	private HashMap<String, Graph<V, ?>> graphCache = new HashMap<String, Graph<V, ?>>();
	private HashMap<String, GraphInitializer> graphRegister = new HashMap<String, GraphInitializer>();
	private ArrayList<GraphInitializer> graphRegisterList = null;
	
	/** listeners for graph changes */
	private ArrayList<GraphChangeListener> changeListener = new ArrayList<GraphChangeListener>();
	
	public GraphRegistry(GraphHandler handler){
		if(handler == null){
			throw new IllegalArgumentException("GraphHandler must not be null!");
		}
		this.handler = handler;
	}
	
	/**
	 * Returns all registered and initialized graphs.
	 * @return
	 */
	protected ArrayList<GraphInitializer> getGraphRegister(){
		return graphRegisterList;
	}
	
	/**
	 * Registers a GraphInitializer with the registry (which 
	 * initializes the graph in the register).
	 * @param graph
	 */
	public void registerGraph(GraphInitializer graph){
		String graphName = checkGraphMode(graph.getGraphName());
		this.graphRegister.put(graphName, graph);
		this.graphRegisterList = new ArrayList<GraphInitializer>(this.graphRegister.values());
		this.graphCache.remove(graphName);
	}
	
	private String checkGraphMode(String graphName){
		if(!handler.multiGraph){
			return handler.UNIFIED_GRAPH;
		}
		return graphName;
	}
	
	public GraphInitializer getGraphInitializer(String graphName){
		graphName = checkGraphMode(graphName);
		return graphRegister.get(graphName);
	}
	
	/**
	 * Returns the Graph specified by the name, else checks if a 
	 * temporary graph has been created and returns that, or 
	 * eventually creates a new graph and registers that.
	 * @param graphName
	 * @return
	 */
	protected Graph getGraph(String graphName){
		graphName = checkGraphMode(graphName);
		if(!graphRegister.containsKey(graphName)){
			if(!graphCache.containsKey(graphName)){
				Graph tempGraph = generateGraph();
				//System.out.println("Created new graph: " + tempGraph);
				graphCache.put(graphName, tempGraph);
			}
			//System.out.println("Returning temporary graph");
			return graphCache.get(graphName);
		}
		GraphInitializer init = graphRegister.get(graphName);
		if(init.getGraph() == null){
			//generate new graph if not existing yet
			return init.setGraph(generateGraph());
		}
		return graphRegister.get(graphName).getGraph();
	}
	
	private Graph generateGraph(){
		return new SparseMultigraph();
	}
	
	public boolean hasGraph(String graphName){
		graphName = checkGraphMode(graphName);
		return graphRegister.containsKey(graphName);
	}
	
	public boolean addVertex(String graphName, V vertex){
		if(graphName == null || graphName.equals("") || vertex == null){
			throw new IllegalArgumentException("Graph name and vertex name need to be specified to add vertex to graph.");
		}
		//System.out.println("Graph: " + getGraph(graphName));
		if(getGraph(graphName) == null){
			throw new RuntimeException("Registration of vertex '" + vertex + "' on graph '" + graphName + "' not possible as graph not registered.");
		}
		notifyChangeListeners(VERTEX_ADDED, vertex);
		return getGraph(graphName).addVertex(vertex);
	}
	
	public boolean removeVertex(String graphName, V vertex){
		if(graphName == null || graphName.equals("") || vertex == null){
			throw new IllegalArgumentException("Graph name and vertex name need to be specified to remove vertex from graph.");
		}
		if(getGraph(graphName) == null){
			throw new RuntimeException("Removal of vertex '" + vertex + "' from graph '" + graphName + "' not possible as graph not registered.");
		}
		notifyChangeListeners(VERTEX_REMOVED, vertex);
		return getGraph(graphName).removeVertex(vertex);
	}
	
	public void registerGraphChangeListener(GraphChangeListener<V> listener){
		changeListener.add(listener);
	}
	
	public void unregisterGraphChangeListener(GraphChangeListener<V> listener){
		changeListener.remove(listener);
	}
	
	private static final String VERTEX_ADDED = "VERTEX_ADDED";
	private static final String VERTEX_REMOVED = "VERTEX_REMOVED";
	
	private void notifyChangeListeners(String action, V vertex){
		if(action.equals(VERTEX_ADDED)){
			for(int i=0; i<changeListener.size(); i++){
				changeListener.get(i).vertexAdded(vertex);
			}
		}
		if(action.equals(VERTEX_REMOVED)){
			for(int i=0; i<changeListener.size(); i++){
				changeListener.get(i).vertexRemoved(vertex);
			}
		}
	}
	
	public void shutdownAllGraphs(){
		for(GraphInitializer init: graphRegister.values()){
			init.shutdownGraph();
		}
	}
	
}
