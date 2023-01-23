package org.sofosim.util;

import java.awt.geom.Point2D;
import javax.vecmath.Point3d;
import org.sofosim.structures.DirectionVector;
import org.sofosim.structures.DirectionVector3D;

public class ProximityCalculator3D extends ProximityCalculator {

	private Integer zGridSize = null;
	private static final float tolerance = 0.0f;
	
	public ProximityCalculator3D(Integer xGridSize, Integer yGridSize, Integer zGridSize,
			boolean toroidal) {
		super(xGridSize, yGridSize, toroidal);
		this.zGridSize = zGridSize;
	}
	
	/**
	 * Calculates absolute distance between two points irrespective of grid size and toroidal 
	 * setup. Useful to calculate the angle of a resultant force.
	 * @param firstPoint coordinate of first point
	 * @param secondPoint coordinate of vector target
	 * @return
	 */
	public synchronized DirectionVector3D calculateAbsoluteDistance(Point3d firstPoint, Point3d secondPoint){
		/*DirectionVector xyResult = calculateAbsoluteDistance(new Point2D.Float((float)firstPoint.x, (float)firstPoint.y), new Point2D.Float((float)secondPoint.x, (float)secondPoint.y));
		DirectionVector xzResult = calculateAbsoluteDistance(new Point2D.Float((float)firstPoint.x, (float)firstPoint.z), new Point2D.Float((float)secondPoint.x, (float)secondPoint.z));
		return new DirectionVector3D(xyResult, xzResult);*/
		return calculateDistance(firstPoint, secondPoint, false, true);
	}
	
	private boolean checkForNaN(Point3d pointToCheck, String identifier){
		if(Double.isNaN(pointToCheck.x) || Double.isNaN(pointToCheck.y) || Double.isNaN(pointToCheck.z)){
			System.out.println("Point for " + identifier + " contains invalid numbers: " + pointToCheck);
			throw new RuntimeException("invalid NaN");
			//return false;
		}
		return true;
	}
	
	/**
	 * Does check on dimension for given point. 
	 * @param firstPoint
	 * @param pointIdentifier
	 * @return boolean indicating if test passed (false -> boundary violations)
	 */
	public boolean doDimensionBoundaryChecks(Point3d firstPoint, String pointIdentifier){
		//check but ignore rounding issues - not relevant for display
		if(!checkForNaN(firstPoint, pointIdentifier)){
			return false;
		}
		boolean pass = true;
		if(firstPoint.x < tolerance || firstPoint.x > xGridSize){
			System.err.println("Invalid X coordinate for " + pointIdentifier + ": Outside the range of 0 to " + xGridSize + " detected. Coordinates: " + firstPoint);
			pass = false;
		}
		if(firstPoint.y < tolerance || firstPoint.y > yGridSize){
			System.err.println("Invalid Y coordinate for " + pointIdentifier + ": Outside the range of 0 to " + yGridSize + " detected. Coordinates: " + firstPoint);
			pass = false;
		}
		if(firstPoint.z < tolerance || firstPoint.z > zGridSize){
			System.err.println("Invalid Z coordinate for " + pointIdentifier + ": Outside the range of 0 to " + zGridSize + " detected. Coordinates: " + firstPoint);
			pass = false;
		}
		return pass;
	}
	
	public void doDimensionBoundaryChecks(Point3d firstPoint, Point3d secondPoint){
		checkForNaN(firstPoint, null);
		checkForNaN(secondPoint, null);
		if(firstPoint.x < tolerance || firstPoint.x > xGridSize
				|| secondPoint.x < tolerance || secondPoint.x > xGridSize){
			System.err.println("Invalid X coordinates outside the range of 0 to " + xGridSize + " detected. Coordinates: " + firstPoint + ", " + secondPoint);
		}
		if(firstPoint.y < tolerance || firstPoint.y > yGridSize
				|| secondPoint.y < tolerance || secondPoint.y > yGridSize){
			System.err.println("Invalid Y coordinates outside the range of 0 to " + yGridSize + " detected. Coordinates: " + firstPoint + ", " + secondPoint);
		}
		if(firstPoint.z < tolerance || firstPoint.z > zGridSize
				|| secondPoint.z < tolerance || secondPoint.z > zGridSize){
			System.err.println("Invalid Z coordinates outside the range of 0 to " + zGridSize + " detected. Coordinates: " + firstPoint + ", " + secondPoint);
		}
	}
	
	/**
	 * Calculates the distance between two points taking grid dimensions and toroidal nature 
	 * into account (as specified during instantiation of ProximityCalculator3D).
	 * @param firstPoint
	 * @param secondPoint
	 * @return
	 */
	public DirectionVector3D calculateGridDistance(Point3d firstPoint, Point3d secondPoint){
		return calculateGridDistance(firstPoint, secondPoint, toroidal);
	}
	
