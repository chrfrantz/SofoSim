package org.sofosim.environment.stats;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

/**
 * Specialized Map structure allowing use of multiple StatsCalculator instances 
 * access via individual keys, which enables further grouping capabilities or 
 * specification of category-based datasets.
 * 
 * @author Christopher Frantz
 *
 * @param <T> Number specialisation for StatsCalculator instances
 */
public class StatsCalculatorMap<T extends Number> extends LinkedHashMap<String,StatsCalculator<T>>{

	/**
	 * Enters a value for a given key and category. Does not overwrite the original value.
	 * @param key Key for StatsCalculator instance
	 * @param category Key in StatsCalculator instance
	 * @param value Actual value
	 */
	public void enterValue(final String key, final String category, final T value){
		if(!containsKey(key)){
			StatsCalculator<T> calc = new StatsCalculator<>();
			calc.enterValue(category, value);
			put(key, calc);
		} else {
			get(key).enterValue(category, value);
		}
	}
	
	/**
	 * Returns the StatsCalculator instance for a given key pair.
	 * @param key Key
	 * @return StatsCalculator instance associated with key
	 */
	public StatsCalculator<T> getStatsCalculatorForKey(final String key){
		return get(key);
	}
	
	/**
	 * Clears all values held in stats calculator instances without deleting
	 * the map keys. The latter can be done by calling {@link #clear()}.
	 */
	public void clearCalculatorValues(){
		for(Entry<String, StatsCalculator<T>> entry: entrySet()){
			entry.getValue().clearAllEntries();
		}
	}
	
}
