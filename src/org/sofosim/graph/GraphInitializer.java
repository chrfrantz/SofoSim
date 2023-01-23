package org.sofosim.graph;

import javax.swing.JComponent;
import javax.swing.JFrame;
import org.sofosim.environment.GridSim;
import org.sofosim.forceLayout.transformers.TransformerSet;
import org.sofosim.planes.SocialPlaneInitializer;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.Graph;

public abstract class GraphInitializer {

	protected Layout layout = null;
	protected GridSim sim = null;
	protected JComponent glassPane = null;
	protected JFrame graphFrame = null;
	protected Graph graph = null;
	protected boolean enabled = true;
	protected boolean initialized = false;
	protected SocialPlaneInitializer planeInitializer = null;
	
	/*public GraphInitializer(Layout layout, GridSim sim){
		this.layout = layout;
		this.sim = sim;
	}*/
	public GraphInitializer(){
		
	}
	
	protected TransformerSet transformers = null;
	
	public void registerTransformers(TransformerSet transformers){
		this.transformers = transformers;
		System.out.println("Graph Initializer: Registered Graph Transformers.");
	}
	
	public abstract String getGraphName();
	
	public void activate(){
		enabled = true;
	}
	
	public void disable(){
		enabled = false;
	}
	
	public boolean enabled(){
		return enabled;
	}
	
	public boolean isInitialized(){
		return initialized;
	}
	
	public JFrame getFrame(){
		return graphFrame;
	}
	
	protected Graph setGraph(Graph graph){
		this.graph = graph;
		return this.graph;
	}
	
	public Graph getGraph(){
		return graph;
	}
	
	public JComponent getGlassPane(){
		return glassPane;
	}
	
	public Layout getLayout(){
		return layout;
	}
	
	public void registerPlanes(SocialPlaneInitializer planeInitializer){
		this.planeInitializer = planeInitializer;
		//System.out.print("Set social planes: ");
		//System.out.print(this.planeInitializer != null);
		//System.out.println(": " + this.planeInitializer);
	}
	
	public abstract void initializeGraph(Graph graph, GridSim sim);
	
	public abstract void visualizeGraph();
	
	public abstract void shutdownGraph();
	
}