	/**
	 * Calculates the distance between two points taking the grid dimensions into account. Takes 
	 * toroidal parameter to override internal settings if desireable. 
	 * @param firstPoint
	 * @param secondPoint
	 * @param toroidal indicator if grid should be considered toroidal
	 * @return
	 */
	public DirectionVector3D calculateGridDistance(Point3d firstPoint, Point3d secondPoint, boolean toroidal){
		return calculateDistance(firstPoint, secondPoint, toroidal, false);
	}
	
	
	/**
	 * Calculates the grid distance between two 3D points and considers toroidal setups.
	 * @param firstPoint
	 * @param secondPoint
	 * @param toroidal
	 * @return
	 */
	private DirectionVector3D calculateDistance(Point3d firstPoint, Point3d secondPoint, boolean toroidal, boolean absolute){
		if(debug){
			if(!toroidal){
				if(absolute){
					System.out.println("Calculating absolute distance between points " + firstPoint + " and " + secondPoint);
				} else {
					System.out.println("Calculating non-toroidal distance between points " + firstPoint + " and " + secondPoint);
				}
			} else {
				System.out.println("Calculating grid distance between points " + firstPoint + " and " + secondPoint);
			}
		}
		
		double xDistance = 0;
		double yDistance = 0;
		double zDistance = 0;
		//secondPoint -1 being west, 1 being east
		int xDir = 0;
		//secondPoint -1 being south, 1 being north
		int yDir = 0;
		//secondPoint -1 being back, 1 being front
		int zDir = 0;
		
		if(doChecks && !absolute){
			doDimensionBoundaryChecks(firstPoint, secondPoint);
		}
				
		if(toroidal && (Math.sqrt(Math.pow(firstPoint.x - secondPoint.x, 2)) > (xGridSize / 2))){
			if(debug){
				System.out.println("Inverted X");
			}
			if(firstPoint.x > secondPoint.x){
				xDistance = secondPoint.x + (xGridSize - firstPoint.x);
				//second point now right of first point as distance is shorter by crossing grid boundary
				xDir = 1;
				if(debug){
					System.out.println("Second X now right of first X");
				}
			} else if(firstPoint.x != secondPoint.x){
				xDistance = firstPoint.x + (xGridSize - secondPoint.x);
				//second point now left of first point as distance is shorter by crossing grid boundary
				xDir = -1;
				if(debug){
					System.out.println("Second X now left of first X");
				}
			}
		} else {
			//absolute distance
			if(firstPoint.x > secondPoint.x){
				xDistance = firstPoint.x - secondPoint.x;
				xDir = -1;
				if(debug){
					System.out.println("Second X left of first X");
				}
			} else if(firstPoint.x != secondPoint.x){
				xDistance = secondPoint.x - firstPoint.x;
				xDir = 1;
				if(debug){
					System.out.println("Second X right of first X");
				}
			} 
		}
		if(toroidal && (Math.sqrt(Math.pow(firstPoint.y - secondPoint.y, 2)) > (yGridSize / 2))){
			if(debug){
				System.out.println("Inverted Y");
			}
			if(firstPoint.y > secondPoint.y){
				yDistance = secondPoint.y + (yGridSize - firstPoint.y);
				//second point now south of first point as distance is shorter by crossing grid boundary
				yDir = -1;
				if(debug){
					System.out.println("Second Y now below of first Y");
				}
			} else if(firstPoint.y != secondPoint.y){
				yDistance = firstPoint.y + (yGridSize - secondPoint.y);
				//second point now north of first point as distance is shorter by crossing grid boundary
				yDir = 1;
				if(debug){
					System.out.println("Second Y now above of first Y");
				}
			}
		} else {
			//absolute distance
			if(firstPoint.y > secondPoint.y){
				yDistance = firstPoint.y - secondPoint.y;
				yDir = 1;
				if(debug){
					System.out.println("Second Y above of first Y");
				}
			} else if(firstPoint.y != secondPoint.y){
				yDistance = secondPoint.y - firstPoint.y;
				yDir = -1;
				if(debug){
					System.out.println("Second Y below of first Y");
				}
			}
		}
		if(toroidal && (Math.sqrt(Math.pow(firstPoint.z - secondPoint.z, 2)) > (zGridSize / 2))){
			if(debug){
				System.out.println("Inverted Z");
			}
			if(firstPoint.z > secondPoint.z){
				zDistance = secondPoint.z + (zGridSize - firstPoint.z);
				//second point now in front of first point as distance is shorter by crossing grid boundary
				zDir = 1;
				if(debug){
					System.out.println("Second Z now in front of first Z");
				}
			} else if(firstPoint.z != secondPoint.z){
				zDistance = firstPoint.z + (zGridSize - secondPoint.z);
				//second point now behind first point as distance is shorter by crossing grid boundary
				zDir = -1;
				if(debug){
					System.out.println("Second Z now behind of first Z");
				}
			}
		} else {
			if(firstPoint.z > secondPoint.z){
				zDistance = firstPoint.z - secondPoint.z;
				zDir = 1;
				if(debug){
					System.out.println("Second Z in front of first Z");
				}
			} else if(firstPoint.z != secondPoint.z){
				zDistance = secondPoint.z - firstPoint.z;
				zDir = -1;
				if(debug){
					System.out.println("Second Z behind of first Z");
				}
			}
		}
		if(doChecks){
			/*if(xDistance == 0.0 || yDistance == 0.0 || zDistance == 0.0){
				System.err.println("Check distance calculation for " + firstPoint + " and " + secondPoint);
				System.err.println(new StringBuffer("xDistance: ").append(xDistance).append(", yDistance: ").append(yDistance).append(", zDistance: ").append(zDistance));
			}*/
		} else {
			if(debug){
				System.out.println(new StringBuffer("xDistance: ").append(xDistance).append(", yDistance: ").append(yDistance).append(", zDistance: ").append(zDistance));
			}
		}
		
		double length = Math.sqrt(Math.pow(xDistance, 2) + Math.pow(yDistance, 2) + Math.pow(zDistance, 2));
		
		DirectionVector xyResult;
		DirectionVector xzResult;
		//if(!absolute){
			xyResult = calculateGridDistance(new Point2D.Float((float)firstPoint.x, (float)firstPoint.y), new Point2D.Float((float)secondPoint.x, (float)secondPoint.y), toroidal, true);
			xzResult = calculateGridDistance(new Point2D.Float((float)firstPoint.x, (float)firstPoint.z), new Point2D.Float((float)secondPoint.x, (float)secondPoint.z), toroidal, false);
		/*} else {
			xyResult = calculateAbsoluteDistance(new Point2D.Float((float)firstPoint.x, (float)firstPoint.y), new Point2D.Float((float)secondPoint.x, (float)secondPoint.y));
			xzResult = calculateAbsoluteDistance(new Point2D.Float((float)firstPoint.x, (float)firstPoint.z), new Point2D.Float((float)secondPoint.x, (float)secondPoint.z));
		}*/
		if(debug){
			System.out.println("Distance between " + firstPoint + " and " + secondPoint + " XY: (" + xyResult + "), XZ: (" + xzResult + "): " + length);
		}
		return new DirectionVector3D(length, xyResult, xzResult);
	}
	
