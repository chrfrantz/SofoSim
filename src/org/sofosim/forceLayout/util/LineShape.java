package org.sofosim.forceLayout.util;

import java.awt.Color;
import java.awt.Stroke;
import java.awt.geom.Point2D;

public class LineShape {

	public final Point2D startingPoint;
	public final Point2D endPoint;
	public final boolean attracting;
	public final Color color;
	public final Stroke stroke;
	public final int hashCode;

	public LineShape(Integer hashCode, Point2D startingPoint, Point2D endPoint, boolean attracting, Color color, Stroke stroke) {
		this.startingPoint = startingPoint;
		this.endPoint = endPoint;
		this.attracting = attracting;
		this.color = color;
		this.hashCode = hashCode;
		this.stroke = stroke;
	}
	
	@Override
	public int hashCode(){
		return hashCode;
	}

	@Override
	public String toString() {
		return "LineShape [startingPoint=" + startingPoint + ", endPoint="
				+ endPoint + ", attracting=" + attracting + ", color=" + color
				+ ", stroke=" + stroke + ", hashCode=" + hashCode + "]";
	}
	
}
