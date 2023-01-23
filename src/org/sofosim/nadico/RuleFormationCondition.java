package org.sofosim.nadico;

import java.util.ArrayList;
import org.sofosim.clustering.VertexPoint3D;

public interface RuleFormationCondition {

	/**
	 * Returns human-readable name for rule formation condition.
	 * @return
	 */
	public String getName();
	
	/**
	 * Returns a human-readable description of this Rule Formation Condition. 
	 * @return
	 */
	public String getDescription();
	
	/**
	 * Checks for condition of rule formation and returns Rule to be established or 
	 * null if condition is not met.
	 * @param cluster cluster to be checked for rule condition.
	 * @return
	 */
	public NAdico checkForRuleFormation(ArrayList<VertexPoint3D<String>> cluster);
	
	/**
	 * Returns the number of individual entries that have been considered (i.e. represented) in a rule decision.
	 * @return
	 */
	public Integer getNumberOfRepresentedIndividuals();
	
	/**
	 * Returns the number of individuals have participated in creating a rule (without necessarily being represented).
	 * @return
	 */
	public Integer getNumberOfParticipatingIndividuals();
	
}
