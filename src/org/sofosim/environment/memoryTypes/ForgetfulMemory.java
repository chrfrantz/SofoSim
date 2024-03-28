package org.sofosim.environment.memoryTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;

import org.sofosim.environment.memoryTypes.listeners.MemoryChangeListener;
import org.sofosim.environment.stats.StatsCalculator;

/**
 * 
 * @author Christopher Frantz
 *
 * Forgetful memory concept that either reflects discounting memory or fixed-length memory. 
 *
 * @param <K> Key type used to identify memory entries by their nature
 * @param <V> Numeric type indicating feedback value associated with memory entry.
 */
public abstract class ForgetfulMemory<K,V extends Number> implements AssociativeMemoryInterface<K, V>{

	/**
	 * Owning agent
	 */
	protected String owner = "";

	/**
	 * Actual memory structure
	 */
	protected LinkedHashMap<K, V> memory = new LinkedHashMap<>();

	/**
	 * Constants to indicate standard deviation
	 */
	protected static final String STDDEV = "STDDEV";

	/**
	 * Indicates whether memory contains entries.
	 * @return
	 */
	@Override
	public boolean hasEntries(){
		return !memory.isEmpty();
	}
	
	/**
	 * Returns the standard deviation of all memory entries.
	 * @return
	 */
	@Override
	public Float getStandardDeviationOfAllEntries(){
		StatsCalculator<V> calc = new StatsCalculator<V>();
		calc.enterValues(STDDEV, new ArrayList<V>(memory.values()));
		return calc.getStdDeviation(STDDEV);
	}
	
	@Override
	public HashMap<K, V> getAllEntriesThatStartWith(
			final String startOfKeyName) {
		HashMap<K,V> returnedMap = new HashMap<>();
		final LinkedHashMap<K, V> memoryCopy = new LinkedHashMap<>();
		for(Entry<K,V> entry0: memory.entrySet()){
			memoryCopy.put(entry0.getKey(), entry0.getValue());
		}
		for(Entry<K, V> entry: memoryCopy.entrySet()){
			if(entry.getKey().toString().startsWith(startOfKeyName)){
				returnedMap.put(entry.getKey(), entry.getValue());
			}
		}
		return returnedMap;
	}

	@Override
	public HashMap<K, V> getAllEntriesThatContain(
			String containedInKeyName) {
		HashMap<K, V> returnedMap = new HashMap<>();
		final LinkedHashMap<K, V> memoryCopy = new LinkedHashMap<>();
		for(Entry<K, V> entry0: memory.entrySet()){
			memoryCopy.put(entry0.getKey(), entry0.getValue());
		}
		for(Entry<K, V> entry: memoryCopy.entrySet()){
			if(entry.getKey().toString().contains(containedInKeyName)){
				returnedMap.put(entry.getKey(), entry.getValue());
			}
		}
		return returnedMap;
	}
	
	/**
	 * Returns a list of keys whose values fall into a given threshold selection.
	 * @param threshold Threshold (from zero) to be considered.
	 * @param greaterOrSmaller Indicates if values should be greater or smaller than threshold (true: greater)
	 * @param inclusiveThreshold Indicates if the threshold value is included or excluded from selection (true: inclusive)
	 * @return
	 */
	private List<K> getAllKeysForValuesWithThreshold(final Float threshold, final boolean greaterOrSmaller, final boolean inclusiveThreshold){
		List<K> results = new ArrayList<K>();
		final LinkedHashMap<K, Float> memoryCopy = new LinkedHashMap<>();
		for(Entry<K,V> entry0: memory.entrySet()){
			memoryCopy.put(entry0.getKey(), entry0.getValue().floatValue());
		}
		if(greaterOrSmaller){
			//greater
			if(inclusiveThreshold){
				//including the threshold value itself
				for(Entry<K,Float> entry: memoryCopy.entrySet()){
					if(entry.getValue() >= threshold){
						results.add(entry.getKey());
					}
				}
			} else {
				for(Entry<K,Float> entry: memoryCopy.entrySet()){
					if(entry.getValue() > threshold){
						results.add(entry.getKey());
					}
				}
			}
		} else {
			//smaller
			if(inclusiveThreshold){
				//including the threshold itself
				for(Entry<K,Float> entry: memoryCopy.entrySet()){
					if(entry.getValue() <= threshold){
						results.add(entry.getKey());
					}
				}
			} else {
				for(Entry<K,Float> entry: memoryCopy.entrySet()){
					if(entry.getValue() < threshold){
						results.add(entry.getKey());
					}
				}
			}
		}
		return results;
	}

