package org.sofosim.clustering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

public class AttractionClusterer<V> {

	private HashMap<V, HashMap<String, Float>> attractingAgentSpheres = new HashMap<V, HashMap<String, Float>>();
	private ArrayList<String> sphereList = new ArrayList<String>();
	private boolean calculateIndividualSphere = false;
	private boolean calculateCompositeSphere = true;
	
	/**
	 * Adds an attraction value for a vertex (agent) on a specified sphere 
	 * (for ex post clustering).
	 * @param vertex vertex index
	 * @param sphere sphere the value is associated with
	 * @param value attraction value for that sphere
	 */
	public void addAttractionValue(V vertex, String sphere, Float value){
		//add to sphere list
		if(!sphereList.contains(sphere)){
			sphereList.add(sphere);
		}
		//if(value > 0){
			if(!attractingAgentSpheres.containsKey(vertex)){
				HashMap<String, Float> innerList = new HashMap<String, Float>();
				innerList.put(sphere, value);
				attractingAgentSpheres.put(vertex, innerList);
			} else {
				HashMap<String, Float> innerList = attractingAgentSpheres.get(vertex);
				if(!innerList.containsKey(sphere)){
					innerList.put(sphere, value);
				} else {
					innerList.put(sphere, innerList.get(sphere) + value);
				}
			}
		//}
	}

	
	/**
	 * Clears all values (setting them to 0.0) for vertices and their respective spheres without 
	 * deleting the vertex and sphere keys - for performance reasons. Should be done 
	 * explicitly every round before refilling with attraction values via addAttractionValue().
	 */
	public void clearAttractionValues(){
		for(Entry<V, HashMap<String, Float>> entry: attractingAgentSpheres.entrySet()){
			for(Entry<String, Float> entry2: entry.getValue().entrySet()){
				entry2.setValue(0.0f);
			}
		}
		
	}

	/** Map containing agent - sphereClusters relationships */
	private HashMap<V, ArrayList<String>> agentSphereMap = new HashMap<V, ArrayList<String>>();
	/** Map containing sphereClusters - agent relationships */
	private HashMap<String, ArrayList<V>> sphereAgentMap = new HashMap<String, ArrayList<V>>();
	
	private void addAgentSphereEntry(V vertex, String sphere){
		if(!agentSphereMap.containsKey(vertex)){
			ArrayList<String> innerList = new ArrayList<String>();
			innerList.add(sphere);
			agentSphereMap.put(vertex, innerList);
		} else {
			agentSphereMap.get(vertex).add(sphere);
		}
	}
	
	private void addSphereAgentEntry(String sphere, V vertex){
		if(!sphereAgentMap.containsKey(sphere)){
			ArrayList<V> innerList = new ArrayList<V>();
			innerList.add(vertex);
			sphereAgentMap.put(sphere, innerList);
		} else {
			sphereAgentMap.get(sphere).add(vertex);
		}
	}
	
	/**
	 * Returns the attracting sphere combinations for a given agent.
	 * @param agent
	 * @return
	 */
	public ArrayList<String> getSpheresForAgent(V agent){
		//System.out.println("Sys: " + agent + " " + agent.getClass().getSimpleName() + " " + agentSphereMap.get(agent) + " " + agentSphereMap.toString());
		return agentSphereMap.get(agent);
	}
	
	/**
	 * Returns the agents for a given sphere combination. 
	 * @param sphere
	 * @return
	 */
	public ArrayList<V> getAgentsForSphere(String sphere){
		return sphereAgentMap.get(sphere);
	}
	
