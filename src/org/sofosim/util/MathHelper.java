package org.sofosim.util;

import java.util.LinkedHashMap;

import org.sofosim.structures.Pair;

public class MathHelper {

	/**
	 * Returns relative difference of second to first number, with positive 
	 * number indicating that second value is greater, and negative its lower value.
	 * @param first
	 * @param second
	 * @return relative difference (+ --> second value is bigger; - --> first is bigger)
	 */
	public static Float calculateRelativeDifference(Float first, Float second) {
		float diff = first > second ? first - second : second - first;
		float relativeDiff = diff / first;
		return first > second ? relativeDiff * -1 : relativeDiff;
	}
	
	/**
	 * Returns an pair of higher and lower boundary values for given 
	 * value and tolerance (as fraction of own value, i.e. between 0 and 1).
	 * @param baseValue
	 * @param relativeDiff
	 * @return
	 */
	public static Pair<Float, Float> getPair(Float baseValue, Float relativeDiff) {
		return new Pair<Float, Float>(baseValue - baseValue * relativeDiff, baseValue + baseValue * relativeDiff);
	}
	
	/**
	 * Returns a map of key-value entries generated from input pairs
	 * @param pairs
	 * @return
	 */
	public static LinkedHashMap<String, Float> constructMap(Pair<String, Float>... pairs) {
		LinkedHashMap<String, Float> map = new LinkedHashMap<String, Float>();
		for (int i = 0; i < pairs.length; i++) {
			map.put(pairs[i].left, pairs[i].right);
		}
		return map;
	}
	
	/**
	 * Scales input value on range between minValue and maxValue and returns the relative position 
	 * as value between 0 and 1.
	 * @param value Non-normalised value
	 * @param minValue Original range min value
	 * @param maxValue Original range max value 
	 * as lying on the boundaries (i.e. returned as 0 or 1 respectively instead of error).
	 * @return Normalised value
	 */
	public static Float normaliseValue(Float value, Float minValue, Float maxValue) {
		return normaliseValue(value, minValue, maxValue, false);
	}
	
	/**
	 * Scales input value on range between minValue and maxValue and returns the relative position 
	 * as value between 0 and 1. Optionally reports outliers (values outside of specified range) as 
	 * boundary values (i.e. 0 and 1).
	 * @param value Non-normalised value
	 * @param minValue Original range min value
	 * @param maxValue Original range max value
	 * @param reportOutlierAsExtreme Indicates if values above or below the range boundaries should be treated 
	 * as lying on the boundaries (i.e. returned as 0 or 1 respectively instead of error).
	 * @return Normalised value
	 */
	public static Float normaliseValue(Float value, Float minValue, Float maxValue, boolean reportOutlierAsExtreme) {
		if (value == null) {
			throw new RuntimeException("Value " + value + " is null.");
		}
		if (!reportOutlierAsExtreme && (value < minValue || value > maxValue)) {
			throw new RuntimeException("Value " + value + 
					" is either smaller than min value " + minValue + 
					", or larger than max value " + maxValue);
		}
		return value > maxValue ? 1.0f : (value < minValue ? 0.0f : ((value - minValue) / (maxValue - minValue)));
	}
	
	/**
	 * Projects normalised value (between 0 and 1) to given range.
	 * @param value Normalised value
	 * @param minValue Target range min value
	 * @param maxValue Target range max value
	 * @return denormalised value
	 */
	public static Float denormaliseValue(Float value, Float minValue, Float maxValue) {
		if(value == null || value < 0 || value > 1.0) {
			throw new RuntimeException("Value " + value + 
					" is either null, smaller than 0" + 
					", or larger than 1.");
		}
		return (value * (maxValue - minValue)) + minValue;
	}
	
}
