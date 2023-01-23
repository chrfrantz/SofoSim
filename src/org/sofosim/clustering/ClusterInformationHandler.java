package org.sofosim.clustering;

import java.util.ArrayList;

public interface ClusterInformationHandler<V> {

	String getClusterStatsAsString(ArrayList<V> clusterVerticesList);
	
}
