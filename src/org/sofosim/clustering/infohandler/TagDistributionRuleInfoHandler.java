package org.sofosim.clustering.infohandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import org.nzdis.micro.messaging.MTRuntime;
import org.sofosim.clustering.ClusterInformationHandler;
import org.sofosim.clustering.VertexPoint3D;
import org.sofosim.environment.GridSim;
import org.sofosim.environment.stats.Statistics;
import org.sofosim.nadico.CommunicationSpace;
import org.sofosim.nadico.NAdico;
import org.sofosim.tags.Tag;

/**
 * This cluster information handler implementation returns the tag distribution in the current cluster and eventual codified rules.
 * @author cfrantz
 *
 */
public class TagDistributionRuleInfoHandler implements ClusterInformationHandler<VertexPoint3D<String>> {

	private GridSim sim = null;
	
	public TagDistributionRuleInfoHandler(GridSim sim){
		this.sim = sim;
	}
	
	private final boolean useComposedTagDistribution = true;
	//tag distribution across all individual tags
	private LinkedHashMap<Tag, Integer> tagDistribution = new LinkedHashMap<Tag, Integer>();
	//tag distribution across all tag combinations
	private LinkedHashMap<String, Integer> composedTagDistribution = new LinkedHashMap<String, Integer>();
	
	@Override
	public String getClusterStatsAsString(
			ArrayList<VertexPoint3D<String>> clusterVerticesList) {
		StringBuffer out = new StringBuffer();
		out.append("Member count: ").append(clusterVerticesList.size()).append(MTRuntime.LINE_DELIMITER);
		//tag distribution
		if(useComposedTagDistribution){
			composedTagDistribution.clear();
			for(int i=0; i<clusterVerticesList.size(); i++){
				if(sim.agentDirectory.get(clusterVerticesList.get(i).vertex).getTags().isEmpty()){
					continue;
				}
				String tags = sim.agentDirectory.get(clusterVerticesList.get(i).vertex).getTags().toString();
				if(composedTagDistribution.containsKey(tags)){
					composedTagDistribution.put(tags, composedTagDistribution.get(tags) + 1);
				} else {
					composedTagDistribution.put(tags, 1);
				}
			}
			LinkedList<Map.Entry<String, Integer>> list = null;
			try{
				list = new LinkedList<Map.Entry<String, Integer>>(composedTagDistribution.entrySet());
			} catch (ConcurrentModificationException e){
				return "";
			}
			Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {

	            public int compare(Map.Entry<String, Integer> m1, Map.Entry<String, Integer> m2) {
	                return (m2.getValue()).compareTo(m1.getValue());
	            }
	        });
			for(Map.Entry<String, Integer> entry: list){
				out.append(entry).append(" | ").append(Statistics.roundThreeDecimals(entry.getValue() / (float)clusterVerticesList.size())).append(MTRuntime.LINE_DELIMITER);
			}
		} else {
			tagDistribution.clear();
			for(int i=0; i<clusterVerticesList.size(); i++){
				if(sim.agentDirectory.get(clusterVerticesList.get(i).vertex).getTags().isEmpty()){
					continue;
				}
				for(Object tag: sim.agentDirectory.get(clusterVerticesList.get(i).vertex).getTags()){
					Tag key = ((Tag)tag);	
					if(tagDistribution.containsKey(key)){
						tagDistribution.put(key, tagDistribution.get(key) + 1);
					} else {
						tagDistribution.put(key, 1);
					}
				}
			}
			LinkedList<Map.Entry<Tag, Integer>> list = new LinkedList<Map.Entry<Tag, Integer>>(tagDistribution.entrySet());
			Collections.sort(list, new Comparator<Map.Entry<Tag, Integer>>() {

	            public int compare(Map.Entry<Tag, Integer> m1, Map.Entry<Tag, Integer> m2) {
	                return (m2.getValue()).compareTo(m1.getValue());
	            }
	        });
			for(Map.Entry<Tag, Integer> entry: list){
				out.append(entry).append(" | ").append(Statistics.roundThreeDecimals(entry.getValue() / (float)clusterVerticesList.size())).append(MTRuntime.LINE_DELIMITER);
			}
		}
		//suggested rules
		/*
		HashMap<NAdico, Integer> suggestedRules = CommunicationSpace.getSuggestedRules(clusterVerticesList);
		if(suggestedRules != null && !suggestedRules.isEmpty()){
			LinkedList<Map.Entry<NAdico, Integer>> list = new LinkedList<Map.Entry<NAdico, Integer>>(suggestedRules.entrySet());
			Collections.sort(list, new Comparator<Map.Entry<NAdico, Integer>>() {

	            public int compare(Map.Entry<NAdico, Integer> m1, Map.Entry<NAdico, Integer> m2) {
	                return (m2.getValue()).compareTo(m1.getValue());
	            }
	        });
			out.append("Suggested rules: ");
			for(Map.Entry<NAdico, Integer> entry: list){
				out.append(entry).append(" | ").append(Statistics.roundThreeDecimals(entry.getValue() / (float)clusterVerticesList.size())).append(MTRuntime.LINE_DELIMITER);
			}
		}*/
		//established rules
		HashSet<NAdico> rulesInCluster = CommunicationSpace.getCodifiedRulesInCluster(clusterVerticesList.get(0).vertex);
		if(!rulesInCluster.isEmpty()){
			out.append("Rules: ").append(rulesInCluster.toString());
		}
		return out.toString();
	}

}
