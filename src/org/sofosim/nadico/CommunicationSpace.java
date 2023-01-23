package org.sofosim.nadico;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import javax.swing.JOptionPane;

import org.frogberry.windowPositionSaver.PositionSaver;
import org.nzdis.micro.inspector.InspectorAnnotationReflector;
import org.nzdis.micro.inspector.annotations.Inspect;
import org.nzdis.micro.messaging.MTRuntime;
import org.nzdis.micro.util.Comprehensions;
import org.nzdis.micro.util.Func;
import org.sofosim.clustering.VertexPoint;
import org.sofosim.clustering.VertexPoint3D;
import org.sofosim.environment.GridSim;
import org.sofosim.environment.annotations.SimulationParam;
import org.sofosim.environment.stats.StatsCalculator;
import org.sofosim.environment.stats.printer.StatsDataWriter;
import org.sofosim.forceLayout.ForceDirectedLayout;
import org.sofosim.graph.initializers.ForceGraphInitializer;
import org.sofosim.tags.Tag;
import sim.engine.SimState;
import sim.engine.Steppable;


public class CommunicationSpace extends ClusterKeys implements Steppable{
 
	/** allows the adoption of established rules independent from local cluster */
	@SimulationParam
	static boolean allowGlobalAdoptionOfCodifiedRules = true;
	
	/**
	 * Indicates if JOptionPane popup should be raised to notify the user 
	 * about rule establishment (incl. information on round, participating 
	 * and represented individuals)
	 */
	public static boolean notifyExperimenterUponRuleCreation = false;
	
	static final boolean printRuleComparisonDebug = false;
	
	/**
	 * Filename to which CommunicationSpace will write (and append) if
	 * a rule has been established.
	 */
	private static final String RULE_OUTPUT_FILENAME = "EstablishedRules.txt";
	
	/**
	 * Indicates if controller for UI output (to toggle for different 
	 * detail levels of labels for individual agents on forces UI) should be started
	 */
	private static boolean showOutputControlUi = true;
	
	private static NAdicoUiController nadicoController = null;
	
	public static boolean showAgentNameOnIndividualUi = false;
	/** Indicates if individuals' tags should be shown on the forces UI */
	public static boolean showTagsOnIndividualUi = false;
	/** Indicates if individuals' suggested rules should be shown on the forces UI */
	public static boolean showSuggestedRulesOnIndividualUi = false;
	/** Indicates if individuals' codified rules should be shown on the forces UI */
	public static boolean showCodifiedRulesOnIndividualUi = false;

	/** HashMap containing suggested NAdico statements by individual agents (Agent -- Rule) */
	@Inspect
	private static HashMap<String, NAdico> agentSuggestedRules = new HashMap<String, NAdico>();
	/** HashMap with key agent name and value (arbitrary_key_identifying_information -- information value) */
	@Inspect
	private static HashMap<String, HashMap<String, Object>> generalInformation = new HashMap<String, HashMap<String,Object>>();
	/** Agents with their respective tags (Agent -- Tag) */
	@Inspect
	private static HashMap<String, HashSet<Tag>> agentTags = new HashMap<String, HashSet<Tag>>();
	/** Tag distribution across all agents (Tag -- Number of occurrences) */
	@Inspect
	private static HashMap<Tag, Integer> tagDistribution = new HashMap<Tag, Integer>();
	/** HashMap holding institutional rules and constituting agents */
	@Inspect
	private static HashMap<NAdico, HashSet<String>> institutionalRulesAgents = new HashMap<NAdico, HashSet<String>>();
	/** HashMap holding agents and institutional rules relating to them */
	@Inspect
	private static HashMap<String, HashSet<NAdico>> agentsInstitutionalRules = new HashMap<String, HashSet<NAdico>>();
	/** Registered rule formation conditions to establish rule from suggested NAdico statements */
	@Inspect
	private static ArrayList<RuleFormationCondition> ruleFormationConditions = new ArrayList<RuleFormationCondition>();
	
	/**
	 * Stats Calculator for CommunicationSpace entries
	 */
	@Inspect
	private volatile static StatsCalculator<Float> statsCalc = new StatsCalculator<Float>();
	
	/**
	 * Resets all data structures and switches
	 */
	public static void reset(){
		agentSuggestedRules.clear();
		generalInformation.clear();
		agentTags.clear();
		tagDistribution.clear();
		institutionalRulesAgents.clear();
		agentsInstitutionalRules.clear();
		ruleFormationConditions.clear();
		if(statsCalc != null){
			statsCalc.shutdownListeners();
			statsCalc = null;
		}
		if(nadicoController != null){
			nadicoController.dispose();
			nadicoController = null;
		}
		l = null;
		sim = null;
		instance = null;
	}
	
	
	/** Stuff related to rule formation conditions */
	
	/**
	 * Registers a rule formation condition which is evaluated at runtime to 
	 * determine the establishment of institutional rules.
	 * @param condition Rule formation condition to be registered
	 */
	public static void registerRuleFormationCondition(RuleFormationCondition condition){
		ruleFormationConditions.add(condition);
		printOutput("Registered Rule Formation Condition '" + condition.getName() + "'.");
	}
	
	/**
	 * Deregisters a given rule formation condition.
	 * @param condition Rule formation condition to be deregistered
	 */
	public static void deregisterRuleFormationCondition(RuleFormationCondition condition){
		ruleFormationConditions.remove(condition);
	}
	
	/**
	 * Removes all rule formation conditions. 
	 */
	public static void removeAllRuleFormationConditions(){
		ruleFormationConditions.clear();
	}
	
	/**
	 * Returns all rule formation conditions.
	 * @return List of all registered rule formation conditions
	 */
	public static ArrayList<RuleFormationCondition> getRuleFormationConditions(){
		return ruleFormationConditions;
	}
	
	
	static ForceDirectedLayout l = null;
	
	private static GridSim sim = null;
	
	private volatile static CommunicationSpace instance = null;
	
	private static final String prefix = "*** CLUSTER_AGENT ***: ";
	
	private static void printOutput(String output){
		System.out.println(new StringBuffer(String.valueOf(sim.schedule.getSteps())).append(" - ").append(prefix).append(output).toString());
	}
	
	private CommunicationSpace(GridSim sim){
		if(this.sim == null){
			CommunicationSpace.sim = sim;
			l = ((ForceDirectedLayout)sim.graphHandler.getGraphInitializer(ForceGraphInitializer.FORCES_GRAPH).getLayout());
			printOutput("CommunicationSpace initialized.");
			if(showOutputControlUi){
				//start controller for output options of individuals on UI
				nadicoController = new NAdicoUiController();
				nadicoController.setVisible(true);
				PositionSaver.registerWindow(nadicoController);
			}
		}
	}
	
	/**
	 * Instantiates the singleton
	 * @param sim Simulation instance to link CommunicationSpace instance to
	 * @return
	 */
	public static CommunicationSpace getInstance(GridSim sim){
		if(instance == null){
			instance = new CommunicationSpace(sim);
		}
		return instance;
	}
	
	private static void initialize(){
		/*if(l == null){
			l = ((ForceDirectedLayout)sim.graphHandler.getGraphInitializer(ForceGraphInitializer.FORCES_GRAPH).getLayout());
		}*/
	}
	
