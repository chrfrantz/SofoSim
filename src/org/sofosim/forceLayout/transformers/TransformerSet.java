package org.sofosim.forceLayout.transformers;

import java.awt.Paint;
import java.awt.geom.Point2D;
import org.apache.commons.collections15.Transformer;
import org.sofosim.clustering.VertexPoint3D;
import org.sofosim.environment.GridSim;
import edu.uci.ics.jung.algorithms.layout.Layout;

public abstract class TransformerSet {

	protected GridSim sim = null;
	//protected Layout layout = null;
	
	public TransformerSet(GridSim sim){
		this.sim = sim;
	}
	
	public abstract Transformer<VertexPoint3D<String>, Point2D> getLocationVertexTransformer(Layout layout);
	
	public abstract Transformer<VertexPoint3D<String>, Paint> getFillPaintTransformer(Layout layout);
	
	public abstract Transformer<VertexPoint3D<String>, String> getVertexLabelTransformer(Layout layout);
	
	public abstract Transformer<VertexPoint3D<String>, String> getVertexToolTipTransformer(Layout layout);
	
	public abstract Transformer<VertexPoint3D<String>, Paint> getVertexColorTransformer(Layout layout);
	
}