	/**
	 * Returns (first) memory entry that has the highest associated value.
	 * @return
	 */
	@Override
	public Entry<K, V> getEntryForHighestValue() {
		float highestVal = -Float.MAX_VALUE;
		Entry<K, V> highest = null;
		// Create a copy prior to iteration
		final LinkedHashMap<K, V> memoryCopy = new LinkedHashMap<>();
		for(Entry<K,V> entry0: memory.entrySet()){
			memoryCopy.put(entry0.getKey(), entry0.getValue());
		}
		// Iterate through memory entries to determine highest value
		for(Entry<K, V> entry: memoryCopy.entrySet()){
			if(entry.getValue().floatValue() > highestVal){
				highestVal = entry.getValue().floatValue();
				highest = entry;
			}
		}
		return highest;
	}

	/**
	 * Returns (first) memory entry that has the lowest (i.e., lowest positive/highest negative) associated value.
	 * @return
	 */
	@Override
	public Entry<K, V> getEntryForLowestValue() {
		float lowestVal = Float.MAX_VALUE;
		Entry<K, V> lowest = null;
		// Create a copy prior to iteration
		final LinkedHashMap<K, V> memoryCopy = new LinkedHashMap<>();
		for(Entry<K,V> entry0: memory.entrySet()){
			memoryCopy.put(entry0.getKey(), entry0.getValue());
		}
		// Iterate through memory entries to determine lowest value
		for(Entry<K, V> entry: memoryCopy.entrySet()){
			if(entry.getValue().floatValue() < lowestVal){
				lowestVal = entry.getValue().floatValue();
				lowest = entry;
			}
		}
		return lowest;
	}
	
	/**
	 * Returns the highest entry for a given HashMap. 
	 * Note: This is a helper function that does not operate on the internal memory.
	 * @param map Map whose highest value should be identified and entry be returned
	 * @return
	 */
	public static Entry<String, Float> getEntryForHighestValue(final HashMap<String, Float> map) {
		float highestVal = -Float.MAX_VALUE;
		Entry<String, Float> highest = null;
		for(Entry<String, Float> entry: map.entrySet()){
			if(entry.getValue() > highestVal){
				highestVal = entry.getValue();
				highest = entry;
			}
		}
		return highest;
	}

	/**
	 * Returns the lowest entry for a given HashMap. 
	 * Note: This is a helper function that does not operate on the internal memory.
	 * @param map Map whose lowest value should be identified and entry be returned
	 * @return
	 */
	public static Entry<String, Float> getEntryForLowestValue(final HashMap<String, Float> map) {
		float lowestVal = Float.MAX_VALUE;
		Entry<String, Float> lowest = null;
		for(Entry<String, Float> entry: map.entrySet()){
			if(entry.getValue() < lowestVal){
				lowestVal = entry.getValue();
				lowest = entry;
			}
		}
		return lowest;
	}
	
	/**
	 * Merges two memories' entries (by creating union of all keys and adding (summing) 
	 * values for duplicate keys). Does not modify any of the passed memories, 
	 * but creates a new HashMap that integrates both memories' entries.
	 * @param firstMemory
	 * @param secondMemory
	 * @return
	 */
	public static HashMap<String, Float> mergeMemoryEntries(final ForgetfulMemory<String, Float> firstMemory, final ForgetfulMemory<String, Float> secondMemory){
		if(firstMemory.getClass().equals(DiscreteNonAggregatingMemory.class) || secondMemory.getClass().equals(DiscreteNonAggregatingMemory.class)){
			throw new RuntimeException("No implementation for discrete non-aggregating memory.");
		}
		HashMap<String, Float> integratedMap = new HashMap<>();
		integratedMap.putAll(firstMemory.memory);
		for(Entry<String, Float> entry: secondMemory.memory.entrySet()){
			if(integratedMap.containsKey(entry.getKey())){
				integratedMap.put(entry.getKey(), integratedMap.get(entry.getKey()) + entry.getValue());
			} else {
				integratedMap.put(entry.getKey(), entry.getValue());
			}
		}
		return integratedMap;
	}
	
