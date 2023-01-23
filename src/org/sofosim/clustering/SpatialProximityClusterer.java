package org.sofosim.clustering;

import java.util.ArrayList;
import java.util.Collection;

public interface SpatialProximityClusterer<V> {

	public void setVertexPoints(Collection<V> collection);
	
	public void setMinimalNumberOfMembersForCluster(int minimalNumberOfMembers);
	
	public void setMaximalDistanceOfClusterMembers(float maximalDistance);
	
	public void applyClustering();

	public ArrayList<ArrayList<V>> getLastResultList();
	
	public ArrayList<V> getClusterNeighbours(String memberName);
	
}
