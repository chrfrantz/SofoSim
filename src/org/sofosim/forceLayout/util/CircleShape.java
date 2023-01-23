package org.sofosim.forceLayout.util;

import java.awt.Color;
import java.awt.geom.Point2D;

public class CircleShape {

	public final Point2D center;
	public final int diameter;
	public boolean filled = false;
	public Color color;
	
	public CircleShape(Point2D center, int diameter) {
		this.center = new Point2D.Double(center.getX() - diameter/(float)2, center.getY() - diameter/(float)2);
		this.diameter = diameter;
	}
	
	public CircleShape(Point2D center, int diameter, boolean filled, Color col){
		this(center, diameter);
		this.filled = filled;
		this.color = col;
	}
	
	
}
