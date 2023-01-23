package org.sofosim.clustering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import org.nzdis.micro.util.SimpleSemaphore;
import org.sofosim.util.ProximityCalculator3D;

public class DBSCAN<V> implements SpatialProximityClusterer<V> {

	/**
	 * Creates and instance of DBSCAN to cluster collections of points.
	 * Initializes with given parameters, then fill setVertexPoints() and
	 * finally applyClustering().
	 * 
	 * @param epsilon
	 * @param minimumNoOfMembersPerCluster
	 * @param proxCalc
	 */
	public DBSCAN(float epsilon, int minimumNoOfMembersPerCluster,
			ProximityCalculator3D proxCalc) {
		this.epsilon = epsilon;
		this.minimumNumberOfClusterMembers = minimumNoOfMembersPerCluster;
		this.proximityCalculator = proxCalc;
		this.semaphore.printWaitInformationOutput(false);
		applyClustering();
	}

	private SimpleSemaphore semaphore = new SimpleSemaphore(
			"ProximityClusterer");

	/** max. distance of vertices to be considered as cluster */
	private float epsilon = 1f;
	/** minimum number of members to start cluster */
	private int minimumNumberOfClusterMembers = 2;

	private ArrayList<ArrayList<V>> resultList = new ArrayList<ArrayList<V>>();

	/**
	 * Get clusters determined in last execution round.
	 */
	public ArrayList<ArrayList<V>> getLastResultList() {
		return resultList;
	}

	private HashMap<String, Integer> clusterNeighbourIndex = new HashMap<String, Integer>();

	/**
	 * Returns the members of the same cluster.
	 * 
	 * @param memberName
	 *            name of vertex - not full VertexPoint
	 * @return
	 */
	public ArrayList<V> getClusterNeighbours(String memberName) {
		//semaphore.acquire();
		// try{
		if (!resultList.isEmpty()
				&& clusterNeighbourIndex.containsKey(memberName)) {
			//semaphore.release();
			return resultList.get(clusterNeighbourIndex.get(memberName));
		}
		/*
		 * }catch (IndexOutOfBoundsException e) { // TODO: handle exception
		 * }catch (NullPointerException e){
		 * 
		 * }
		 */
		//semaphore.release();
		return null;
	}

	/** internal field for list of VertexPoints to be clustered */
	private ArrayList<V> pointList = null;

	/** index maintaining visited points */
	private HashSet<V> visitedPoints = new HashSet<V>();

	/** ProximityCalculator for calculating distance between points */
	private ProximityCalculator3D proximityCalculator = null;

	/**
	 * Calculates the neighbours of a given VertexPoint.
	 * 
	 * @param p
	 *            VertexPoint whose neighbours are looked for
	 * @return Vector of VertexPoints that are neighbours to p
	 */
	private ArrayList<V> getNeighbours(V p) {
		ArrayList<V> neighbours = new ArrayList<V>();
		//Iterator<V> points = pointList.iterator();
		for(int i=0; i<pointList.size(); i++){
		//while (points.hasNext()) {
			V q = pointList.get(i);
			if (proximityCalculator.calculateGridDistance(
					((VertexPoint3D) p).point, ((VertexPoint3D) q).point,
					proximityCalculator.isToroidalGraph()).getLength() <= epsilon) {
				neighbours.add(q);
			}
		}
		return neighbours;
	}

	/**
	 * Merges the elements of the right collection to the left one and returns
	 * the combination.
	 * 
	 * @param neighbours1
	 *            left collection
	 * @param neighbours2
	 *            right collection
	 * @return amended left collection
	 */
	private ArrayList<V> mergeRightToLeftCollection(ArrayList<V> neighbours1,
			ArrayList<V> neighbours2) {
		/*HashSet<V> set = new HashSet<V>(neighbours1);
		set.addAll(neighbours2);
		return new ArrayList<V>(set);*/
		for (int i = 0; i < neighbours2.size(); i++) {
			V tempPt = neighbours2.get(i);
			if (!neighbours1.contains(tempPt)) {
				neighbours1.add(tempPt);
			}
		}
		return neighbours1;
	}

	/**
	 * Sets a collection of VertexPoints to be clustered
	 * 
	 * @param collection
	 */
	public void setVertexPoints(Collection<V> collection) {
		this.pointList = new ArrayList<V>(collection);
	}

	/**
	 * Sets the minimal number of members to consider points of close proximity
	 * clustered.
	 * 
	 * @param minimalNumberOfMembers
	 */
	public void setMinimalNumberOfMembersForCluster(int minimalNumberOfMembers) {
		this.minimumNumberOfClusterMembers = minimalNumberOfMembers;
	}

	/**
	 * Sets the maximal distance members of the same cluster can have while
	 * still be considered in the same cluster.
	 * 
	 * @param maximalDistance
	 */
	public void setMaximalDistanceOfClusterMembers(float maximalDistance) {
		this.epsilon = maximalDistance;
	}

	/**
	 * Applies the clustering and returns a collection of clusters (i.e. a list
	 * of lists of the respective cluster members).
	 * 
	 * @return
	 */
	public void applyClustering() {
		
		if(pointList == null){
			return;
		}
		if(pointList.isEmpty()){
			throw new RuntimeException(
					"DBSCAN: No points added for clustering!");
		}
		// temporary result list - result to be copied later to maintain
		// updated and complete results list
		ArrayList<ArrayList<V>> tempResultList = new ArrayList<ArrayList<V>>();
		// resultList.clear();
		HashMap<String, Integer> tempClusterNeighbourIndex = new HashMap<String, Integer>();
		//clusterNeighbourIndex.clear();
		visitedPoints.clear();

		ArrayList<V> neighbours;
		int index = 0;

		while (pointList.size() > index) {
			V p = pointList.get(index);
			if (!visitedPoints.contains(p)) {
				visitedPoints.add(p);
				neighbours = getNeighbours(p);

				if (neighbours.size() >= minimumNumberOfClusterMembers) {
					int ind = 0;
					while (neighbours.size() > ind) {
						V r = neighbours.get(ind);
						if (!visitedPoints.contains(r)) {
							visitedPoints.add(r);
							ArrayList<V> individualNeighbours = getNeighbours(r);
							if (individualNeighbours.size() >= minimumNumberOfClusterMembers) {
								neighbours = mergeRightToLeftCollection(
										neighbours,
										individualNeighbours);
							}
						}
						ind++;
					}
					tempResultList.add(neighbours);
				}
			}
			index++;
		}
		// create inverted index
		for (int i = 0; i < tempResultList.size(); i++) {
			ArrayList<V> vpList = tempResultList.get(i);
			for (int l = 0; l < vpList.size(); l++) {
				tempClusterNeighbourIndex.put(((VertexPoint3D) vpList
						.get(l)).vertex.toString(), i);
			}
		}
		// System.out.println("Index: " + clusterNeighbourIndex.size());
		clusterNeighbourIndex = tempClusterNeighbourIndex;
		resultList = tempResultList;//(ArrayList<ArrayList<V>>) tempResultList.clone();
		//semaphore.release();
	}

}
