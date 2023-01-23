package org.sofosim.nadico;

import org.sofosim.nadico.aim.CrispAim;

public class NAdico {

	public static final String AIC = "AIC | Convention";
	public static final String ADIC = "ADIC | Norm";
	public static final String ADICO = "ADICO | Rule";
	public static final String NAIC = "nAIC | Nested Convention";
	public static final String NADIC = "nADIC | Nested Norm";
	public static final String NADICO = "nADICO | Nested Rule";
	public static final String INVALID = "Invalid statement";
	
	private String attributes = null;
	private Deontic deontic = null;
	private Aim aim = null;
	private String conditions = null;
	private NAdico orElse = null;
	private NAdico invokingRule = null;

	
	public NAdico(){
		
	}
	
	/**
	 * Constructor for independent 'Convention'.
	 * @param attributes
	 * @param aim
	 * @param conditions
	 */
	public NAdico(String attributes, Aim aim, String conditions){
		this.attributes = attributes;
		this.aim = aim;
		this.conditions = conditions;
	}
	
	/**
	 * Constructor for independent 'Convention'.
	 * @param attributes
	 * @param aim
	 * @param conditions
	 */
	public NAdico(String attributes, String aim, String conditions){
		this.attributes = attributes;
		this.aim = new CrispAim(aim);
		this.conditions = conditions;
	}
	
	/**
	 * Constructor for 'nested Convention'.
	 * @param invokingRule
	 * @param attributes
	 * @param aim
	 * @param conditions
	 */
	public NAdico(NAdico invokingRule, String attributes, Aim aim, String conditions){
		this.invokingRule = invokingRule;
		this.attributes = attributes;
		this.aim = aim;
		this.conditions = conditions;
	}
	
	/**
	 * Constructor for 'nested Convention'.
	 * @param invokingRule
	 * @param attributes
	 * @param aim
	 * @param conditions
	 */
	public NAdico(NAdico invokingRule, String attributes, String aim, String conditions){
		this.invokingRule = invokingRule;
		this.attributes = attributes;
		this.aim = new CrispAim(aim);
		this.conditions = conditions;
	}
	
	/**
	 * Constructor for independent Norm.
	 * @param attributes
	 * @param deontic
	 * @param aim
	 * @param conditions
	 */
	public NAdico(String attributes, Deontic deontic, Aim aim, String conditions){
		this(null, attributes, deontic, aim, conditions);
	}
	
	/**
	 * Constructor for independent Norm.
	 * @param attributes
	 * @param deontic
	 * @param aim
	 * @param conditions
	 */
	public NAdico(String attributes, Deontic deontic, String aim, String conditions){
		this(null, attributes, deontic, aim, conditions);
	}
	
	/**
	 * Constructor for 'nested Norm'.
	 * @param invokingRule
	 * @param attributes
	 * @param deontic
	 * @param aim
	 * @param conditions
	 */
	public NAdico(NAdico invokingRule, String attributes, Deontic deontic, Aim aim, String conditions){
		this.invokingRule = invokingRule;
		this.attributes = attributes;
		this.deontic = deontic;
		this.aim = aim;
		this.conditions = conditions;
	}
	
	/**
	 * Constructor for 'nested Norm'.
	 * @param invokingRule
	 * @param attributes
	 * @param deontic
	 * @param aim
	 * @param conditions
	 */
	public NAdico(NAdico invokingRule, String attributes, Deontic deontic, String aim, String conditions){
		this.invokingRule = invokingRule;
		this.attributes = attributes;
		this.deontic = deontic;
		this.aim = new CrispAim(aim);
		this.conditions = conditions;
	}
	
	/**
	 * Constructor for independent 'Rule'.
	 * @param attributes
	 * @param deontic
	 * @param aim
	 * @param conditions
	 * @param orElse
	 */
	public NAdico(String attributes, Deontic deontic, Aim aim, String conditions, NAdico orElse){
		this(null, attributes, deontic, aim, conditions, orElse);
	}
	
	/**
	 * Constructor for independent 'Rule'.
	 * @param attributes
	 * @param deontic
	 * @param aim
	 * @param conditions
	 * @param orElse
	 */
	public NAdico(String attributes, Deontic deontic, String aim, String conditions, NAdico orElse){
		this(null, attributes, deontic, aim, conditions, orElse);
	}
	
