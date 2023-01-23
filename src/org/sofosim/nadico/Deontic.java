package org.sofosim.nadico;

public class Deontic {

	public String deontic;
	public float delta;
	
	public Deontic(String deontic){
		this.deontic = deontic;
		this.delta = 1.0f;
	}
	
	public Deontic(String deontic, float delta){
		this.deontic = deontic;
		this.delta = delta;
	}
	
	public boolean isObligation(){
		if(deontic.equals(Deontics.O)){
			return true;
		}
		return false;
	}
	
	public boolean isProhibition(){
		if(deontic.equals(Deontics.F)){
			return true;
		}
		return false;
	}
	
	public boolean isPermission(){
		if(deontic.equals(Deontics.P)){
			return true;
		}
		return false;
	}
	
	public Deontic invert(){
		return new Deontic(Deontics.getInversion(deontic), this.delta);
	}

	@Override
	public String toString() {
		return "Deontic [deontic=" + deontic + "]";
	}
	
	public String toShortString(){
		return deontic;
	}
	
	public String toFullString(){
		return "Deontic [deontic=" + deontic + ", delta=" + delta + "]";
	}
	
	/**
	 * Calculates hashCode for deontic excluding delta value.
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		//result = prime * result + Float.floatToIntBits(delta);
		result = prime * result + ((deontic == null) ? 0 : deontic.hashCode());
		return result;
	}

	/**
	 * Checks if two deontics are equal but ignores 
	 * deltas associated with them.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Deontic other = (Deontic) obj;
		//if (Float.floatToIntBits(delta) != Float.floatToIntBits(other.delta))
			//return false;
		if (deontic == null) {
			if (other.deontic != null)
				return false;
		} else if (!deontic.equals(other.deontic))
			return false;
		return true;
	}
	
}
