package org.sofosim.environment.memoryTypes.util;

public class ScaleDifferenceCalculator {

	/**
	 * Calculates 'absolute difference on scale', thus assuming the potential 
	 * transition via 0 value, resolving it to scale range, not absolute difference based on values! 
	 * Instead it considers whether values are left and/or right of the zero point.
	 * Additionally allows printing of debug output (calculated range on console).
	 * @param valueOne
	 * @param valueTwo
	 * @param debug
	 * @return Difference on zero-centered scale
	 */
	public static Float calculateDifferenceOnScale(Float valueOne, Float valueTwo, final boolean debug){
		float diff = 0;
		if((valueOne <= 0 && valueTwo <= 0)
				|| (valueOne >= 0 && valueTwo >= 0)) {
			//determine absolute greater number and subtract smaller one (all on same side of scale)
			diff = Math.max(Math.abs(valueOne), Math.abs(valueTwo)) - Math.min(Math.abs(valueOne), Math.abs(valueTwo));
		} else {
			//special treatment if different signs in order to calculate scale difference when crossing 0
			if(valueOne >= 0 && valueTwo <= 0){
				diff = valueOne - valueTwo;
			} else if(valueOne <= 0 && valueTwo >= 0){
				diff = valueTwo - valueOne;
			}
		}
		if(debug){
			System.out.println("Scale difference between " + valueOne + " and " + valueTwo + ": " + diff);
		}
		return diff;
	}
	
	/**
	 * Calculates 'absolute difference on scale', thus assuming the potential 
	 * transition via 0 value, resolving it to scale range, not absolute difference based on values! 
	 * Instead it considers whether values are left and/or right of the zero point.
	 * @param valueOne
	 * @param valueTwo
	 * @return
	 */
	public static Float calculateDifferenceOnScale(Float valueOne, Float valueTwo){
		return calculateDifferenceOnScale(valueOne, valueTwo, false);
	}

}
