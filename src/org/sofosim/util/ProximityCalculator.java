package org.sofosim.util;

import java.awt.geom.Point2D;
import org.sofosim.structures.DirectionVector;
import ec.util.MersenneTwisterFast;

public class ProximityCalculator {

	protected Integer xGridSize = null;
	protected Integer yGridSize = null;
	protected Boolean toroidal = null;
	protected final boolean doChecks = false;
	protected final boolean debug = false;

	
	public ProximityCalculator(Integer xGridSize, Integer yGridSize, boolean toroidal){
		this.xGridSize = xGridSize;
		this.yGridSize = yGridSize;
		this.toroidal = toroidal;
	}
	
	public boolean isToroidalGraph(){
		return this.toroidal;
	}
	
	/**
	 * Calculates absolute distance between two points irrespective of grid size and toroidal 
	 * setup. Useful to calculate the angle of a resultant force.
	 * @param firstPoint coordinate of first point
	 * @param secondPoint coordinate of vector target
	 * @return
	 */
	public synchronized DirectionVector calculateAbsoluteDistance(Point2D firstPoint, Point2D secondPoint){
		
		double xDistance = 0;
		double yDistance = 0;
		//secondPoint -1 being west, 1 being east
		int xDir = 0;
		//secondPoint -1 being south, 1 being north
		int yDir = 0;
		if(firstPoint.getX() > secondPoint.getX()){
			xDistance = firstPoint.getX() - secondPoint.getX();
			xDir = -1;
		} else if(firstPoint.getX() != secondPoint.getX()){
			xDistance = secondPoint.getX() - firstPoint.getX();
			xDir = 1;
		}
		if(firstPoint.getY() > secondPoint.getY()){
			yDistance = firstPoint.getY() - secondPoint.getY();
			yDir = 1;
		} else if(firstPoint.getY() != secondPoint.getY()){
			yDistance = secondPoint.getY() - firstPoint.getY();
			yDir = -1;
		}
		if(debug){
			StringBuffer buffer = new StringBuffer("Calculate Absolute Distance:\n");
			buffer.append("First: ").append(firstPoint).append(", Second: ").append(secondPoint).append("\n");
			buffer.append("X Distance: ").append(xDistance).append("\n");
			buffer.append("Y Distance: ").append(yDistance).append("\n");
			buffer.append("xDir: ").append(xDir).append("\n");
			buffer.append("yDir: ").append(yDir).append("\n");
			System.out.println(buffer);
			//ForceDirectedLayout.getWriter().write(buffer);
		}
		double angle = Math.atan2(yDistance * yDir, xDistance * xDir);
		double length = Math.sqrt(Math.pow(xDistance, 2) + Math.pow(yDistance, 2)); 
		return new DirectionVector(length, angle);
	}
	
	/**
	 * Calculates the distance between two points by taking the grid size and toroidal property 
	 * into account. Input values must be in range of the grid, i.e. visible.
	 * This method uses the toroidal setting passed upon instantiation of ProximityCalculator.
	 * It also considers input coordinates convertes Y for correct screen display.
	 * @param firstPoint coordinate of first point
	 * @param secondPoint coordinate of second (target) point
	 * @return
	 */
	public synchronized DirectionVector calculateGridDistance(Point2D firstPoint, Point2D secondPoint){
		return calculateGridDistance(firstPoint, secondPoint, toroidal, true);
	}
	
	/**
	 * Calculates the distance between two points by taking the grid size and toroidal property 
	 * into account. Input values must be in range of the grid, i.e. visible.
	 * This method uses the toroidal setting passed upon instantiation of ProximityCalculator.
	 * It does NOT translate input coordinates to an inverted Y for correct screen display.
	 * @param firstPoint coordinate of first point
	 * @param secondPoint coordinate of second (target) point
	 * @return
	 */
	public synchronized DirectionVector calculateGridDistanceWithoutConvertedY(Point2D firstPoint, Point2D secondPoint){
		return calculateGridDistance(firstPoint, secondPoint, toroidal, false);
	}
	
