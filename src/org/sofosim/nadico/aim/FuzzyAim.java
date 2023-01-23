package org.sofosim.nadico.aim;

import org.nzdis.it2fls.IT2FLS;
import org.sofosim.nadico.Aim;

public class FuzzyAim extends Aim {

	private IT2FLS fls = null;
	
	public FuzzyAim(IT2FLS fls){
		this.fls = fls;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fls == null) ? 0 : fls.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FuzzyAim other = (FuzzyAim) obj;
		if (fls == null) {
			if (other.fls != null)
				return false;
		} else if (!fls.equals(other.fls))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "FuzzyAim [fls=" + fls + "]";
	}

	@Override
	public String toFullString() {
		return "FuzzyAim [fls=" + fls.toFullString() + "]";
	}
	
}
