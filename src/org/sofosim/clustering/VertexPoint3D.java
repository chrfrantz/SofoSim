package org.sofosim.clustering;

import javax.vecmath.Point3d;

public class VertexPoint3D<V> {


	public final V vertex;
	public final Point3d point;
	
	public VertexPoint3D(V vertex){
		this(vertex, null);
	}
	
	public VertexPoint3D(V vertex, Point3d point){
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
		//System.out.println("Other Obj: " + ((VertexPoint3D<V>)obj2).vertex + " (" + ((VertexPoint3D<V>)obj2).vertex.getClass().getSimpleName() + ") vs. own " + this.vertex + " (" + vertex.getClass().getSimpleName() + ") -> " + ((VertexPoint3D)obj2).vertex.equals(vertex));
		if(((VertexPoint3D<V>)obj2).vertex.equals(this.vertex)){
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