	/**
	 * Shares a set of tags with the sender's cluster but also adds it to 
	 * the tag distribution across all agents. Passed tag information 
	 * overwrites any older information. That means that upon tag changes, the 
	 * entire tag set needs to be passed to this method to update tag information.
	 * @param sender agent sending his tags
	 * @param tags set of tags held by the sender
	 */
	public synchronized static void shareTags(String sender, HashSet<Tag> tags2){
		HashSet<Tag> oldTags = agentTags.get(sender);
		//create new HashSet (!), else comparison might not work because of generics used in IndivWeighProvider (equals() method).
		HashSet<Tag> tags = new HashSet<Tag>(tags2);
		//do nothing if all is empty
		if((oldTags == null || oldTags.isEmpty()) && (tags == null || tags.isEmpty())){
			return;
		}
		//check what tags have been removed in new tag set
		if(oldTags != null && !oldTags.isEmpty()){
			for(Tag oldTag: oldTags){
				//System.out.println("Checking for removal of " + oldTag);
				if(tags == null || tags.isEmpty() || !tags.contains(oldTag)){
					if(tagDistribution.containsKey(oldTag)){
						//System.err.println("Removed tag " + oldTag);
						if(tagDistribution.get(oldTag) == 1){
							//System.out.println("Removed entry for " + oldTag);
							tagDistribution.remove(oldTag);
						} else {
							tagDistribution.put(oldTag, tagDistribution.get(oldTag) - 1);
						}
					} else {
						//System.out.println(oldTag + " is not in " + tags);
					}
				} else {
					//System.out.println(tags + " does contain " + oldTag + " - no need to remove");
				}
			}
		}
		//check for tags missing in old tag set (i.e. have been added to agent)
		for(Tag tag: tags){
			//System.out.println("Checking tag " + tag);
			if(oldTags == null || oldTags.isEmpty() || !oldTags.contains(tag)){
				//System.err.println("Added tag " + tag);
				if(!tagDistribution.containsKey(tag)){
					tagDistribution.put(tag, 1);
				} else {
					tagDistribution.put(tag, tagDistribution.get(tag) + 1);
				}
			} else {
				//System.out.println(oldTags + " contains " + tag + " - no need to add it.");
			}
		}
		//alternative approach - removing all old ones, then adding new ones
		/*if(oldTags != null){
			for(Tag oldTag: oldTags){
				if(tagDistribution.containsKey(oldTag)){
					if(tagDistribution.get(oldTag) == 1){
						tagDistribution.remove(oldTag);
					} else {
						tagDistribution.put(oldTag, tagDistribution.get(oldTag) - 1);
					}
				}
			}
		}
		if(tags != null){
			for(Tag newTag: tags){
				if(tagDistribution.containsKey(newTag)){
					tagDistribution.put(newTag, tagDistribution.get(newTag) + 1);
				} else {
					tagDistribution.put(newTag, 1);
				}
			}
		}*/
		
		//add updated agent-tagSet assignment
		if(tags == null || tags.isEmpty()){
			agentTags.remove(sender);
		} else {
			agentTags.put(sender, tags);
		}
	}
	
	/**
	 * Returns the number of agents that carry a given tag.
	 * @param tag tag to be checked
	 * @return number of agents carrying that tag
	 */
	public static Integer getNumberOfEntitiesHoldingTag(Tag tag){
		Integer count = tagDistribution.get(tag);
		if(count == null){
			return 0;
		}
		return count;
	}
	
	/**
	 * Returns the tag distribution across all agents.
	 * @return Tag distribution as <tag, number of tag holders> map
	 */
	public static HashMap<Tag, Integer> getTagDistribution(){
		return tagDistribution;
	}
	
	/**
	 * Returns a hashmap with tags present in the same cluster as the 
	 * requester's, along with the frequency.
	 * @param sender
	 * @return
	 */
	public static HashMap<Tag, Integer> getTags(String sender){
		initialize();
		if(l != null && l.clusteringOfVertices){
			ArrayList<VertexPoint> clusterMembers = l.getProximityClusterer().getClusterNeighbours(sender);
			HashMap<Tag, Integer> tagCount = new HashMap<Tag, Integer>();
			try{
				for(int i=0; i<clusterMembers.size(); i++){
					String agent = clusterMembers.get(i).vertex.toString();
					if(agentTags.containsKey(agent)){
						HashSet<Tag> tempTags = agentTags.get(agent);
						for(Tag tag: tempTags){
							if(!tagCount.containsKey(tag)){
								tagCount.put(tag, 1);
							} else {
								tagCount.put(tag, tagCount.get(tag) + 1);
							}
						}
					}
				}
			}catch(ConcurrentModificationException e){
				tagCount = getTags(sender);
			}
			return tagCount;
		}
		return null;
	}

	/**
	 * Allows sender to suggest a rule for a given plane.
	 * @param sender sender of suggestion
	 * @param plane social plane he is referring to
	 * @param suggestedStatement NAdico statement of suggested rule
	 */
	private static void suggestRule(String sender, String plane, NAdico suggestedStatement){
		if(suggestedStatement == null){
			agentSuggestedRules.remove(sender);
			return;
		}
		initialize();
		if(l.clusteringOfVertices){ 
			agentSuggestedRules.put(sender, suggestedStatement);
		}
	}
	
	/**
	 * Returns a suggested rule for a given agent. 
	 * Returns null if no rule suggested. 
	 * @param sender
	 * @return eventual suggested rule
	 */
	public static NAdico getSuggestedRule(String sender){
		initialize();
		if(l.clusteringOfVertices){
			return agentSuggestedRules.get(sender);
		}
		return null;
	}
	
	/**
	 * Returns all suggested rules for the given list of agents (usually cluster members) 
	 * along with distribution information (within cluster).
	 * @param cluster cluster to check
	 * @return HashMap containing all suggested rules and their frequency of suggestion within cluster
	 */
	public static HashMap<NAdico, Integer> getSuggestedRules(ArrayList<VertexPoint3D<String>> clusterMembers){
		if(l != null && l.clusteringOfVertices){
			HashMap<NAdico, Integer> ruleCount = new HashMap<NAdico, Integer>();
			for(int i=0; i<clusterMembers.size(); i++){
				String agent = clusterMembers.get(i).vertex.toString();
				if(agentSuggestedRules.containsKey(agent)){
					NAdico adico = agentSuggestedRules.get(agent);
					if(!ruleCount.containsKey(adico)){
						ruleCount.put(adico, 1);
						//System.err.println("Rule is new: " + adico);
					} else {
						ruleCount.put(adico, ruleCount.get(adico) + 1);
						//System.err.println("Rule is OLD: " + ruleCount.get(adico));
					}
				}
			}
			return ruleCount;
		}
		return null;
	}
	