	/**
	 * Constructor for 'nested Rule'.
	 * @param invokingRule
	 * @param attributes
	 * @param deontic
	 * @param aim
	 * @param conditions
	 * @param orElse
	 */
	public NAdico(NAdico invokingRule, String attributes, Deontic deontic, Aim aim, String conditions, NAdico orElse){
		this.invokingRule = invokingRule;
		this.attributes = attributes;
		this.deontic = deontic;
		this.aim = aim;
		this.conditions = conditions;
		this.orElse = orElse;
		//make this rule the invoking one
		this.orElse.invokingRule = this;
	}
	
	/**
	 * Constructor for 'nested Rule'.
	 * @param invokingRule
	 * @param attributes
	 * @param deontic
	 * @param aim
	 * @param conditions
	 * @param orElse
	 */
	public NAdico(NAdico invokingRule, String attributes, Deontic deontic, String aim, String conditions, NAdico orElse){
		this.invokingRule = invokingRule;
		this.attributes = attributes;
		this.deontic = deontic;
		this.aim = new CrispAim(aim);
		this.conditions = conditions;
		this.orElse = orElse;
		//make this rule the invoking one
		this.orElse.invokingRule = this;
	}
	
	public String getStatementType(){
		boolean nested = false;
		String type = INVALID;
		if(invokingRule != null){
			nested = true;
			type = NADICO;
		} else {
			type = ADICO;
		}
		if(orElse == null){
			if(nested){
				type = NADIC;
			} else {
				type = ADIC;
			}
		}
		if(orElse == null && deontic == null){
			if(nested){
				type = NAIC;
			} else {
				type = AIC;
			}
		}
		return type;
	}
	
	public boolean isAicStatement(){
		String rule = getStatementType();
		if(rule.equals(AIC) || rule.equals(NAIC)){
			return true;
		} else {
			return false;
		}
	}
	
	public boolean isAdicStatement(){
		String rule = getStatementType();
		if(rule.equals(ADIC) || rule.equals(NADIC)){
			return true;
		} else {
			return false;
		}
	}
	
	public boolean isAdicoStatement(){
		String rule = getStatementType();
		if(rule.equals(ADICO) || rule.equals(NADICO)){
			return true;
		} else {
			return false;
		}
	}

	public String getAttributes() {
		return attributes;
	}

	public void setAttributes(String attributes) {
		this.attributes = attributes;
	}

	public Deontic getDeontic() {
		return deontic;
	}

	public void setDeontic(Deontic deontic) {
		this.deontic = deontic;
	}

	public Aim getAim() {
		return aim;
	}

	public void setAim(Aim aim) {
		this.aim = aim;
	}
	
	public void setAim(String aim) {
		this.aim = new CrispAim(aim);
	}

	public String getConditions() {
		return conditions;
	}

	public void setConditions(String conditions) {
		this.conditions = conditions;
	}

	public NAdico getOrElse() {
		return orElse;
	}

	public void setOrElse(NAdico orElse) {
		this.orElse = orElse;
	}

	public NAdico getInvokingRule() {
		return invokingRule;
	}

	public void setInvokingRule(NAdico invokingRule) {
		this.invokingRule = invokingRule;
	}

