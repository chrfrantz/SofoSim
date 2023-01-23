package org.sofosim.clustering;

import java.awt.geom.Point2D;

public class CentroidWithMinMaxValues {

	public final Point2D centroid;
	public final float xMinValue;
	public final float xMaxValue;
	public final float yMinValue;
	public final float yMaxValue;
	public final float xRadius;
	public final float yRadius;
	public final boolean xSplit;
	public final boolean ySplit;
	/** Indicates if the majority of cluster members is on the left, else right (only valid if xSplit == true) */
	public final boolean xMajorityOnLeft;
	/** Indicates if the majority of cluster members is on the top, else bottom (only valid if ySplit == true) */
	public final boolean yMajorityOnTop;
	
	public CentroidWithMinMaxValues(Point2D centroid, float xMinValue, float xMaxValue, float yMinValue, float yMaxValue, float xRadius, float yRadius, boolean xSplit, boolean xMajorityOnLeft, boolean ySplit, boolean yMajorityOnTop){
		this.centroid = centroid;
		this.xMinValue = xMinValue;
		this.xMaxValue = xMaxValue;
		this.yMinValue = yMinValue;
		this.yMaxValue = yMaxValue;
		this.xRadius = xRadius;
		this.yRadius = yRadius;
		this.xSplit = xSplit;
		this.ySplit = ySplit;
		this.xMajorityOnLeft = xMajorityOnLeft;
		this.yMajorityOnTop = yMajorityOnTop;
	}

	public Float getRadius(){
		/*if(xSplit || ySplit){
			double xMax = 0;
			if(xSplit){
				xMax = Math.min(centroid.getX() - xMinValue, xMaxValue - centroid.getX());
			}
			double yMax = 0;
			if(ySplit){
				yMax = Math.min(centroid.getY() - yMinValue, yMaxValue - centroid.getY());
			}
			return Math.max(xMax, yMax);
		}
		return Math.max(Math.max(centroid.getX() - xMinValue, xMaxValue - centroid.getX()), 
				Math.max(centroid.getY() - yMinValue, yMaxValue - centroid.getY()));*/
		return Math.max(xRadius, yRadius);
	}
	
	@Override
	public String toString() {
		return "CentroidWithMinMaxValues [centroid=" + centroid
				+ ", xMinValue=" + xMinValue + ", xMaxValue=" + xMaxValue
				+ ", yMinValue=" + yMinValue + ", yMaxValue=" + yMaxValue + "]";
	}
	
}
