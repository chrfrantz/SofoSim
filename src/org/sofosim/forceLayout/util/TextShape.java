package org.sofosim.forceLayout.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Point2D;

public class TextShape {

	public final Point2D center;
	public final String text;
	public final Font font;
	public final Color color;	
	
	public TextShape(Point2D center, String text, Font font, Color color) {
		this.center = center;
		this.text = text;
		this.font = font;
		this.color = color;
	}
	
}
