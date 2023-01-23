package org.sofosim.clustering;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.vecmath.Point3d;

public class ClusterUtility {

	/**
	 * Calculates the centroid for a given List of VertexPoints, a 
	 * maximal distance to consider them clustered and grid dimensions 
	 * to transpose centroids on a toroidal grid (if toroidal is set true)s.
	 * @param vertexPoints
	 * @param maxDistanceOfClusterMembers
	 * @param gridWidth
	 * @param gridHeight
	 * @param toroidal
	 * @return CentroidWithMinMaxValues data structure with extended data.
	 */
	public static CentroidWithMinMaxValues calculateCentroid(final List<VertexPoint3D> vertexPoints, final float maxDistanceOfClusterMembers, final int gridWidth, final int gridHeight, final boolean toroidal){
		float xCenter = 0.0f;
		float yCenter = 0.0f;
		float xMinValue = Float.MAX_VALUE;
		float xMaxValue = Float.MIN_VALUE;
		float yMinValue = Float.MAX_VALUE;
		float yMaxValue = Float.MIN_VALUE;
		//collections for individual points
		ArrayList<Float> xPoints = new ArrayList<Float>();
		ArrayList<Float> yPoints = new ArrayList<Float>();
		
		for(int i=0; i<vertexPoints.size(); i++){
			Point3d point = vertexPoints.get(i).point;
			xCenter += point.x;
			yCenter += point.y;
			if(point.x > xMaxValue){
				xMaxValue = (float) point.x;
			}
			if(point.x < xMinValue){
				xMinValue = (float) point.x;
			}
			if(point.y > yMaxValue){
				yMaxValue = (float) point.y;
			}
			if(point.y < yMinValue){
				yMinValue = (float) point.y;
			}
			//adding individual points for gap detection
			if(toroidal){
				xPoints.add((float)point.x);
				yPoints.add((float)point.y);
			}
		}
		float xMean = xCenter / (float)vertexPoints.size();
		float yMean = yCenter / (float)vertexPoints.size();
		float xRadius = -1;
		float yRadius = -1;

		//split detection information for result data structure
		boolean xSplitDetected = false;
		boolean ySplitDetected = false;
		boolean xMajorityOnLeft = false;
		boolean yMajorityOnTop = false;
		if(toroidal){
		
			//Identify separation of cluster in a toroidal grid and correct centroid calculation
			Collections.sort(xPoints);
			Collections.sort(yPoints);
			
			//calculate for split clusters on x dimension
			//placeholder for last checked x value
			float last = 0.0f;
			//sum of x values in first part of split cluster (or if not split at all)
			float x1Sum = 0.0f;
			//number of members belonging to first part of split cluster
			int x1Members = 0;
			//sum of x values in second part of split cluster
			float x2Sum = 0.0f;
			//respective min-max values for individual clusters in case of split
			float xMaxValueInFirstPartOfSplitCluster = 0;
			float xMinValueInSecondPartOfSplitCluster = 0;
			
			for(int v = 0; v < xPoints.size(); v++){
				float current = xPoints.get(v);
				if(!xSplitDetected){
					x1Sum += current;
				} else {
					x2Sum += current;
				}
				if(last != 0.0f && !xSplitDetected && (current - last) > maxDistanceOfClusterMembers){
					/* if two values in a sort set of x values are in further distance than max. cluster distance for DBSCAN, 
					 * consider it a split across the grid */
					//System.out.println("Found x split between " + last + " and " + current);
					x1Members = v;
					xSplitDetected = true;
					//last x value must be max. of first part of split cluster, current one min. of second part of cluster
					xMaxValueInFirstPartOfSplitCluster = last;
					xMinValueInSecondPartOfSplitCluster = current;
				}
				last = current;
			}
			if(xSplitDetected){
				//mean of first part of cluster
				float x1Mean = x1Sum / x1Members;
				//mean of second part of cluster
				float x2Mean = x2Sum / (xPoints.size() - x1Members);
				
				if(x1Members > xPoints.size() / 2){
					//majority left
					xMajorityOnLeft = true;
					//transpose mean of second cluster to left
					x2Mean = x2Mean - gridWidth;
					//transpose min value for second cluster part to left (i.e. below zero on grid in absolute terms) 
					xMinValueInSecondPartOfSplitCluster = xMinValueInSecondPartOfSplitCluster - gridWidth;
				} else {
					//majority right
					//transpose mean of first cluster to right
					x1Mean = x1Mean + gridWidth;
					//transpose max value for first cluster part to right (i.e. now beyond grid size in absolute terms)
					xMaxValueInFirstPartOfSplitCluster = xMaxValueInFirstPartOfSplitCluster + gridWidth;
				}
				//calculate weighted mean
				xMean = (x1Mean * x1Members + x2Mean * (xPoints.size() - x1Members)) / xPoints.size();
				//calculate x radius
				xRadius = Math.max(xMaxValueInFirstPartOfSplitCluster - xMean, xMean - xMinValueInSecondPartOfSplitCluster);
			} else {
				xRadius = Math.max(xMaxValue - xMean, xMean - xMinValue);
			}
			//calculate transformations for y
			//placeholder for last checked y value
			last = 0.0f;
			//sum of y values in first part of split cluster
			float y1Sum = 0.0f;
			//number of values in first part of split cluster
			int y1Members = 0;
			//sum of y values in second part of split cluster
			float y2Sum = 0.0f;
			//respective min-max values for individual clusters in case of split
			float yMaxValueInFirstPartOfSplitCluster = 0;
			float yMinValueInSecondPartOfSplitCluster = 0;
			
			for(int v = 0; v < yPoints.size(); v++){
				float current = yPoints.get(v);
				if(!ySplitDetected){
					y1Sum += current;
				} else {
					y2Sum += current;
				}
				if(last != 0.0f && !ySplitDetected && (current - last) > maxDistanceOfClusterMembers){
					//System.out.println("Found y split between " + last + " and " + current);
					y1Members = v;
					ySplitDetected = true;
					//last y value must be max. of first part of split cluster, current one min. of second part of cluster
					yMaxValueInFirstPartOfSplitCluster = last;
					yMinValueInSecondPartOfSplitCluster = current;
				}
				last = current;
			}
			if(ySplitDetected){
				float y1Mean = y1Sum / y1Members;
				float y2Mean = y2Sum / (yPoints.size() - y1Members);
				
				if(y1Members > yPoints.size() / 2){
					//majority top
					yMajorityOnTop = true;
					//transpose mean of second cluster to top
					y2Mean = y2Mean - gridHeight;
					//transpose min value for second cluster part to top (i.e. below zero on grid in absolute terms) 
					yMinValueInSecondPartOfSplitCluster = yMinValueInSecondPartOfSplitCluster - gridHeight;
				} else {
					//majority bottom
					//transpose mean of first cluster to bottom
					y1Mean = y1Mean + gridHeight;
					//transpose max value for first cluster part to bottom (i.e. now beyond grid size in absolute terms)
					yMaxValueInFirstPartOfSplitCluster = yMaxValueInFirstPartOfSplitCluster + gridHeight;
				}
				//calculate weighted mean
				yMean = (y1Mean * y1Members + y2Mean * (yPoints.size() - y1Members)) / yPoints.size();
				//calculate radius
				yRadius = Math.max(yMaxValueInFirstPartOfSplitCluster - yMean, yMean - yMinValueInSecondPartOfSplitCluster);
			} else {
				yRadius = Math.max(yMaxValue - yMean, yMean - yMinValue);
			}
		}
		return new CentroidWithMinMaxValues(new Point2D.Float(xMean, yMean), xMinValue, xMaxValue, yMinValue, yMaxValue, xRadius, yRadius, xSplitDetected, xMajorityOnLeft, ySplitDetected, yMajorityOnTop);
	}
	
}
