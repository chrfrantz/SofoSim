package org.sofosim.forceLayout.transformers;

import org.sofosim.environment.GridSim;
import org.sofosim.graph.GraphHandler;
import edu.uci.ics.jung.algorithms.layout.Layout;

public class AbstractTransformer {
	
	protected Layout layout = null;
	protected GridSim sim = null;
	protected Integer graphXSize = GraphHandler.graphXSize;
	protected Integer graphYSize = GraphHandler.graphYSize;
	
	//offset from margins
	protected Double defaultOffset = 15.0;
	//offset of points from each other to visualize number of agents on same coordinate
	protected Double collisionOffset = 1.0;
	
	public AbstractTransformer(Layout layout, GridSim sim){
		this.layout = layout;
		this.sim = sim;
	}
	
}