	/**
	 * Calculates the grid distance between two 3D points and considers toroidal setups.
	 * Uses toroidal information from ProximityCalculator at time of instantiation. 
	 * @param firstPoint
	 * @param secondPoint
	 * @return
	 */
	/*
	public DirectionVector3D calculateGridDistance(Point3d firstPoint, Point3d secondPoint){
		return calculateGridDistance(firstPoint, secondPoint, this.toroidal);
	}*/
	
	//taken from Processing source: http://processing.googlecode.com/svn/trunk/processing/core/src/processing/core/PVector.java
	/*static public float angleBetween(Point3d v1, Point3d v2) {
	    double dot = v1.x * v2.x + v1.y * v2.y + v1.z * v2.z;
	    double v1mag = Math.sqrt(v1.x * v1.x + v1.y * v1.y + v1.z * v1.z);
	    double v2mag = Math.sqrt(v2.x * v2.x + v2.y * v2.y + v2.z * v2.z);
	    // This should be a number between -1 and 1, since it's "normalized"
	    double amt = dot / (v1mag * v2mag);
	    // But if it's not due to rounding error, then we need to fix it
	    // http://code.google.com/p/processing/issues/detail?id=340
	    // Otherwise if outside the range, acos() will return NaN
	    // http://www.cppreference.com/wiki/c/math/acos
	    if (amt <= -1) {
	      return (float) Math.PI;
	    } else if (amt >= 1) {
	      // http://code.google.com/p/processing/issues/detail?id=435
	      return 0;
	    }
	    return (float) Math.acos(amt);
	  }

	public synchronized DirectionVector calculateGridDistance(Point3d firstPoint, Point3d secondPoint){
		return calculateGridDistance(firstPoint, secondPoint, toroidal);
	}*/