	/**
	 * Clusters provided VertexPoints (agents) by agents and spheres. 
	 * If no initial points provided, all previously saved attraction 
	 * values are used. calculateIndividualSphere and calculateCompositeSphere
	 * determine degree of granularity (e.g. list agent for all individual 
	 * spheres he is grouped under, or/and only sphere combinations (i.e. two or more
	 * characteristics by which agents are clustered)
	 * @param initialCluster
	 */
	public void clusterMatrixEntries(ArrayList<V> initialCluster){
		//map holding agent-<sphere combination> association
		agentSphereMap.clear();
		//map holding sphere-<agent combination> association
		sphereAgentMap.clear();
		HashMap<V, HashMap<String, Float>> basisMap = null;
		//if parameter initialCluster not null, use it as clustering corpus
		if(initialCluster != null){
			basisMap = new HashMap<V, HashMap<String, Float>>();
			for(int i=0; i<initialCluster.size(); i++){
				V key = initialCluster.get(i);
				//System.out.println("Attracting spheres: " + attractingAgentSpheres.toString() + ", key: " + key);
				basisMap.put(key, attractingAgentSpheres.get(key));
			}
		} else {
			//else use all agent information available
			basisMap = attractingAgentSpheres;
		}
		
		ArrayList<String> sphereList = new ArrayList<String>();
		for(Entry<V, HashMap<String, Float>> entryOne: basisMap.entrySet()){
			//ensure that sphereSet is cleared for each agent
			sphereList.clear();
			//for each agent, find related spheres
			if(entryOne.getValue() != null){
				for(Entry<String, Float> entrySpheres: entryOne.getValue().entrySet()){
					if(entrySpheres.getValue() > 0){
						//add all spheres positive for particular agent into cluster set
						if(!sphereList.contains(entrySpheres.getKey())){
							sphereList.add(entrySpheres.getKey());
						}
					}
				}
			}
			if(!sphereList.isEmpty()){
				V agent = entryOne.getKey();
				if(calculateCompositeSphere){
					addAgentSphereEntry(agent, sphereList.toString());
					addSphereAgentEntry(sphereList.toString(), agent);
				} else {
					StringBuffer compositeSphere = null;
					for(String soleSphere: sphereList){
						//add sphere to agent-spheres map
						if(calculateIndividualSphere){
							addAgentSphereEntry(agent, soleSphere);
							//add agent to sphere-agents map
							addSphereAgentEntry(soleSphere, agent);
						}
						if(compositeSphere == null){
							compositeSphere = new StringBuffer();
							compositeSphere.append(soleSphere);
						} else {
							compositeSphere.append("|").append(soleSphere);
							addAgentSphereEntry(agent, compositeSphere.toString());
							addSphereAgentEntry(compositeSphere.toString(), agent);
						}
					}
				}
			}
		}
	}
	
	/**
	 * Returns a list of fully expanded cluster names (i.e. properties by 
	 * which they are clustered).
	 * @return
	 */
	public ArrayList<String> getListOfClusterCharacteristics(){
		ArrayList<String> clusterSpheres = new ArrayList<String>();
		for(Entry<V, ArrayList<String>> entry: agentSphereMap.entrySet()){
			String clusterSphere = entry.getValue().toString();
			if(!clusterSpheres.contains(clusterSphere)){
				clusterSpheres.add(clusterSphere);
			}
		}
		return clusterSpheres;
	}

	/*
	private double similarity(V vertexOne, V vertexTwo){
		float similarity = 0.0f;
		for(int i=0; i<sphereList.size(); i++){
			String sphere = sphereList.get(i);
			Float oneValue = attractingAgentSpheres.get(vertexOne).get(sphere); 
			Float twoValue = attractingAgentSpheres.get(vertexTwo).get(sphere);
			boolean similar = false;
			if(oneValue > 0 && twoValue > 0){
				similar = true;
			} else if(oneValue < 0 && twoValue < 0){
				similar = true;
			} else if(oneValue == 0 && twoValue == 0){
				similar = true;
			}
			if(similar){
				similarity++;
			}
		}
		return similarity;
	}*/
	
	/*
	private void applyClustering(){
		HierarchicalAgglomerativeClustering clusterer = new HierarchicalAgglomerativeClustering();
		Properties props = new Properties();
		props.setProperty(HierarchicalAgglomerativeClustering.MIN_CLUSTER_SIMILARITY_PROPERTY, "1.0");
		/*Assignments results = clusterer.cluster(buildMatrix(), props);
		//System.out.println("Results: " + results);
		Iterator<Assignment> it = results.iterator();
		while(it.hasNext()){
			Assignment ass = it.next();
			for(int i=0; i<ass.length(); i++){
				System.out.print(ass.assignments()[i]);
			}
			System.out.print("\n");
		}*//*
		List<Merge> list = clusterer.buildDendrogram(buildMatrix(), HierarchicalAgglomerativeClustering.ClusterLinkage.MEAN_LINKAGE);
		System.out.println(list.toString());
		
	}*/
	/*
	private Matrix buildMatrix(){
		int cols = sphereList.size();
		int rows = attractingAgentSpheres.size();
		ArrayMatrix arrMatrix = new ArrayMatrix(rows, cols);
		int rowIndex = 0;
		for(Entry<V, HashMap<String, Float>> entry: attractingAgentSpheres.entrySet()){
			for(int i=0; i<sphereList.size(); i++){
				String sphere = sphereList.get(i);
				if(entry.getValue().containsKey(sphere)){
					arrMatrix.set(rowIndex, i, entry.getValue().get(sphere));
				} else {
					arrMatrix.set(rowIndex, i, 0);
				}
			}
			rowIndex++;
		}
		return arrMatrix;
	}*/
	
	
}
