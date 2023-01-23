package org.sofosim.structures;

import java.awt.geom.Point2D;
import javax.vecmath.Point3d;

public class DirectionVector3D {

	private Double length3D = 0.0;
	private DirectionVector xyComponent;
	private DirectionVector xzComponent;
	
	public DirectionVector3D(double length, double xyAngleInRadians, double xzAngleInRadians){
		this.length3D = length;
		this.xyComponent = new DirectionVector(0, xyAngleInRadians);
		this.xzComponent = new DirectionVector(0, xzAngleInRadians);
	}
	
	public DirectionVector3D(double length, DirectionVector xyComponent, DirectionVector xzComponent){
		this.length3D = length;
		this.xyComponent = xyComponent;
		this.xzComponent = xzComponent;
	}
	
	public Double getXYAngle(){
		return xyComponent.getAngle();
	}
	
	public Double getXYAngleInDegrees(){
		return xyComponent.getAngleInDegrees();
	}
	
	public void setXYAngle(Double angle){
		this.xyComponent.setAngle(angle);
	}
	
	public Double getXZAngle(){
		return xzComponent.getAngle();
	}
	
	public Double getXZAngleInDegrees(){
		return xzComponent.getAngleInDegrees();
	}
	
	public void setXZAngle(Double angle){
		this.xzComponent.setAngle(angle);
	}
	
	public double getLength(){
		return this.length3D;
	}
	
	public void setLength(Double length){
		if(length == 0.0){
			//avoid division by zero and handle manually instead
			this.xyComponent.setLength(length);
			this.xzComponent.setLength(length);
			this.length3D = length;
			return;
		}
		double ratio = this.length3D / length;
		this.xyComponent.setLength(xyComponent.getLength() / ratio);
		this.xzComponent.setLength(xzComponent.getLength() / ratio);
		this.length3D = length;
	}
	
	public Point3d convertAngleToCartesianScreenCoordinates(){
		Point2D xy = xyComponent.convertAngleToCartesianScreenCoordinates();
		Point2D xz = xzComponent.convertAngleToCartesianNonScreenCoordinates();
		return new Point3d(xy.getX(), xy.getY(), xz.getY());
	}
	
	public Point3d convertVectorToCartesianScreenCoordinates(){
		Point2D xy = xyComponent.convertVectorToCartesianScreenCoordinates();
		Point2D xz = xzComponent.convertVectorToCartesianNonScreenCoordinates();
		return new Point3d(xy.getX(), xy.getY(), xz.getY());
	}

	@Override
	public String toString() {
		return "DirectionVector3D [xyComponent=" + xyComponent
				+ ", xzComponent=" + xzComponent + "]";
	}
	
}
