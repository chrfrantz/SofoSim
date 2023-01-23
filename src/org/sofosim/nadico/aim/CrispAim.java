package org.sofosim.nadico.aim;

import org.sofosim.nadico.Aim;

public class CrispAim extends Aim {

	private String aim;
	
	public CrispAim(String aim){
		this.aim = aim;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((aim == null) ? 0 : aim.hashCode());
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
		CrispAim other = (CrispAim) obj;
		if (aim == null) {
			if (other.aim != null)
				return false;
		} else if (!aim.equals(other.aim))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return this.aim;
	}

	@Override
	public String toFullString() {
		return toString();
	}
	
}
