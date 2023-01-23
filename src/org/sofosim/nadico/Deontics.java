package org.sofosim.nadico;

public class Deontics {

	public static final String O = "OBLIGED / MUST";
	public static final String P = "PERMITTED / MAY";
	public static final String F = "FORBIDDEN / MUST NOT";
	
	public static String getPositiveDeontic(){
		return O;
	}
	
	public static String getNeutralDeontic(){
		return P;
	}
	
	public static String getNegativeDeontic(){
		return F;
	}
	
	public static String getInversion(String deontic){
		if(deontic.equals(O)){
			return F;
		}
		if(deontic.equals(P)){
			return F;
		}
		if(deontic.equals(F)){
			return O;
		}
		return null;
	}
	
}
