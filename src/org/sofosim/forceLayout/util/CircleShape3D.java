package org.sofosim.forceLayout.util;

import java.awt.Color;
import java.awt.geom.Point2D;
import javax.vecmath.Point3d;

public class CircleShape3D {

	public final Point3d center;
	public final int diameter;
	public boolean filled = false;
	public Color color;
	
	public CircleShape3D(Point3d center, int diameter) {
		this.center = center;//new Point3d(center.getX() - diameter/(float)2, center.getY() - diameter/(float)2, center.getZ() - diameter/(float)2);
		this.diameter = diameter;
	}
	
	public CircleShape3D(Point3d center, int diameter, boolean filled, Color col){
		this(center, diameter);
		this.filled = filled;
		this.color = col;
	}
	
	public CircleShape toCircleShape(){
		return new CircleShape(new Point2D.Double((float)center.x, (float)center.y), diameter, filled, color);
	}

	@Override
	public String toString() {
		return "CircleShape3D [center=" + center + ", diameter=" + diameter
				+ ", filled=" + filled + ", color=" + color + "]";
	}
	
}
