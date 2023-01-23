package org.sofosim.nadico;

import org.sofosim.nadico.aim.CrispAim;

public abstract class Aim {

	/**
	 * Indicates whether this is a crisp aim (consisting of String for instruction). 
	 * If not it is likely to contain a fuzzy set system the agent needs to invoke 
	 * in order to retrieve the action value.
	 * @return Returns true if it is a crisp aim (i.e. fixed String)
	 */
	public boolean isCrispAim(){
		//System.out.println("Static Aim? " + this.getClass().equals(CrispAim.class));
		return this.getClass().equals(CrispAim.class);
	}
	
	/**
	 * Long String representation of aim. Might be useful for more elaborate variants.
	 * @return
	 */
	public abstract String toFullString();
	
}