	/**
	 * Merges ForgetfulMemories into new memory of same subtypes (right now supported: DiscreteMemory, DiscountingMemory)
	 * @param firstMemory
	 * @param secondMemory
	 * @return
	 */
	public static ForgetfulMemory mergeMemories(final ForgetfulMemory<String, Float> firstMemory, final ForgetfulMemory<String, Float> secondMemory){
		HashMap<String, Float> integratedMap = new HashMap<>();
		if(!firstMemory.getClass().equals(secondMemory.getClass())){
			throw new RuntimeException("Merging of two different memory types not implemented. Memory 1: " + firstMemory.getClass().getSimpleName() + ", Memory 2: " + secondMemory.getClass().getSimpleName());
		}
		if(firstMemory.getClass().equals(DiscreteNonAggregatingMemory.class)){
			//Integration of array-based implementation
			for(Entry<String, Float> entry: firstMemory.getAllEntries().entrySet()){
				if(integratedMap.containsKey(entry.getKey())){
					integratedMap.put(entry.getKey(), integratedMap.get(entry.getKey()) + entry.getValue());
				} else {
					integratedMap.put(entry.getKey(), entry.getValue());
				}
			}
			for(Entry<String, Float> entry: secondMemory.getAllEntries().entrySet()){
				if(integratedMap.containsKey(entry.getKey())){
					integratedMap.put(entry.getKey(), integratedMap.get(entry.getKey()) + entry.getValue());
				} else {
					integratedMap.put(entry.getKey(), entry.getValue());
				}
			}
		} else {
			//Integration of HashMap-based implementation
			integratedMap.putAll(firstMemory.memory);
			for(Entry<String, Float> entry: secondMemory.memory.entrySet()){
				if(integratedMap.containsKey(entry.getKey())){
					integratedMap.put(entry.getKey(), integratedMap.get(entry.getKey()) + (Float)entry.getValue());
				} else {
					integratedMap.put(entry.getKey(), entry.getValue());
				}
			}
		}
		if(firstMemory.getClass().equals(DiscreteAggregatingMemory.class)){
			return new DiscreteAggregatingMemory(integratedMap, ((DiscreteAggregatingMemory)firstMemory).getNumberOfMemoryEntries(), firstMemory.getOwner());
		} else if(firstMemory.getClass().equals(DiscountingMemory.class)){
			return new DiscountingMemory(integratedMap, ((DiscountingMemory)firstMemory).getDiscountFactor(), firstMemory.getOwner());
		} else if(firstMemory.getClass().equals(CountingDiscountingMemory.class)){
			return CountingDiscountingMemory.mergeCountingMemories(((CountingDiscountingMemory)firstMemory), ((CountingDiscountingMemory)secondMemory), false);
		} else if(firstMemory.getClass().equals(DiscreteNonAggregatingMemory.class)){
			return new DiscreteNonAggregatingMemory(integratedMap, firstMemory.getNumberOfEntries(), firstMemory.getOwner());
		} else {
			throw new RuntimeException("Merging of unknown memory type requested. Type: " + firstMemory.getClass().getSimpleName());
		}
	}
	
	/**
	 * Returns a list of keys whose values are greater than a given threshold.
	 * @param threshold
	 * @return
	 */
	@Override
	public List<K> getAllKeysForValuesGreaterThan(float threshold){
		return getAllKeysForValuesWithThreshold(threshold, true, false);
	}
	
	/**
	 * Returns a list of keys whose values are smaller than a given threshold.
	 * @param threshold
	 * @return
	 */
	@Override
	public List<K> getAllKeysForValuesSmallerThan(float threshold){
		return getAllKeysForValuesWithThreshold(threshold, false, false);
	}
	
