package org.sofosim.tags;

public class Tag {

	public final String subject;
	public final String actionState;
	public final String valence;
	
	
	public Tag(String actionState){
		this.subject = null;
		this.actionState = actionState;
		this.valence = null;
	}
	
	public Tag(String actionState, String valence){
		this.subject = null;
		this.actionState = actionState;
		this.valence = valence;
	}
	
	public Tag(String subject, String action, String valence){
		this.subject = subject;
		this.actionState = action;
		this.valence = valence;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((actionState == null) ? 0 : actionState.hashCode());
		result = prime * result + ((subject == null) ? 0 : subject.hashCode());
		result = prime * result + ((valence == null) ? 0 : valence.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		/*System.out.println("Checking " + this + " against other " + obj);
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Tag other = (Tag) obj;
		if (this.actionState == null) {
			if (other.actionState != null)
				return false;
		} else if (!this.actionState.equals(other.actionState))
			return false;
		if (this.subject == null) {
			if (other.subject != null)
				return false;
		} else if (!this.subject.equals(other.subject))
			return false;
		if (this.valence == null) {
			if (other.valence != null)
				return false;
		} else if (!this.valence.equals(other.valence))
			return false;
		return true;*/
		if(obj == null){
			return false;
		}
		return this.toString().equals(obj.toString());
	}
	
	@Override
	public String toString() {
		StringBuffer output = new StringBuffer();
		if(subject != null){
			output.append("[");
			output.append(subject).append(", ");
		}
		if(actionState != null){
			output.append(actionState);
		}
		if(valence != null){
			output.append(", ").append(valence);
			output.append("]");
		}
		/*return "Tag [subject=" + subject + ", actionState=" + actionState + ", valence="
				+ valence + "]";*/
		return output.toString();
	}


}