	/**
	 * Returns suggested nADICO rules and their frequency for the cluster a requesting agent is member of
	 * @param requester agent requesting all rules he suggested
	 * @return HashMap containing all suggested rules and their frequency of suggestion within cluster
	 */
	public static HashMap<NAdico, Integer> getSuggestedRules(String requester){
		//ForceDirectedLayout<String, RelationshipEdge> l = MaghribiSim.graphHandler.forceGraphLayout;
		initialize();
		//System.out.println("L: " + l + ", clustering: " + l.clusteringOfVertices);
		if(l != null && l.clusteringOfVertices){
			ArrayList<VertexPoint3D> clusterMembers = l.getProximityClusterer().getClusterNeighbours(requester);
			//HashSet<Tag> tags = new HashSet<Tag>();
			HashMap<NAdico, Integer> ruleCount = new HashMap<NAdico, Integer>();
			for(int i=0; i<clusterMembers.size(); i++){
				String agent = clusterMembers.get(i).vertex.toString();
				if(agentSuggestedRules.containsKey(agent)){
					NAdico adico = agentSuggestedRules.get(agent);
					//for(NAdico adico: tempTags){
						if(!ruleCount.containsKey(adico)){
							ruleCount.put(adico, 1);
						} else {
							ruleCount.put(adico, ruleCount.get(adico) + 1);
						}
					//}
				}
			}
			//System.out.println("Found " + ruleCount.size() + " rules.");
			return ruleCount;
		}
		/*if(l.clusteringOfVertices && l.clusterSecondLevel){
			//retrieve unification sphere
			ArrayList<String> mySpheres = l.getAttractionClusterer().getSpheresForAgent(requester);
			HashMap<String, NAdico> resultMap = new HashMap<String, NAdico>();
			//if sphere found
			if(mySpheres != null){
				//get all agents on that sphere
				ArrayList<String> agentsInCluster = l.getAttractionClusterer().getAgentsForSphere(mySpheres.get(0));
				for(int i=0; i<agentsInCluster.size(); i++){
					String agent = agentsInCluster.get(i);
					if(agentRule.containsKey(agent)){
						resultMap.put(agent, agentRule.get(agent));
					}
				}
			}
			return resultMap;
		}*/
		return null;
	}
	
	/**
	 * Returns indication whether a given agent has codified institutional rules.
	 * @param requester agent requesting if he has codified rules
	 * @return true indicating existence of rules; if false, no rules are codified
	 */
	public static boolean hasCodifiedRules(String requester){
		return agentsInstitutionalRules.containsKey(requester);
	}
	
	/**
	 * Gets all codified rules, independent from any cluster assignment.
	 * @return Returns all codified rules
	 */
	public static HashMap<NAdico, HashSet<String>> getCodifiedRules(){
		HashMap<NAdico, HashSet<String>> copy = (HashMap<NAdico, HashSet<String>>) institutionalRulesAgents.clone();
		return copy;
	}

	
	/**
	 * Returns formalized/codified rules for a given agent. Returns null if 
	 * no rules are assigned for the agent.
	 * @param requester agent whose rules should be returned
	 * @return returns formalized (i.e. active) rules valid for the requester.
	 */
	public static HashSet<NAdico> getCodifiedRules(String requester){
		return agentsInstitutionalRules.get(requester);
	}
	
	/**
	 * Returns codified rules in cluster (i.e. for all other members) of given agent. 
	 * @param requester Name of requesting entity
	 * @return HashSet containing all codified rules within the requester's cluster
	 */
	public static HashSet<NAdico> getCodifiedRulesInCluster(String requester){
		HashSet<NAdico> rules = new HashSet<NAdico>();
		ArrayList<VertexPoint3D> members = getClusterMembers(requester);
		if(members != null){
			for(int i=0; i<members.size(); i++){
				HashSet<NAdico> adi = getCodifiedRules(members.get(i).vertex.toString());
				if(adi != null && !adi.isEmpty()){
					rules.addAll(adi);
				}
			}
		}
		return rules;
	}
	
	/**
	 * Compares the header (or conditional part - not sanction part or nested rules) 
	 * of the passed rule against all codified rules (i.e. adopted and activated) for the given agent. 
	 * @param requester requesting agent who wants to check a given rule against rules he has adopted (e.g. to detect a rule conflict)
	 * @param ruleToBeChecked NAdico statement to be matched (header only)
	 * @return true if rule header exists in codified rules.
	 */
	public synchronized static boolean compareCodifiedRulesHeaderAgainstOwnRules(String requester, NAdico ruleToBeChecked){
		HashSet<NAdico> tempSet = getCodifiedRules(requester);
		return compareCodifiedRulesHeaderAgainstRule(tempSet, ruleToBeChecked, requester);
	}
	