	/**
	 * Returns a list of keys whose values are greater than or equal to a given threshold.
	 * @param threshold
	 * @return
	 */
	@Override
	public List<K> getAllKeysForValuesGreaterThanOrEqualTo(float threshold){
		return getAllKeysForValuesWithThreshold(threshold, true, true);
	}
	
	/**
	 * Returns a list of keys whose values are smaller than or equal to a given threshold.
	 * @param threshold
	 * @return
	 */
	@Override
	public List<K> getAllKeysForValuesSmallerThanOrEqualTo(float threshold){
		return getAllKeysForValuesWithThreshold(threshold, false, true);
	}
	
	private LinkedHashSet<MemoryChangeListener> listeners = new LinkedHashSet<>();
	
	/**
	 * Registers a MemoryChangeListener. A MemoryChangeListener is notified upon 
	 * change to the memory (such as adding new entries or forgetting).
	 * @param listener
	 */
	public void registerMemoryChangeListener(MemoryChangeListener listener){
		this.listeners.add(listener);
	}
	
	/**
	 * Removes a registered listener. 
	 * @param listener
	 */
	public void deregisterMemoryChangeListener(MemoryChangeListener listener){
		this.listeners.remove(listener);
	}
	
	/**
	 * Notifies all currently registered listeners.
	 */
	protected void notifyMemoryChangeListeners(){
		for(MemoryChangeListener listener: this.listeners){
			listener.memoryChanged();
		}
	}
	
	/**
	 * Adds to values somewhat type-independent and precise.
	 * @param a
	 * @param b
	 * @return sum
	 */
	protected static Number addTwoValues(final Number a, final Number b){
		if(a.getClass().equals(Float.class)){
			if(!b.getClass().equals(Float.class)){
				return ((Float)a) + b.floatValue();
			}
			return ((Float)a) + ((Float)b);
		}
		if(a.getClass().equals(Integer.class)){
			if(!b.getClass().equals(Integer.class)){
				return a.floatValue() + b.floatValue();
			}
			return ((Integer)a) + ((Integer)b);
		}
		if(a.getClass().equals(Long.class)){
			return ((Long)a) + ((Long)b);
		}
		if(a.getClass().equals(Double.class)){
			if(!b.getClass().equals(Double.class)){
				return ((Double)a) + b.doubleValue();
			}
			return ((Double)a) + ((Double)b);
		}
		if(a.getClass().equals(Short.class)){
			return ((Short)a) + ((Short)b);
		}
		//let's think about the rest when errors occur
		throw new RuntimeException("Check type implementations in addTwoValues() for type " + a.getClass().getSimpleName());
	}
	
	/**
	 * Subtracts second value from first
	 * @param a
	 * @param b
	 * @return result
	 */
	protected static Number subtractTwoValues(final Number a, final Number b){
		if(a.getClass().equals(Float.class)){
			if(!b.getClass().equals(Float.class)){
				return ((Float)a) - b.floatValue();
			}
			return ((Float)a) - ((Float)b);
		}
		if(a.getClass().equals(Integer.class)){
			if(!b.getClass().equals(Integer.class)){
				return a.floatValue() - b.floatValue();
			}
			return ((Integer)a) - ((Integer)b);
		}
		if(a.getClass().equals(Long.class)){
			return ((Long)a) - ((Long)b);
		}
		if(a.getClass().equals(Double.class)){
			if(!b.getClass().equals(Double.class)){
				return ((Double)a) - b.doubleValue();
			}
			return ((Double)a) - ((Double)b);
		}
		if(a.getClass().equals(Short.class)){
			return ((Short)a) - ((Short)b);
		}
		//let's think about the rest when errors occur
		throw new RuntimeException("Check type implementations in subtractTwoValues() for type " + a.getClass().getSimpleName());
	}
	
	/**
	 * Returns this memory's owner. 
	 * @return
	 */
	public String getOwner(){
		return this.owner;
	}

	/**
	 * Returns human-readable stringified content of memory.
	 */
	@Override
	public String toString(){
		return owner + "'s memory: " + memory.size() + " entries, mean: " + getMeanOfAllEntries() + ", values: " + memory.toString();
	}
	
}