	/**
	 * Calculates the distance between two points by taking the grid size and toroidal property 
	 * into account. Input values must be in range of the grid, i.e. visible
	 * @param firstPoint coordinate of first point
	 * @param secondPoint coordinate of second (target) point
	 * @param toroidal boolean indicator if grid is toroidal
	 * @return
	 */
	/*
	public synchronized DirectionVector calculateGridDistance(Point3d firstPoint, Point3d secondPoint, boolean toroidal){
		
		double xDistance = 0;
		double yDistance = 0;
		double zDistance = 0;
		//secondPoint -1 being west, 1 being east
		int xDir = 0;
		//secondPoint -1 being south, 1 being north
		int yDir = 0;
		//secondPoint -1 being back, 1 being front
		int zDir = 0;
		
		if(doChecks){
			if(firstPoint.getX() < 0 || firstPoint.getX() >= xGridSize
					|| secondPoint.getX() < 0 || secondPoint.getX() >= xGridSize){
				System.err.println("Invalid X coordinates outside the range of 0 to " + xGridSize + " detected. Coordinates: " + firstPoint + ", " + secondPoint);
			}
			if(firstPoint.getY() < 0 || firstPoint.getY() >= yGridSize
					|| secondPoint.getY() < 0 || secondPoint.getY() >= yGridSize){
				System.err.println("Invalid Y coordinates outside the range of 0 to " + yGridSize + " detected. Coordinates: " + firstPoint + ", " + secondPoint);
			}
			if(firstPoint.getZ() < 0 || firstPoint.getZ() >= zGridSize
					|| secondPoint.getZ() < 0 || secondPoint.getZ() >= zGridSize){
				System.err.println("Invalid Z coordinates outside the range of 0 to " + zGridSize + " detected. Coordinates: " + firstPoint + ", " + secondPoint);
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
			if(Math.sqrt(Math.pow(firstPoint.getZ() - secondPoint.getZ(), 2)) > (zGridSize / 2)){
				if(firstPoint.getZ() > secondPoint.getZ()){
					zDistance = secondPoint.getZ() + (yGridSize - firstPoint.getZ());
					//second point now in front of first point as distance is shorter by crossing grid boundary
					zDir = 1;
				} else if(firstPoint.getZ() != secondPoint.getZ()){
					zDistance = firstPoint.getZ() + (zGridSize - secondPoint.getZ());
					//second point now behind first point as distance is shorter by crossing grid boundary
					zDir = -1;
				}
				if(debug){
					System.out.println("Inverted Z");
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
		if(zDir == 0){
			if(firstPoint.getZ() > secondPoint.getZ()){
				zDistance = firstPoint.getZ() - secondPoint.getZ();
				//second point behind first
				zDir = -1;
			} else if(firstPoint.getZ() != secondPoint.getZ()) {
				zDistance = secondPoint.getZ() - firstPoint.getZ();
				//second point in front of first
				zDir = 1;
			}
		}
		double angle = angleBetween(firstPoint, secondPoint);//Math.atan2(yDistance * yDir, xDistance * xDir);
		//Calc: http://www.dreamincode.net/code/snippet4417.htm
		double length = Math.sqrt(Math.pow(xDistance, 2) + Math.pow(yDistance, 2) + Math.pow(zDistance, 2)); 
		return new DirectionVector(length, angle);
	}*/
	
	/**
	 * Transpose a point using the scale passed via constructor.
	 * This method uses the toroidal setting passed upon instantiation of ProximityCalculator.
	 * @param desiredTarget
	 * @return
	 */
	public Point3d transposeTarget(Point3d desiredTarget){
		desiredTarget.set(transposeTargetX(desiredTarget.x, toroidal), transposeTargetY(desiredTarget.y, toroidal), transposeTargetZ(desiredTarget.z, toroidal));
		return desiredTarget;
	}
	
	public Point3d transposeTarget(Point3d desiredTarget, boolean toroidal){
		desiredTarget.set(transposeTargetX(desiredTarget.x, toroidal), transposeTargetY(desiredTarget.y, toroidal), transposeTargetZ(desiredTarget.z, toroidal));
		return desiredTarget;
	}
	
	public Point3d transposeTarget(Point3d desiredTarget, int scale, boolean toroidal){
		desiredTarget.set(transpose(desiredTarget.x, scale, toroidal), transpose(desiredTarget.y, scale, toroidal), transpose(desiredTarget.z, scale, toroidal));
		return desiredTarget;
	}
	
	/**
	 * Transpose z value of desired target position to grid.
	 * This method uses the toroidal setting passed upon instantiation of ProximityCalculator.
	 * @param diffZ
	 * @return
	 */
	public Double transposeTargetZ(Double diffZ){
		return transpose(diffZ, zGridSize, toroidal);
	}
	
	/**
	 * Transpose z value of desired target position to grid.
	 * @param diffZ
	 * @param toroidal
	 * @return
	 */
	public Double transposeTargetZ(Double diffZ, boolean toroidal){
		return transpose(diffZ, zGridSize, toroidal);
	}
	
}
