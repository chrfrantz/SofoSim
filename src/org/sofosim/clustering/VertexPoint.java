package org.sofosim.clustering;

import java.awt.geom.Point2D;

public class VertexPoint<V>{

	public final V vertex;
	public final Point2D point;
	
	public VertexPoint(V vertex){
		this(vertex, null);
	}
	
	public VertexPoint(V vertex, Point2D point){
		this.vertex = vertex;
		this.point = point;
	}

	
	public String toFullString() {
		return "VertexPoint [vertex=" + vertex + ", point=" + point + "]";
	}
	
	@Override
	public boolean equals(Object obj2){
		if(obj2 == null){
			return false;
		}
		if(((VertexPoint)obj2).vertex.equals(vertex)){
			return true;
		}
		return false;
	}
	
	@Override
	public int hashCode(){
		return toString().hashCode();
	}
	
	@Override
	//needs to be only name as of comparison on name only
	public String toString() {
		//return "VertexPoint [vertex=" + vertex + "]";
		return vertex.toString();
	}
	
}
