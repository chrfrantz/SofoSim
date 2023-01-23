package org.sofosim.structures;

import java.awt.geom.Point2D;

public class DirectionVector {

	private double length = 0.0;
	private double angle = 0.0;
	
	public DirectionVector(double length, double angleInRadians){
		this.length = length;
		this.angle = angleInRadians;
	}

	public double getLength() {
		return length;
	}

	public void setLength(double length) {
		this.length = length;
	}

	public double getAngle() {
		return angle;
	}

	public void setAngle(double angle) {
		this.angle = angle;
	}

	public double getAngleInDegrees(){
		return Math.toDegrees(angle);
	}
	
	/**
	 * Checks for negative zero value and replaces with positive one.
	 * @param valueToCheck
	 * @return
	 */
	private double checkForNegativeZero(double valueToCheck){
		if(valueToCheck == -0.0){
			return 0.0;
		}
		return valueToCheck;
	}
	
	/**
	 * Returns the relative coordinates for the theta (the vector angle).
	 * @return
	 */
	public Point2D convertAngleToCartesianScreenCoordinates(){
		double xLength = Math.cos(angle);
		//inverting of y dimensions as of inverted y axis in computers
		double yLength = Math.sin(angle) * -1;
		return new Point2D.Double(xLength, yLength);
		//return new Point2D.Double(checkForNegativeZero(xLength), checkForNegativeZero(yLength));
	}
	
	/**
	 * Returns the relative coordinates for the theta (the vector angle) 
	 * without converting y axis (no screen conversion).
	 * @return
	 */
	public Point2D convertAngleToCartesianNonScreenCoordinates(){
		double xLength = Math.cos(angle);
		//inverting of y dimensions as of inverted y axis in computers
		double yLength = Math.sin(angle);
		return new Point2D.Double(xLength, yLength);
		//return new Point2D.Double(checkForNegativeZero(xLength), checkForNegativeZero(yLength));
	}
	
	/**
	 * Returns relative coordinates for full vector (including length).
	 * See convertAngleToCartesianScreenCoordinates for conversion of angle only.
	 * @return
	 */
	public Point2D convertVectorToCartesianScreenCoordinates(){
		double xLength = Math.cos(angle) * length;
		//inverting of y dimensions as of inverted y axis in computers
		double yLength = Math.sin(angle) * length * -1;
		return new Point2D.Double(xLength, yLength);
		//return new Point2D.Double(checkForNegativeZero(xLength), checkForNegativeZero(yLength));
	}
	
	/**
	 * Returns relative coordinates for full vector (including length) 
	 * ignoring the y axis translation for screens.
	 * See convertAngleToCartesianScreenCoordinates for conversion of angle only.
	 * @return
	 */
	public Point2D convertVectorToCartesianNonScreenCoordinates(){
		double xLength = Math.cos(angle) * length;
		//no inversion of y dimensions (e.g. when representing z axis)
		double yLength = Math.sin(angle) * length;
		return new Point2D.Double(xLength, yLength);
		//return new Point2D.Double(checkForNegativeZero(xLength), checkForNegativeZero(yLength));
	}
	
	@Override
	public String toString() {
		return "DirectionVector [length=" + length + ", angle=" + angle + " (" + getAngleInDegrees() + " degrees)]";
	}
	
	
	
}