	/**
	 * Similar to method @link compareCodifiedRulesHeaderAgainstOwnRules() but it checks against a set of
	 * given rules, not only the ones within the same cluster of a given agent.
	 * @param existingRules rules to be checked against (need to be provided) 
	 * @param ruleToBeChecked rule to be checked for same header across all codified rules
	 * @param requester entity requesting comparison
	 * @return true -> matching rule existing in existingRules
	 */
	private static boolean compareCodifiedRulesHeaderAgainstRule(HashSet<NAdico> existingRules, NAdico ruleToBeChecked, String requester){
		if(ruleToBeChecked == null){
			//printOutput("Rule to be checked is null (requested by " + requester + ")!");
			return false;
		}
		if(existingRules == null || existingRules.isEmpty()){
			printOutput("No codified rules to check rule header of rule " + ruleToBeChecked + "(requested by " + requester + ")!");
			return false;
		}
		if(existingRules != null && !existingRules.isEmpty()){
			for(NAdico nadico: existingRules){
				if(nadico != null){
					try{
						if(nadico.equalsOnAdicLevel(ruleToBeChecked)){
							return true;
						}
					}catch (NullPointerException e){
						System.err.println(existingRules.toString());
						System.err.println(": NADICO rule: " + nadico);
						System.err.println("Rule to check: " + ruleToBeChecked);
						e.printStackTrace();
						return false;
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * Checks for matching rule AIC (not deontics) headers ACROSS ALL AGENTS, 
	 * not only within an agent's cluster.
	 * @param adico rule to be checked against existing codified rule headers
	 * @return Rules that match on AIC level
	 */
	public static HashSet<NAdico> checkForRuleHeadersAICMatch(NAdico adico){
		if(!institutionalRulesAgents.isEmpty()){
			return checkStatementAgainstCollectionOfConventionsNormsRulesOnAICLevel(new HashSet<NAdico>(getCodifiedRules().keySet()), adico);
		} else {
			return null;
		}
	}
	
	/**
	 * Matches actions or states specified as AIC statements (or supersets of those) against 
	 * existing codified rules that are already valid for the requester and his cluster. 
	 * Comparison occurs on AIC level; deontics and sanctions are ignored.
	 * However, complete NAdico rules are returned as a result (if matching).
	 * @param requester Entity whose rulebase should be used for comparison
	 * @param adico input statement to check active rules against (only AIC components)
	 * @return Returns rules that match on AIC level with input statement.
	 */
	public static HashSet<NAdico> checkForRuleHeadersAICMatchForRequester(String requester, NAdico adico){
		if(hasCodifiedRules(requester)){
			return checkStatementAgainstCollectionOfConventionsNormsRulesOnAICLevel(getCodifiedRules(requester), adico);
		} else {
			return null;
		}
	}
	
	/**
	 * Matches actions or states specified as AIC statements against existing codified rules that 
	 * are valid within the requesters cluster (but not necessarily for the requester). 
	 * Comparison happens on AIC level; deontics and sanctions are ignored. Complete NAdico rules 
	 * are returned as a result.
	 * @param requester
	 * @param adico input statement to check active rules against
	 * @return
	 */
	public static HashSet<NAdico> checkForRuleHeadersAICMatchWithinCluster(String requester, NAdico adico){
		return checkStatementAgainstCollectionOfConventionsNormsRulesOnAICLevel(getCodifiedRulesInCluster(requester), adico);
	}
	
	/**
	 * Checks the AIC elements of a provided statement against given conventions/norms/rules. 
	 * Use case: Figuring out if convention addressing similar issue 
	 * has already been established, i.e. no need to establish an additional one.
	 * @param rules set of rules (ADICO style) 
	 * @param adico rule to test against rule set
	 * @return set of rules from rules parameter that match adico parameter on AIC level.
	 */
	private synchronized static HashSet<NAdico> checkStatementAgainstCollectionOfConventionsNormsRulesOnAICLevel(HashSet<NAdico> rules, NAdico adico){
		HashSet<NAdico> result = new HashSet<NAdico>();
		if(adico == null || rules == null){
			return result;
		}
		HashSet<NAdico> localRules = (HashSet<NAdico>) rules.clone();
		if(localRules == null || localRules.isEmpty()){
			return result;
		}
		
		if(!adico.getStatementType().equals(NAdico.AIC)){
			//printOutput(new StringBuffer("CommunicationSpace: Received non-AIC rule ").append(adico)
					//.append(" for comparison to existing statements.\nWill only compare based on AIC elements.").toString());
		}
		if(localRules != null && !localRules.isEmpty()){
			for(NAdico nadico: localRules){
				boolean match = false;
				try{
					if(adico != null && nadico != null){
						if(adico.getAttributes() != null && nadico.getAttributes() != null){
							if(adico.getAttributes().equals(nadico.getAttributes())){
								match = true;
							}
						}
						if(adico.getAim() != null && nadico.getAim() != null){
							if(adico.getAim().equals(nadico.getAim())){
								match = true;
							}
						}
						if(adico.getConditions() != null && nadico.getConditions() != null){
							if(adico.getConditions().equals(nadico.getConditions())){
								match = true;
							}
						}
						if(match){
							result.add(nadico);
						}
					}
				}catch(NullPointerException e){
					System.err.println("Rule to be tested: " + adico);
					System.err.println("Iterated rule in cluster suggestion set: " + nadico);
					System.err.println("Tested ruleset: " + localRules.toString());
					e.printStackTrace();
					return result;
				}
			}
		}
		return result;
	}
	
	/**
	 * Checks whether an agent should suggest a rule he wishes to be implemented 
	 * depending on the existence of established rules, i.e. a rule suggestion is 
	 * only submitted if no rule exists that matches the suggestion's header 
	 * (conditional statement part). 
	 * This method considers whether the global adoption of rules is activated 
	 * (and thus considers established rules outside the own cluster).
	 * @param agent Name of agent checking for rule suggestion or adoption
	 * @param rule Rule to be checked against existing rules and suggestions (and suggested if not suggested yet)
	 */
	public static void checkForSuggestionOrAdoptionOfRule(String agent, NAdico rule){
		HashSet<NAdico> rulesForAgent = getCodifiedRules(agent);
		//if agent already has rules....
		if(rulesForAgent != null && !rulesForAgent.isEmpty()){
			if(compareCodifiedRulesHeaderAgainstOwnRules(agent, rule)){
				//if agent has already subscribed to a rule matching the suggested one, don't continue to suggest anything
				suggestRule(agent, null, null);
				return;
			} else {
				//else if rule exists but headers don't match, suggest own rule
				suggestRule(agent, null, rule);
				return;
			}
		}
		//if other agents have already established rules...
		HashSet<NAdico> res = null;
		if(allowGlobalAdoptionOfCodifiedRules){
			if(!getCodifiedRules().isEmpty()){
				res = CommunicationSpace.checkForRuleHeadersAICMatch(rule);
			}
		} else {
			if(!getCodifiedRulesInCluster(agent).isEmpty()){
				res = CommunicationSpace.checkForRuleHeadersAICMatchWithinCluster(agent, rule);
			}
		}
		/*if(res == null){
			System.err.println("Thinking about applying rule " + rule + " as I haven't found any competing rule.");
		}*/
		if(res != null && !res.isEmpty()){
			if(printRuleComparisonDebug){
				System.err.println(agent + ": Candidate rules for adoption: " + res.size() + " elements");
			}
			for(NAdico nad: res){
				//compare but ignore different aim types
				if(nad.equals(rule)
						|| nad.equalsOnAcLevel(rule) 
							&& !nad.getOrElse().getAim().isCrispAim() 
							&& rule.getOrElse().getAim().isCrispAim()){
				/* adopt rule either if they fully equal on ADICO level or if they
			     * equal on ADIC level and the existing rule has a fuzzy aim in the orElse */
				//if(rule.equalsWithThisBeingFuzzyAndTheOtherCrisp(nad, true)){
					if(printRuleComparisonDebug){
						System.err.println(agent + ": Have found matching rules in cluster which I have not adopted yet.");
					}
					adoptCodifiedRule(nad, agent);
					return;
				} else {
					if(printRuleComparisonDebug){
						System.err.println(agent + ": Have found rule that differs from mine - won't adopt it.");
					}
				}
				if(printRuleComparisonDebug){
					System.err.println(agent + ": *** My rule: " + rule);
					System.err.println(agent + ": *** Existing rule: " + nad);
				}
			}
		} else {
			//suggest own rule if no rule established yet at all (and no rule subscribed to myself)
			suggestRule(agent, null, rule);
		}
	}
	
	/**
	 * Share information with other entities in reach. Added information overwrites old information that 
	 * had been stored using the same informationKey (identifier)
	 * @param requester Name of entity sharing information
	 * @param informationKey identifier for information
	 * @param informationValue information value as object
	 */
	public static void shareInformation(String requester, String informationKey, Object informationValue){
		if(generalInformation.containsKey(requester)){
			generalInformation.get(requester).put(informationKey, informationValue);
		} else {
			HashMap<String, Object> nestedMap = new HashMap<String, Object>();
			nestedMap.put(informationKey, informationValue);
			generalInformation.put(requester, nestedMap);
		}
	}
	
	/**
	 * Similar to shareInformation() method but for sharing collection of 
	 * information identified by a particular informationKey.
	 * @param requester
	 * @param informationKey identifier for information
	 * @param informationValueToBeAddedToCollection individual value to be added to collection (collection will be automatically created if not-existing)
	 */
	public static void shareInformationAsCollection(String requester, String informationKey, Object informationValueToBeAddedToCollection){
		if(generalInformation.containsKey(requester)){
			HashMap<String, Object> nestedMap = generalInformation.get(requester);
			if(nestedMap.containsKey(informationKey)){
				Collection collection = (Collection) generalInformation.get(requester).get(informationKey);
				collection.add(informationValueToBeAddedToCollection);
				nestedMap.put(informationKey, collection);
				generalInformation.put(requester, nestedMap);
			} else {
				Collection collection = new HashSet();
				collection.add(informationValueToBeAddedToCollection);
				nestedMap.put(informationKey, collection);
				generalInformation.put(requester, nestedMap);
			}
		} else {
			HashMap<String, Object> nestedMap = new HashMap<String, Object>();
			Collection collection = new HashSet();
			collection.add(informationValueToBeAddedToCollection);
			nestedMap.put(informationKey, collection);
			generalInformation.put(requester, nestedMap);
		}
	}
	
	/**
	 * Removes previously shared information from a given agent and identified by an 
	 * information key.
	 * @param requester
	 * @param informationKey identifier for information to be removed
	 */
	public static void removeInformation(String requester, String informationKey){
		if(generalInformation.containsKey(requester)){
			generalInformation.get(requester).remove(informationKey);
		}
	}
	
	/**
	 * Returns shared information of members in same cluster.
	 * @param requester entity requesting information
	 * @return HashMap containing <original member as key,<and informationKey and associated value>> as value. 
	 * The value may be collection in itself (see shareInformationAsCollection()).
	 */
	public static HashMap<String, HashMap<String, Object>> getSharedInformation(String requester){
		HashMap<String, HashMap<String, Object>> retMap = new HashMap<String, HashMap<String,Object>>();
		if(l != null && l.clusteringOfVertices){
			ArrayList<VertexPoint> clusterMembers = l.getProximityClusterer().getClusterNeighbours(requester);
			for(int i=0; i<clusterMembers.size(); i++){
				String agent = clusterMembers.get(i).vertex.toString();
				if(generalInformation.containsKey(agent)){
					retMap.put(agent, generalInformation.get(agent));
				}
			}
			return retMap;
		}
		return null;
	}
	
	/**
	 * Returns specified shared information FROM FELLOW CLUSTER MEMBERS as value aggregate. It thus gives 
	 * up the association to the originally sending member and also decomposes eventual collections to
	 * individual elements, returning as collection of objects. 
	 * @param requester entity requesting the information
	 * @param informationKey identifier for specific information
	 * @return set of objects shared within cluster (without association to sender)
	 */
	public static HashSet<Object> getAggregateValuesOfSpecifiedInformation(String requester, String informationKey){
		HashSet<Object> resultSet = new HashSet<Object>();
		if(l != null && l.clusteringOfVertices){
			ArrayList<VertexPoint> clusterMembers = l.getProximityClusterer().getClusterNeighbours(requester);
			if(clusterMembers != null){
				for(int i=0; i<clusterMembers.size(); i++){
					String agent = clusterMembers.get(i).vertex.toString();
					if(generalInformation.containsKey(agent)){
						if(generalInformation.get(agent).containsKey(informationKey)){
							if(InspectorAnnotationReflector.implementsInterface(Collection.class, generalInformation.get(agent).get(informationKey))){
								//if it is collection, treat resultSet as collection and decompose into new result collection
								resultSet.addAll((Collection)generalInformation.get(agent).get(informationKey));
							} else {
								//else add individual values
								resultSet.add(generalInformation.get(agent).get(informationKey));
							}
						}
					}
				}
				return resultSet;
			}
		}
		return null;
	}
	
	/**
	 * Returns specified shared information FROM ALL INDIVIDUALS as value aggregate. It thus gives 
	 * up the association to the originally sending member and also decomposes eventual collections to
	 * individual elements, returning as collection of objects.
	 * @param informationKey key identifying simulation-dependent information
	 * @return
	 */
	public static HashSet<Object> getAllAggregateValuesOfSpecifiedInformation(String informationKey){
		HashSet<Object> resultSet = new HashSet<Object>();
		for(Entry<String, HashMap<String, Object>> entry: generalInformation.entrySet()){
			if(generalInformation.containsKey(entry.getKey())){
				if(generalInformation.get(entry.getKey()).containsKey(informationKey)){
					if(InspectorAnnotationReflector.implementsInterface(Collection.class, generalInformation.get(entry.getKey()).get(informationKey))){
						//if it is collection, treat resultSet as collection and decompose into new result collection
						resultSet.addAll((Collection)generalInformation.get(entry.getKey()).get(informationKey));
					} else {
						//else add individual values
						resultSet.add(generalInformation.get(entry.getKey()).get(informationKey));
					}
				}
			}
		}
		return resultSet;
	}
	
	/**
	 * Indicates if calling individual is member of a cluster.
	 * @param requester calling entity
	 */
	public static boolean amClustered(String requester){
		initialize();
		if(l != null && l.clusteringOfVertices){
			ArrayList<VertexPoint> clusterMembers = l.getProximityClusterer().getClusterNeighbours(requester);
			if(clusterMembers != null){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns the cluster members for a given requester.
	 * @param requester Agent requesting his cluster members
	 * @return ArrayList of VertexPoints representing the cluster neighbours/members.
	 */
	public static ArrayList<VertexPoint3D> getClusterMembers(String requester){
		initialize();
		if(l != null && l.clusteringOfVertices){
			return l.getProximityClusterer().getClusterNeighbours(requester);
		}
		return null;
	}
	
	/**
	 * Returns information about the requester's cluster, including all information as well 
	 * as stats.
	 * @param requester entity requesting information
	 * @return StatsCalculator holding cluster stats for the requester's cluster
	 */
	public static StatsCalculator getClusterStats(String requester){
		//ForceDirectedLayout<String, RelationshipEdge> l = MaghribiSim.graphHandler.forceGraphLayout;
		initialize();
		if(l != null && l.clusteringOfVertices){
			ArrayList<VertexPoint3D> clusterMembers = l.getProximityClusterer().getClusterNeighbours(requester);
			if(clusterMembers == null){
				return null;
			}
			return getClusterStats(null, clusterMembers, null);
		}
		return null;
	}
	
	/**
	 * Returns cluster stats in StatsCalculator container
	 * @param statsCalc Container stats should be added to (can be null)
	 * @param clusterMembers List of cluster members to be analyzed (assuming all members to be in one cluster)
	 * @param suffix Text suffix used to disambiguate StatsCalculator keys for different clusters
	 * @return StatsCalculator container holding statistics for clusterMembers
	 */
	private static StatsCalculator<Float> getClusterStats(StatsCalculator<Float> statsCalc, ArrayList<VertexPoint3D> clusterMembers, String suffix){
		if(l != null && l.clusteringOfVertices){
			if(clusterMembers == null){
				return null;
			}
			suffix = ((suffix != null && !suffix.isEmpty()) ? suffix : "");
			StatsCalculator<Float> calc = null;
			if(statsCalc != null){
				calc = statsCalc;
			} else {
				calc = new StatsCalculator<Float>();
			}
			for(int i=0; i<clusterMembers.size(); i++){
				String agent = clusterMembers.get(i).vertex.toString();
				if(agentSuggestedRules.containsKey(agent)){
					NAdico adico = agentSuggestedRules.get(agent);
					calc.enterValue(DEONTIC_DELTA + suffix, adico.getDeontic().delta);
				}
				if(generalInformation.containsKey(agent)){
					for(Entry<String, Object> entry: generalInformation.get(agent).entrySet()){
						try{
							Float fl = Float.parseFloat(entry.getValue().toString());
							calc.enterValue(entry.getKey() + suffix, fl);
						}catch(NumberFormatException e){
							//if values are Strings, adding to StatsCalculator will probably fail
							break;
							//System.err.println("Error when interpreting shared information for " + agent);
							//e.printStackTrace();
						}
					}
				}
			}
			//add number of members to cluster stats
			calc.enterValue(NUMBER_OF_MEMBERS + suffix, (float)clusterMembers.size());
			return calc;
		}
		return null;
	}
	
	/**
	 * Returns statistics for all identified clusters. 
	 * @param statsCalc StatsCalculator results should be added to
	 * @param minClusterSize Minimal size of cluster to be considered in stats
	 * @return StatsCalculator containing information about all established cluster of size clusterSize and greater.
	 */
	public static StatsCalculator<Float> getAllClustersStats(StatsCalculator<Float> statsCalc, int minClusterSize){
		initialize();
		if(l != null && l.clusteringOfVertices){
			StatsCalculator<Float> calc = null;
			if(statsCalc != null){
				calc = statsCalc;
			} else {
				calc = new StatsCalculator<Float>();
			}
			ArrayList<ArrayList<VertexPoint3D>> clusterMembers = l.getProximityClusterer().getLastResultList();
			for(Integer i=0; i<clusterMembers.size(); i++){
				if(clusterMembers.get(i).size() >= minClusterSize){
					getClusterStats(calc, clusterMembers.get(i), "_Cluster_" + i.toString());
				}
			}
			return calc;
		}
		return null;
	}
	
	/**
	 * Returns cluster information as String for printing on console or in GUI.
	 * @param clusterVerticesList
	 * @return
	 */
	public static synchronized String getClusterStatsAsString(ArrayList<VertexPoint3D> clusterVerticesList){
		//determine tags per cluster
		/* to do that, use random (here: first) vertex from cluster and look up shared memory of his cluster. 
		 * Then only list proposed rules and print on centroid.
		 */
		StringBuilder builder = new StringBuilder();
		builder.append(MTRuntime.LINE_DELIMITER).append(MTRuntime.LINE_DELIMITER).append(MTRuntime.LINE_DELIMITER);
		HashMap<Tag, Integer> tagSet = CommunicationSpace.getTags(clusterVerticesList.get(0).vertex.toString());
		boolean content = false;
		//iterate to have linebreak between entries
		if(tagSet != null && !tagSet.isEmpty()){
			builder.append("Tags:").append(MTRuntime.LINE_DELIMITER);
			for(Entry<Tag, Integer> entry: tagSet.entrySet()){
				builder.append(entry.getKey()).append(" -- ").append(entry.getValue()).append(MTRuntime.LINE_DELIMITER);
			}
			content = true;
		}
		HashMap<NAdico, Integer> candidateNAdicoSet = CommunicationSpace.getSuggestedRules(clusterVerticesList.get(0).vertex.toString());
		//iterate to have linebreak between entries
		if(candidateNAdicoSet != null && !candidateNAdicoSet.isEmpty()){
			builder.append("Suggested Rules:").append(MTRuntime.LINE_DELIMITER);
			for(Entry<NAdico, Integer> entry: candidateNAdicoSet.entrySet()){
				builder.append(entry.getKey()).append(" -- ").append(entry.getValue()).append(MTRuntime.LINE_DELIMITER);
			}
			content = true;
		}
		if(content){
			StatsCalculator stats = CommunicationSpace.getClusterStats(clusterVerticesList.get(0).vertex.toString());
			builder.append("Mean Pressure: ").append(stats.getMean(DEONTIC_DELTA))
			.append(", Std dev: ").append(stats.getStdDeviation(DEONTIC_DELTA));
		}
		//check if codified rules are applicable for this cluster - and if so, calculate number of subscribers for respective rule from that cluster
		if(!institutionalRulesAgents.isEmpty()){
			
			HashMap<NAdico, Integer> rulesApplicableForCluster = new HashMap<NAdico, Integer>();
			for(Entry<NAdico, HashSet<String>> entry: institutionalRulesAgents.entrySet()){
				HashSet<String> agentSet = entry.getValue();
				int numberOfSubscribersInCluster = 0;
				
				for(int i=0; i<clusterVerticesList.size(); i++){
					String agent = clusterVerticesList.get(i).vertex.toString();
					//check if particular agent has subscribed to this rule
					if(agentSet.contains(agent)){
						numberOfSubscribersInCluster++;
					}
				}
				//save rule and number of individuals subscribed to it
				if(entry.getKey() != null && numberOfSubscribersInCluster > 0){
					rulesApplicableForCluster.put(entry.getKey(), numberOfSubscribersInCluster);
				}
			}
			if(!rulesApplicableForCluster.isEmpty()){
				builder.append(MTRuntime.LINE_DELIMITER).append("-------");
				builder.append(MTRuntime.LINE_DELIMITER).append("Codified Rules: ");
				for(Entry<NAdico, Integer> adicoEntry: rulesApplicableForCluster.entrySet()){
					try{
						builder.append(MTRuntime.LINE_DELIMITER).append(adicoEntry.getKey().toFullString()).append(": ").append(adicoEntry.getValue())
							.append(", total rule subs: ").append(institutionalRulesAgents.get(adicoEntry.getKey()).size());
					} catch(NullPointerException e){
						printOutput("NPE on Adico entry: " + adicoEntry.getKey());
						//printOutput("Full Adico entry: " + adicoEntry.getKey().toFullString());
					}
				}
			}
		}
		return builder.toString();
	}

	//structure holding memory about structure
	//HashMap<Float, Integer> clusterMemory = new HashMap<Float, Integer>();
	//HashMap<Float, Integer> previousClusterMemory = new HashMap<Float, Integer>();
	//key value pair of centroid and reputation sum
	//HashMap<Float, Point2D> centroidMemory = new HashMap<Float, Point2D>();
	//HashMap<Float, Point2D> previousCentroidMemory = new HashMap<Float, Point2D>();
	
	//int lastMemoryRound = 0;
	//final private boolean checkOnCentroidMovement = true;
	
	
	
	@Override
	public void step(SimState state) {
		
		/*
		 * Initialize if not done
		 */
		if(statsCalc == null){
			statsCalc = new StatsCalculator<>();
		}
		/*
		 * Start dynamic visualization of stats (if activated in simulation)
		 */
		if(!statsCalc.hasRegisteredListeners() && ((GridSim)state).SHOW_STATS_GRAPHS){
			statsCalc.startStatsCalculatorView();
		}
		statsCalc.clearAllEntries();
		getAllClustersStats(statsCalc, 10);
		
		//retrieve cluster
		ArrayList<ArrayList<VertexPoint3D<String>>> clusters = l.getProximityClusterer().getLastResultList();
		
		//iterate over each cluster
		for(int i=0; i<clusters.size(); i++){
			ArrayList<VertexPoint3D<String>> cluster = clusters.get(i);
			//for each cluster check if rule formation condition is fulfilled
			for(int l=0; l<ruleFormationConditions.size(); l++){
				NAdico nadico = ruleFormationConditions.get(l).checkForRuleFormation(cluster);
				//rule condition is met if a rule is returned
				if(nadico != null){
					ArrayList<String> namedClusterMembers = new ArrayList<String>(Comprehensions.map(cluster, new Func<VertexPoint3D<String>, String>(){
						
						@Override
						public String apply(VertexPoint3D in) {
							return in.vertex.toString();
						}
						
					}));
					StringBuffer buf = new StringBuffer();
					buf.append("Rule formation condition '").append(ruleFormationConditions.get(l).getName())
						.append("' is met: ").append(MTRuntime.LINE_DELIMITER)
						.append(ruleFormationConditions.get(l).getDescription())
						.append(MTRuntime.LINE_DELIMITER).append("Attempting to establish rule '")
						.append(nadico.toFullString()).append("'").append(MTRuntime.LINE_DELIMITER)
						.append(ruleFormationConditions.get(l).getNumberOfParticipatingIndividuals())
						.append(" are involved in rule creation, ").append(ruleFormationConditions.get(l).getNumberOfRepresentedIndividuals())
						.append(" are represented in decision.").append(MTRuntime.LINE_DELIMITER);
					if(notifyExperimenterUponRuleCreation){
						JOptionPane.showMessageDialog(null, "Participating: " + ruleFormationConditions.get(l).getNumberOfParticipatingIndividuals() + ", Represented: " + ruleFormationConditions.get(l).getNumberOfRepresentedIndividuals() + MTRuntime.LINE_DELIMITER + "Rule: " + nadico, "Round " + sim.schedule.getSteps(), JOptionPane.INFORMATION_MESSAGE);
					}
					printOutput(buf.toString());
					//write it
					new StatsDataWriter(RULE_OUTPUT_FILENAME, true, false).writeAndClose("Round " + sim.schedule.getSteps() + ": " + buf);
					//establish actual rule for cluster members
					fixNAdicoRule(nadico, namedClusterMembers);
				}
			}
		}
		
		/*for(ArrayList<VertexPoint3D<String>> entry: clusters){
			System.out.println(entry.toString());
		}*/
		//calculate cluster stats
		
		
		//check for action condition fulfillment
		
		/*if(reputations == null){
			reputations = new HashMap<String, Float>();
		}
		if(clusterMemory != null){
			//copy contents of previous round's results to previous memory
			previousClusterMemory = (HashMap<Float, Integer>) clusterMemory.clone();
		}
		if(centroidMemory != null){
			//copy contents of previous round's centroid memory
			previousCentroidMemory = (HashMap<Float, Point2D>) centroidMemory.clone();
		}
		//reinitialize cluster memory
		clusterMemory = new HashMap<Float, Integer>();
		centroidMemory = new HashMap<Float, Point2D>();
		
		for(Entry<String, HashMap<String, Object>> entry: generalInformation.entrySet()){
			if(entry.getValue().containsKey(InformationKeys.REPUTATION)){
				//System.out.println(entry.getValue().get(InformationKeys.REPUTATION));
				Float base = Float.parseFloat(entry.getValue().get(InformationKeys.REPUTATION).toString());
				Float numberOfAgents = 0f;
				if(entry.getValue().containsKey(InformationKeys.NUMBER_OF_AGENT_RELATIONSHIPS)){
					numberOfAgents = Float.parseFloat(entry.getValue().get(InformationKeys.NUMBER_OF_AGENT_RELATIONSHIPS).toString());
				}
				Float result = ((base * sim.REPUTATION_BASE_RATIO));// + numberOfAgents * MaghribiSim.REPUTATION_DYNAMIC_RATIO) / 2);
				reputations.put(entry.getKey(), result);
			}
		}
		//build entire reputation list (of all traders)
		StatsCalculator<Float> calc = new StatsCalculator<Float>();
		calc.enterValues(REPUTATION_KEY, reputations.values());
		//build cluster-related reputation list
		if(sim.establishCodifiedRules && l != null 
				&& l.getProximityClusterer() != null 
				&& l.getProximityClusterer().getLastResultList() != null){
			ArrayList<ArrayList<VertexPoint>> clusterResults = l.getProximityClusterer().getLastResultList();
			
			System.out.println("Number of clusters: " + clusterResults.size());
			
			//either check on changed centroid and stability of group (by monitoring group reputation), or simply check if 50% of all traders are in groups
			boolean checkForCentroidMovementAndStabilityOrElseCheckForRatio = false;
			//iterate through list of clusters (cluster = list of vertices)
			for(int i=0; i<clusterResults.size(); i++){
				ArrayList<VertexPoint> cluster = clusterResults.get(i);
				
				ArrayList<String> namedClusterMembers = new ArrayList<String>(Comprehensions.map(cluster, new Func<VertexPoint, String>(){
					
					@Override
					public String apply(VertexPoint in) {
						return in.vertex.toString();
					}
					
				}));
				
				if(checkForCentroidMovementAndStabilityOrElseCheckForRatio){
					//check for moved centroid and stable groups
					
					Float clusterReputationSum = 0f;
					if(namedClusterMembers != null){
						//System.out.println(namedClusterMembers);
						for(int l=0; l<namedClusterMembers.size(); l++){
							//sum of reputations of cluster members
							try{
								clusterReputationSum += reputations.get(namedClusterMembers.get(l));
							} catch(NullPointerException e){
								System.err.println("Reputation information for " + namedClusterMembers.get(l) + " is null");
							}
						}
					}
					//System.out.println("Previous: " + previousClusterReputationSum);
					System.out.println("Reputation: " + clusterReputationSum);
					
					int diffRounds = (int)(state.schedule.getSteps() - lastMemoryRound);
					//calculate centroid for this cluster
					Point2D centroid = ClusterUtility.calculateCentroid(cluster, l.maxClusterNeighbourDistance, GraphHandler.graphXSize, GraphHandler.graphYSize, ForceDirectedLayout.toroidal).centroid;
					//indicates that cluster has been found (based on similarity)
					boolean clusterFound = false;
					for(Entry<Float, Integer> entry: previousClusterMemory.entrySet()){
						float oldReputation = entry.getKey();
						
						System.out.println("Reputation Difference: " + (clusterReputationSum - oldReputation));
						//compare based on reputation change
						if((oldReputation - clusterReputationSum <= sim.MAX_REPUTATION_DECREASE)
								&& (clusterReputationSum - oldReputation <= sim.MAX_REPUTATION_INCREASE)){
							System.out.println("Cluster probably the same based on reputation. Still to test on centroid.");
							//compare based on centroid movement
							if(checkOnCentroidMovement){
								if(previousCentroidMemory != null && !previousCentroidMemory.isEmpty()){
									if(previousCentroidMemory.containsKey(oldReputation)){
										Point2D oldCentroid = previousCentroidMemory.get(oldReputation);
										System.out.println("Centroid Difference: " + centroid.distance(oldCentroid));
										if(l.getProximityCalculator().calculateGridDistance(centroid, oldCentroid, l.toroidal).getLength() 
												<= sim.MAX_CENTROID_MOVEMENT){
											System.out.println("Cluster probably the same based on centroid movement.");
											clusterMemory.put(clusterReputationSum, (previousClusterMemory.get(entry.getKey()) + diffRounds));
											centroidMemory.put(clusterReputationSum, centroid);
											clusterFound = true;
											break;
										}
									}
								}
							} else {
								clusterMemory.put(clusterReputationSum, (previousClusterMemory.get(entry.getKey()) + diffRounds));
							}
						}
					}
					if(previousClusterMemory.isEmpty() || !clusterFound){
						clusterMemory.put(clusterReputationSum, 0);
						if(checkOnCentroidMovement){
							centroidMemory.put(clusterReputationSum, centroid);
						}
					}
					
					//System.out.println("Rounds since last change: " + (state.schedule.getSteps() - roundsSinceLastChange));
					System.out.println("Rounds since last change: " + clusterMemory.get(clusterReputationSum));
					System.out.println("Reputation ratio for cluster: " + clusterReputationSum / calc.getSumOfEntries(REPUTATION_KEY));
					
					if(sim.establishCodifiedRules && clusterMemory.get(clusterReputationSum) != null 
							&& (clusterMemory.get(clusterReputationSum) > sim.INSTITUTION_ROUNDS_OF_STABILITY_TO_SHAPE_RULE)){
						System.out.println("Trying to fix rule using member " + namedClusterMembers.get(0));
						fixFavouredNAdicoRule(getSuggestedRules(namedClusterMembers.get(0)), namedClusterMembers);
					}
				} else {
					//check for more than 50% traders in cluster
					if(namedClusterMembers.size() >= sim.NO_OF_TRADERS / 2){
						System.out.println("More than half of the traders in cluster.");
						fixFavouredNAdicoRule(getSuggestedRules(namedClusterMembers.get(0)), namedClusterMembers);
					} /*else {
						System.out.println("Cluster has " + namedClusterMembers.size() + " members, but needs " + MaghribiSim.NO_OF_TRADERS/2);
					}*/
				/*}
			}
			//end loop
			//memorize last time memory has been executed
			lastMemoryRound = (int)state.schedule.getSteps();
		}*/
	}
	
	/**
	 * Registers a codified rule for a given new member.
	 * @param rule rule to be registered
	 * @param newMember member adopting the rule
	 */
	public static void adoptCodifiedRule(NAdico rule, String newMember){
		ArrayList<String> newMembers = new ArrayList<String>();
		newMembers.add(newMember);
		specifyInstitutionalRule(rule, newMembers);
		printOutput(new StringBuffer("Member ").append(newMember).append(" ADOPTED existing rule ").append(rule)
				.append(MTRuntime.LINE_DELIMITER).append("Now ").append(institutionalRulesAgents.get(rule).size())
				.append(" followers.").toString());
	}
	
	
	/**
	 * Establishes most popular suggested rule as codified rule - implies decision making mechanism 'absolute majority' in cluster.
	 * Checks for existence of conflicting rule (i.e. same rule header) before establishing. 
	 * SHOULD BE ABANDONED IN FAVOUR OF RuleFormationCondition (implemented for individual simulation settings)
	 * @param hashMap
	 * @param namedClusterMembers
	 */
	@Deprecated
	private static synchronized void fixFavouredNAdicoRule(HashMap<NAdico, Integer> hashMap, ArrayList<String> namedClusterMembers){
		if(hashMap == null || hashMap.isEmpty()){
			return;
		}
		int maxValue = 0;
		NAdico maxRule = null;
		//System.out.println("Have " + hashMap.size() + " candidate rules.");
		for(Entry<NAdico, Integer> entry: hashMap.entrySet()){
			if(//(entry.getKey().getStatementType().equals(NAdico.ADIC)
					//|| 
					(entry.getKey().getStatementType().equals(NAdico.ADICO))
					&& entry.getValue() > maxValue){
				maxRule = entry.getKey();
				maxValue = entry.getValue();
			}
		}
		printOutput("Identified max. rule: " + maxRule);
		if(getCodifiedRules(namedClusterMembers.get(0)) != null){
			HashSet<NAdico> rules = getCodifiedRules(namedClusterMembers.get(0));
			for(NAdico rule: rules){
				try{
					if(rule.equalsOnAdicLevel(maxRule)){
						printOutput("Will not add new rule as already have one matching one. Suggested rule: " + maxRule);
						return;
					}
				} catch(NullPointerException ex){
					System.err.println("NPE\nexisting rule: " + rule);
					System.err.println("Max rule: " + maxRule);
				}
			}
			System.err.println("Will add new rule " + maxRule);
			specifyInstitutionalRule(maxRule, namedClusterMembers);
		} else {
			System.err.println("Will add first rule " + maxRule);
			specifyInstitutionalRule(maxRule, namedClusterMembers);
		}
	}
	
	/**
	 * Fixes a given rule as institutional rule for provided cluster members. 
	 * Will only work if no ADIC-compatible rule (i.e. match on ADIC primitives) 
	 * is already registered. Should only be called as a result of evaluating
	 * registered RuleFormationConditions inside CommunicationSpace.
	 * @param nadico rule to be registered as institutional rule.
	 * @param namedClusterMembers names of cluster members that accept the rule
	 * @return
	 */
	private static boolean fixNAdicoRule(NAdico nadico, ArrayList<String> namedClusterMembers){
		HashSet<NAdico> rules = getCodifiedRules(namedClusterMembers.get(0));
		if(rules != null){
			for(NAdico rule: rules){
				try{
					if(rule.equalsOnAdicLevel(nadico)){
						printOutput("Will not add new rule as already have one matching one. Suggested rule: " + nadico);
						return false;
					}
				} catch(NullPointerException ex){
					System.err.println("NPE:" + MTRuntime.LINE_DELIMITER + prefix + "Existing rule: " + rule);
					System.err.println("Suggested new rule: " + nadico);
				}
			}
		}
		specifyInstitutionalRule(nadico, namedClusterMembers);
		printOutput("ESTABLISHED rule '" + nadico + "' for cluster of " + namedClusterMembers.size() + " agents.");
		return true;
	}
	
	/**
	 * Registers rule as codified rule for given members without further checking.
	 * Should only be called from fixNAdicoRule().
	 * @param rule rule to fix
	 * @param ruleMembers members accepting that rule
	 */
	private static void specifyInstitutionalRule(NAdico rule, Collection<String> ruleMembers){
		if(institutionalRulesAgents.containsKey(rule)){
			//HashSet<String> members = institutionalRulesAgents.get(rule);
			//members.addAll(ruleMembers);
			//institutionalRulesAgents.put(rule, members);
			institutionalRulesAgents.get(rule).addAll(ruleMembers);
		} else {
			institutionalRulesAgents.put(rule, new HashSet<String>(ruleMembers));
		}
		for(String member: ruleMembers){
			if(agentsInstitutionalRules.containsKey(member)){
				agentsInstitutionalRules.get(member).add(rule);
			} else {
				HashSet<NAdico> rules = new HashSet<NAdico>();
				rules.add(rule);
				agentsInstitutionalRules.put(member, rules);
			}
		}	
	}
	
	
	
}
