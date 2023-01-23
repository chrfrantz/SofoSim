package org.sofosim.forceLayout;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public interface ForceClusterListener<V> {

	/**
	 * Returns results of clustering operation
	 * @param clusterColors HashMap holding clustered vertices and a respective color for each cluster (for consistent visualization).
	 * @param totalNumberOfAgents total number of agents clustered.
	 */
	void receiveClusterResults(LinkedHashMap<ArrayList<V>, Color> clusterColors, Integer totalNumberOfAgents);
	
}
