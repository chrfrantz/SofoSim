package org.sofosim.forceLayout.transformers;

import java.awt.geom.Point2D;
import java.util.HashSet;
import org.apache.commons.collections15.Transformer;
import org.sofosim.clustering.VertexPoint3D;
import org.sofosim.environment.GridSim;
import org.sofosim.forceLayout.ForceDirectedLayout;
import org.sofosim.util.ProximityCalculator;

public class ForceLocationVertexTransformer implements Transformer<VertexPoint3D<String>,Point2D>{

	private GridSim sim = null;
	protected Integer graphXSize = null;
	protected Integer graphYSize = null;
	
	HashSet<Point2D> points = new HashSet<Point2D>();
	ProximityCalculator proxCalc = null;
	ForceDirectedLayout layout = null;
	
	
	/*public void setLayout(ForceDirectedLayout layout){
		this.sim = layout.sim;
		this.graphXSize = sim.GRID_WIDTH;
		this.graphYSize = sim.GRID_HEIGHT;
	}*/
	
	public ForceLocationVertexTransformer(ForceDirectedLayout layout){
		this.layout = layout;
		this.sim = layout.sim;
		this.graphXSize = sim.GRID_WIDTH;
		this.graphYSize = sim.GRID_HEIGHT;
		proxCalc = new ProximityCalculator(graphXSize, graphYSize, sim.TOROIDAL);
	}
	
	@Override
	public synchronized Point2D transform(VertexPoint3D<String> node) {
		if(sim == null) throw new RuntimeException("Force Layout has not been registered with ForceLocationVertex.");
		Point2D pt = null;
		if(layout.startWithRandomPositions){
			boolean valid = false;
			while(!valid){
				pt = new Point2D.Double(sim.random().nextInt(graphXSize), sim.random().nextInt(graphYSize));
				boolean foundCloseOne = false;
				/*for(Point2D pts: points){
					if(proxCalc.calculateAbsoluteDistance(pt, pts).getLength() < ForceDirectedLayout.vertexDiameter){
						foundCloseOne = true;
					}
				}*/
				if(!foundCloseOne){
					valid = true;
				}
				//System.out.println("Generated point " + pt + " for " + node);
			}
			points.add(pt);
			return pt;
		} else {
			//provide infinite coordinate to identify that none is set
			return new Point2D.Double(Double.MAX_VALUE, Double.MAX_VALUE);
		}
	}

}