	/**
	 * Calculates the distance between two points by taking the grid size and toroidal property 
	 * into account. Input values must be in range of the grid, i.e. visible
	 * @param firstPoint coordinate of first point
	 * @param secondPoint coordinate of second (target) point
	 * @param toroidal boolean indicator if grid is toroidal
	 * @param useScreenYCoordinate indicates if y coordinate should be translated for screens (inverted)
	 * @return
	 */
	public synchronized DirectionVector calculateGridDistance(Point2D firstPoint, Point2D secondPoint, boolean toroidal, boolean useScreenYCoordinate){
		
		double xDistance = 0;
		double yDistance = 0;
		//secondPoint -1 being west, 1 being east
		int xDir = 0;
		//secondPoint -1 being south, 1 being north
		int yDir = 0;
		
		
		if(doChecks){
			if(firstPoint.getX() < 0 || firstPoint.getX() >= xGridSize
					|| secondPoint.getX() < 0 || secondPoint.getX() >= xGridSize){
				System.err.println("Invalid X coordinates outside the range of 0 to " + xGridSize + " detected. Coordinates: " + firstPoint + ", " + secondPoint);
			}
			if(firstPoint.getY() < 0 || firstPoint.getY() >= yGridSize
					|| secondPoint.getY() < 0 || secondPoint.getY() >= yGridSize){
				System.err.println("Invalid Y coordinates outside the range of 0 to " + yGridSize + " detected. Coordinates: " + firstPoint + ", " + secondPoint);
			}
		}
				
		if(toroidal){	
			if(Math.sqrt(Math.pow(firstPoint.getX() - secondPoint.getX(), 2)) > (xGridSize / 2)){
				if(firstPoint.getX() > secondPoint.getX()){
					xDistance = secondPoint.getX() + (xGridSize - firstPoint.getX());
					//second point now right of first point as distance is shorter by crossing grid boundary
					xDir = 1;
				} else if(firstPoint.getX() != secondPoint.getX()){
					xDistance = firstPoint.getX() + (xGridSize - secondPoint.getX());
					//second point now left of first point as distance is shorter by crossing grid boundary
					xDir = -1;
				}
				if(debug){
					System.out.println("Inverted X");
				}
			}
			if(Math.sqrt(Math.pow(firstPoint.getY() - secondPoint.getY(), 2)) > (yGridSize / 2)){
				if(firstPoint.getY() > secondPoint.getY()){
					yDistance = secondPoint.getY() + (yGridSize - firstPoint.getY());
					//second point now south of first point as distance is shorter by crossing grid boundary
					yDir = -1;
				} else if(firstPoint.getY() != secondPoint.getY()){
					yDistance = firstPoint.getY() + (yGridSize - secondPoint.getY());
					//second point now north of first point as distance is shorter by crossing grid boundary
					yDir = 1;
				}
				if(debug){
					System.out.println("Inverted Y");
				}
			}
		} 
		if(xDir == 0) {
			if(firstPoint.getX() > secondPoint.getX()){
				xDistance = firstPoint.getX() - secondPoint.getX();
				//second point west of first
				xDir = -1;
			} else if(firstPoint.getX() != secondPoint.getX()) {
				xDistance = secondPoint.getX() - firstPoint.getX();
				//second point east of first
				xDir = 1;
			}
		}
		if(yDir == 0){
			if(firstPoint.getY() > secondPoint.getY()){
				yDistance = firstPoint.getY() - secondPoint.getY();
				//second point north of first (consider inverted y - 0 on top)
				yDir = 1;
			} else if(firstPoint.getY() != secondPoint.getY()) {
				yDistance = secondPoint.getY() - firstPoint.getY();
				//second point south of first (consider inverted y - 0 on top)
				yDir = -1;
			}
		}
		if(!useScreenYCoordinate){
			yDir *= -1;
		}
		double angle;
		if(yDir == 0){
			//if y irrelevant, ensure that angle is not set to PI (180.0) to avoid rounding issues
			angle = 0;
		} else {
			angle = Math.atan2(yDistance * yDir, xDistance * xDir);
		}
		double length = Math.sqrt(Math.pow(xDistance, 2) + Math.pow(yDistance, 2)); 
		return new DirectionVector(length, angle);
	}
	