	/**
	 * Generates hashCode for nADICO statement but ignores the 
	 * deontic as well as aims (to allow comparison of crisp aims 
	 * with fuzzy ones).
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		/* no hashCode for aims. Else crisp and fuzzy aims are not comparable
		 * if(aim.isCrispAim()){
			//only include crisp aim in hashCode calculation - not fuzzy ones
			result = prime * result + ((aim == null) ? 0 : aim.hashCode());
		}*/
		result = prime * result
				+ ((attributes == null) ? 0 : attributes.hashCode());
		result = prime * result
				+ ((conditions == null) ? 0 : conditions.hashCode());
		//result = prime * result + ((deontic == null) ? 0 : deontic.hashCode());
		result = prime * result
				+ ((invokingRule == null) ? 0 : invokingRule.hashCode());
		result = prime * result + ((orElse == null) ? 0 : orElse.hashCode());
		return result;
	}
	
	/**
	 * Check for match on AC level, i.e. only on attributes and conditions. 
	 * This is useful if a general check on AC level is needed and an additional
	 * customized check on the aim is performed.
	 * @param ac ADICO statement (or subset)
	 * @return
	 */
	public boolean equalsOnAcLevel(NAdico ac){
		if (this == ac)
			return true;
		if (ac == null)
			return false;
		if (getClass() != ac.getClass())
			return false;
		NAdico other = (NAdico) ac;
		if (aim == null) {
			if (other.aim != null)
				return false;
		}
		if (attributes == null) {
			if (other.attributes != null)
				return false;
		} else if (!attributes.equals(other.attributes))
			return false;
		if (conditions == null) {
			if (other.conditions != null)
				return false;
		} else if (!conditions.equals(other.conditions))
			return false;
		return true;
	}
	
	/**
	 * Checks for the match on AIC level, thus ignoring the deontic and the OrElse.
	 * Note: If both aims are non-crisp ones (i.e. they are fuzzy systems), consider the aims as equal by default.
	 * @param aic ADICO statement (or subset)
	 * @return
	 */
	public boolean equalsOnAicLevel(NAdico aic){
		if (this == aic)
			return true;
		if (aic == null)
			return false;
		if (getClass() != aic.getClass())
			return false;
		NAdico other = (NAdico) aic;
		if (aim == null) {
			if (other.aim != null)
				return false;
			//if both compared aims are crisp, then compare them - else it must be fuzzy, so just accept it as equal
		} else if (!aim.equals(other.aim) && this.aim.isCrispAim() && other.aim.isCrispAim()
				|| !aim.equals(other.aim) && this.aim.isCrispAim() != other.aim.isCrispAim())
			return false;
		if (attributes == null) {
			if (other.attributes != null)
				return false;
		} else if (!attributes.equals(other.attributes))
			return false;
		if (conditions == null) {
			if (other.conditions != null)
				return false;
		} else if (!conditions.equals(other.conditions))
			return false;
		return true;
	}
	
	/**
	 * Checks for the match on ADIC level, thus ignoring the OrElse.
	 * Note: If both aims are non-crisp ones (i.e. they are fuzzy systems), consider the aims as equal by default.
	 * @param adic ADICO statement (or subset)
	 * @return
	 */
	public boolean equalsOnAdicLevel(NAdico adic){
		if (this == adic)
			return true;
		if (adic == null)
			return false;
		if (getClass() != adic.getClass())
			return false;
		NAdico other = (NAdico) adic;
		if (aim == null) {
			if (other.aim != null)
				return false;
			//if both compared aims are crisp, then compare them - else it must be fuzzy, so just accept it as equal
		} else if (!aim.equals(other.aim) && this.aim.isCrispAim() && other.aim.isCrispAim()
				|| !aim.equals(other.aim) && this.aim.isCrispAim() != other.aim.isCrispAim())
			return false;
		if (attributes == null) {
			if (other.attributes != null)
				return false;
		} else if (!attributes.equals(other.attributes))
			return false;
		if (conditions == null) {
			if (other.conditions != null)
				return false;
		} else if (!conditions.equals(other.conditions))
			return false;
		if (deontic == null) {
			if (other.deontic != null)
				return false;
		} else if (!deontic.equals(other.deontic))
			return false;
		return true;
	}
	
	/**
	 * Checks for the match of the full ADICO sequence.
	 * Note: If both aims are non-crisp ones (i.e. they are fuzzy systems), consider the aims as equal by default.
	 * If you want to treat crisp and fuzzy aims as equal, use equalsIgnoringCrispVsFuzzyAim().
	 * @param adico
	 * @return
	 */
	public boolean equalsOnAdicoLevel(NAdico adico){
		return equals(adico);
	}
	
	/**
	 * Checks for sameness of two full nADICO statements but 
	 * treats different aim types (e.g. Crisp vs. Fuzzy aim) 
	 * as equal. Crisp types are directly compared. If comparing 
	 * different aim types, the check fails if this is crisp aim 
	 * and the other is not. If this is fuzzy and the other is not, 
	 * the test succeeds. (Background: Only allow adoption of 
	 * fuzzy aims, not of crisp aims).
	 * ATTENTION: This check has a wider range of semantic checks than equals().
	 * @param obj Object to be compared
	 * @param trace true indicates that matching should be traced and indicate where it fails (for debugging)
	 * @return
	 *//*
	public boolean equalsWithThisBeingFuzzyAndTheOtherCrisp(Object obj, boolean trace) {
		if (this == obj)
			return true;
		if (obj == null){
			if(trace){
				System.out.println("nADICO object comparison failed: nADICO Comparison object is null!");
			}
			return false;
		}
		if (getClass() != obj.getClass()){
			if(trace){
				System.out.println("nADICO object comparison failed: nADICO object is of different class (" + linebreak + "*MINE*: " + getClass().getSimpleName() + ", " 
						+ linebreak + "*OTHER*: " + obj.getClass().getSimpleName()); 
			}
			return false;
		}
		NAdico other = (NAdico) obj;
		if (aim == null) {
			if (other.aim != null){
				if(trace){
					System.out.println("nADICO object comparison failed: My aim is null, other's is : " + other.aim); 
				}
				return false;
			}
			//if both compared aims are crisp, then compare them - else at least one must be fuzzy, so just accept it as equal
		} else if (!aim.equals(other.aim) && this.aim.isCrispAim() && other.aim.isCrispAim()
				//fail if this is crisp aim and the other is not crisp (i.e. fuzzy). Fuzzy is superset of crisp, not the other way around. 
				|| !aim.equals(other.aim) && !this.aim.isCrispAim() &&  other.aim.isCrispAim()
				){
			if(trace){
				System.out.println("nADICO object comparison failed: Both aims are either crisp and different, or mine is fuzzy and the other crisp. (" + linebreak + "*MINE*: " + aim + ", " 
						+ linebreak + "*OTHER*: " + other.aim + ")"); 
			}
			return false;
		}
		if (attributes == null) {
			if (other.attributes != null){
				if(trace){
					System.out.println("nADICO object comparison failed: My attributes are null, other's are : " + other.attributes); 
				}
				return false;
			}
		} else if (!attributes.equals(other.attributes)){
			if(trace){
				System.out.println("nADICO object comparison failed: My attributes and others' differ: (" + linebreak + "*MINE*: " + attributes + ", " 
						+ linebreak + "*OTHER*: " + other.attributes + ")"); 
			}
			return false;
		}
		if (conditions == null) {
			if (other.conditions != null){
				if(trace){
					System.out.println("nADICO object comparison failed: My conditions are null, other's are : " + other.conditions); 
				}
				return false;
			}
		} else if (!conditions.equals(other.conditions)){
			if(trace){
				System.out.println("nADICO object comparison failed: My conditions and others' differ: (" + linebreak + "*MINE*: " + conditions + ", " 
						+ linebreak + "*OTHER*: " + other.conditions + ")"); 
			}
			return false;
		}
		if (deontic == null) {
			if (other.deontic != null){
				if(trace){
					System.out.println("nADICO object comparison failed: My deontic is null, other's is: " + other.deontic); 
				}
				return false;
			}
		} else if (!deontic.equals(other.deontic)){
			if(trace){
				System.out.println("nADICO object comparison failed: My deontic and other's differ: (" + linebreak + "*MINE*: " + deontic + ", " 
						+ linebreak + "*OTHER*: " + other.deontic + ")"); 
			}
			return false;
		}
		if (invokingRule == null) {
			if (other.invokingRule != null){
				if(trace){
					System.out.println("nADICO object comparison failed: My invoking rule is null, other's is: " + other.invokingRule); 
				}
				return false;
			}
		} else if (!invokingRule.equals(other.invokingRule)){
			if(trace){
				System.out.println("nADICO object comparison failed: My invoking rule and other's differ: (" + linebreak + "*MINE*: " + invokingRule + ", " 
						+ linebreak + "*OTHER*: " + other.invokingRule + ")"); 
			}
			return false;
		}
		if (orElse == null) {
			if (other.orElse != null){
				if(trace){
					System.out.println("nADICO object comparison failed: My orElse / consequent is null, other's is: " + other.orElse); 
				}
				return false;
			}
		} else if (!orElse.equalsWithThisBeingFuzzyAndTheOtherCrisp(other.orElse, trace)){
			if(trace){
				System.out.println("nADICO object comparison failed: My orElse and other's differ: (" + linebreak + "*MINE*: " + orElse + ", " 
						+ linebreak + "*OTHER*: " + other.orElse + ")"); 
			}
			return false;
		}
		return true;
	}*/
	
	/**
	 * Generic equals() method checks for full ADICO equivalence.
	 * If both statements' aims are non-static (i.e. fuzzy), the aims are considered equal.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NAdico other = (NAdico) obj;
		if (aim == null) {
			if (other.aim != null)
				return false;
			//if both compared aims are crisp, then compare them - else it must be fuzzy, so just accept it as equal
		} else if (!aim.equals(other.aim) && this.aim.isCrispAim() && other.aim.isCrispAim()
				|| !aim.equals(other.aim) && this.aim.isCrispAim() != other.aim.isCrispAim())
			return false;
		if (attributes == null) {
			if (other.attributes != null)
				return false;
		} else if (!attributes.equals(other.attributes))
			return false;
		if (conditions == null) {
			if (other.conditions != null)
				return false;
		} else if (!conditions.equals(other.conditions))
			return false;
		if (deontic == null) {
			if (other.deontic != null)
				return false;
		} else if (!deontic.equals(other.deontic))
			return false;
		if (invokingRule == null) {
			if (other.invokingRule != null)
				return false;
		} else if (!invokingRule.equals(other.invokingRule))
			return false;
		if (orElse == null) {
			if (other.orElse != null)
				return false;
		} else if (!orElse.equals(other.orElse))
			return false;
		return true;
	}
	
	private static final String linebreak = System.getProperty("line.separator");

	@Override
	public String toString() {
		/*StringBuffer out = new StringBuffer();
		out.append("NAdico [attributes=").append(attributes).append(", ").append(linebreak);
		out.append("deontic=").append(deontic).append(", ").append(linebreak);
		out.append("aim=").append(aim).append(", ").append(linebreak);
		out.append("conditions=").append(conditions).append(", ").append(linebreak);
		out.append("invokingRule=").append(invokingRule).append(", ").append(linebreak);
		out.append("getStatementType()=").append(getStatementType()).append(", ").append(linebreak);
		out.append("orElse=");
		if(orElse != null){
			out.append(linebreak);
		}
		out.append(orElse).append("]");
		return out.toString();
		*/
		StringBuffer out = new StringBuffer();
		out.append("NAdico [attributes=").append(attributes).append(", ");
		out.append("deontic=").append(deontic.toFullString()).append(", ").append(linebreak);
		out.append("aim=").append(aim).append(", ");
		out.append("conditions=").append(conditions).append(", ");
		out.append("invokingRule=").append(invokingRule).append(", ");
		out.append("getStatementType()=").append(getStatementType()).append(", ").append(linebreak);
		out.append("orElse=");
		if(orElse != null){
			out.append(linebreak);
		}
		out.append(orElse).append("]");
		return out.toString();
		/*
		return "NAdico [attributes=" + attributes 
				+ ", deontic=" + deontic + linebreak
				+ ", aim=" + aim 
				+ ", conditions=" + conditions 
				+ ", invokingRule=" + invokingRule
				+ ", getStatementType()=" + getStatementType() + linebreak + 
				", orElse="+ ((orElse != null) ? linebreak: "") + orElse +"]";*/
	}
	
	public String toFullString() {
		StringBuffer out = new StringBuffer();
		out.append("NAdico [attributes=").append(attributes).append(", ").append(linebreak);
		out.append("deontic=").append(deontic.toFullString()).append(", ").append(linebreak);
		out.append("aim=").append(aim.toFullString()).append(", ").append(linebreak);
		out.append("conditions=").append(conditions).append(", ").append(linebreak);
		out.append("invokingRule=").append(invokingRule).append(", ").append(linebreak);
		out.append("getStatementType()=").append(getStatementType()).append(", ").append(linebreak);
		out.append("orElse=");
		if(orElse != null){
			out.append(linebreak);
		}
		out.append(orElse).append("]");
		return out.toString();
		/*return "NAdico [attributes=" + attributes 
				+ ", deontic=" + deontic.toFullString() + linebreak
				+ ", aim=" + aim 
				+ ", conditions=" + conditions 
				+ ", invokingRule=" + invokingRule
				+ ", getStatementType()=" + getStatementType() + linebreak + 
				", orElse="+ ((orElse != null) ? linebreak: "") + orElse +"]";*/
	}
	
	public String toShortString(){
		return new StringBuffer("NAdico: '").append(attributes) 
				.append("' '").append(deontic.toShortString())
				.append("' '").append(aim)
				.append("' '").append(conditions)
				.append("' ").append(((orElse != null) ? "or else: " + orElse.toShortString() : "'null'")).toString();
	}
	
}
