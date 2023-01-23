package org.sofosim.forceLayout.util;

import java.awt.geom.Point2D;

public class OvalShape {

	public final Point2D center;
	public final int xLength;
	public final int yLength;
	
	public OvalShape(Point2D center, int xLength, int yLength){
		this.center = new Point2D.Double(center.getX() - xLength/(float)2, center.getY() - yLength/(float)2);
		this.xLength = xLength;
		this.yLength = yLength;
	}
	
}