	/**
	 * Transpose a point using the scale passed via constructor.
	 * This method uses the toroidal setting passed upon instantiation of ProximityCalculator.
	 * @param desiredTarget
	 * @return
	 */
	public Point2D transposeTarget(Point2D desiredTarget){
		desiredTarget.setLocation(transposeTargetX(desiredTarget.getX(), toroidal), transposeTargetY(desiredTarget.getY(), toroidal));
		return desiredTarget;
	}
	
	/**
	 * Transpose a point using the scale passed via constructor.
	 * @param desiredTarget
	 * @param toroidal
	 * @return
	 */
	public Point2D transposeTarget(Point2D desiredTarget, boolean toroidal){
		desiredTarget.setLocation(transposeTargetX(desiredTarget.getX(), toroidal), transposeTargetY(desiredTarget.getY(), toroidal));
		return desiredTarget;
	}
	
	/**
	 * Transpose a point with a different scale than the one used in the constructor of ProximityCalculator.
	 * This method uses the toroidal setting passed upon instantiation of ProximityCalculator.
	 * @param desiredTarget
	 * @param scale
	 * @return
	 */
	public Point2D transposeTarget(Point2D desiredTarget, Integer scale){
		return new Point2D.Double(transpose(desiredTarget.getX(), scale, toroidal), transpose(desiredTarget.getY(), scale, toroidal));
	}
	
	/**
	 * Transpose a point with a different scale than the one used in the constructor of ProximityCalculator.
	 * @param desiredTarget
	 * @param scale
	 * @param toroidal
	 * @return
	 */
	public Point2D transposeTarget(Point2D desiredTarget, Integer scale, boolean toroidal){
		return new Point2D.Double(transpose(desiredTarget.getX(), scale, toroidal), transpose(desiredTarget.getY(), scale, toroidal));
	}
	
	/**
	 * Transpose x value of desired target position to grid.
	 * This method uses the toroidal setting passed upon instantiation of ProximityCalculator.
	 * @param diffX
	 * @return
	 */
	public Double transposeTargetX(Double diffX){
		return transpose(diffX, xGridSize, toroidal);
	}
	
	/**
	 * Transpose x value of desired target position to grid.
	 * @param diffX
	 * @param toroidal
	 * @return
	 */
	public Double transposeTargetX(Double diffX, boolean toroidal){
		return transpose(diffX, xGridSize, toroidal);
	}
	
	/**
	 * Transpose y value of desired target position to grid.
	 * This method uses the toroidal setting passed upon instantiation of ProximityCalculator.
	 * @param diffY
	 * @return
	 */
	public Double transposeTargetY(Double diffY){
		return transpose(diffY, yGridSize, toroidal);
	}
	
	/**
	 * Transpose y value of desired target position to grid.
	 * @param diffY
	 * @param toroidal
	 * @return
	 */
	public Double transposeTargetY(Double diffY, boolean toroidal){
		return transpose(diffY, yGridSize, toroidal);
	}
	
	/**
	 * Transposes a given desired target position onto a valid position 
	 * of the specified grid (indicated by grid size)
	 * @param desiredTargetPos desired target position
	 * @param scale grid size (length of one grid dimension, assuming a squared XY grid)
	 * @param toroidal indicator if grid is toroidal (in both directions)
	 * @return
	 */
	protected Double transpose(Double desiredTargetPos, Integer scale, boolean toroidal){
		if(toroidal){
			//if target position is multitudes beyond scale, correct that
			if(Math.abs(desiredTargetPos) > scale){
				//calculate absolute value of scale factor
				Double div = Math.abs(desiredTargetPos / new Double(scale));
				if(desiredTargetPos < 0){
					desiredTargetPos += div.intValue() * scale;
				} else {
					//if greater zero
					desiredTargetPos -= div.intValue() * scale;
				}
			} else if(Math.abs(desiredTargetPos) == scale) {
				desiredTargetPos = 0.0;
			}
			//if it is negative, transpose
			if(desiredTargetPos < 0){
				desiredTargetPos += scale;
			}
		} else {
			if(desiredTargetPos < 0 || desiredTargetPos >= scale){
				return 0.0;
			}
		}
		return desiredTargetPos;
	}

}
