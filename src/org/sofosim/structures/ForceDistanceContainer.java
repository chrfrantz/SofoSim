package org.sofosim.structures;


public class ForceDistanceContainer {

	public DirectionVector3D distanceVector = null;
	public double force = 0.0;
	
	public ForceDistanceContainer(DirectionVector3D distanceVector, double force) {
		super();
		this.distanceVector = distanceVector;
		this.force = force;
	}

}
