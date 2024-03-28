package org.sofosim.environment.memoryTypes;

import java.util.*;
import java.util.Map.Entry;

import org.nzdis.micro.util.DataStructurePrettyPrinter;
import org.sofosim.environment.memoryTypes.util.MapValueComparator;
import org.sofosim.environment.stats.StatsCalculator;

/**
 * Maintains fixed-length array-based approach to storing 
 * a fixed number of memory entries. Iterates over array 
 * elements and updates, so no explicit deletion is required.
 * Evaluation methods accessing values by key return aggregate 
 * values for that key across the array. However, the values 
 * are not stored as aggregated values, which is unlike 
 * DiscreteAggregatingMemory.
 * 
 * @author Christopher Frantz
 *
 */
public class DiscreteNonAggregatingMemory<K,V extends Number> extends ForgetfulMemory<K,V>{

	/**
	 * Number of entries that can in principle be accessed.
	 */
	private Integer numberOfEntries = null;

	/**
	 * Actual memory array.
	 */
	protected MemoryEntry<K, V>[] memoryArray;

	/**
	 * Internal counter for memory array slot access.
	 */
	private int currentCounter = 0;

	/**
	 * Indicates that memory entries have been saved (written).
	 */
	private boolean hasEntries = false;
	
	public DiscreteNonAggregatingMemory(Integer numberOfEntries){
		setNumberOfMemoryEntries(numberOfEntries);
	}
	
	public DiscreteNonAggregatingMemory(Integer numberOfEntries, String owner){
		this(numberOfEntries);
		this.owner = owner;
	}
	
	public DiscreteNonAggregatingMemory(HashMap<K, V> entries, Integer numberOfEntries, String owner){
		this(numberOfEntries, owner);
		if(entries.size() > this.numberOfEntries){
			throw new RuntimeException("Tried to initialize DiscreteAggregationMemory with greater number than permissible memory entries.");
		}
		this.memoryArray = new MemoryEntry[this.numberOfEntries];
		int i = 0;
		for(Entry<K, V> entry: entries.entrySet()){
			this.memoryArray[i] = new MemoryEntry(entry.getKey(), entry.getValue());
			i++;
		}
		notifyMemoryChangeListeners();
	}
	
	protected class MemoryEntry<K,V> {
		
		public K key = null;
		public V value = null;
		
		public MemoryEntry(K key, V value){
			this.key = key;
			this.value = value;
		}

		@Override
		public String toString() {
			return "MemoryEntry [key=" + key + ", value=" + value + "]";
		}
		
	}
	
	/**
	 * Memorizes a value for a given agent, removing
	 * the last element from the queue.
	 * Important: If you continue to use the key in the calling instance, 
	 * ensure you save a copy to the memory to prevent modification after storing values. 
	 */
	public void memorize(K key, V value) {
		//replace old entry
		memoryArray[currentCounter] = new MemoryEntry<K,V>(key, value);
		currentCounter++;
		if(currentCounter == memoryArray.length){
			currentCounter = 0;
		}
		//notify change listeners
		notifyMemoryChangeListeners();
		//adjust memory write flag
		if (!hasEntries) {
			hasEntries = true;
		}
	}

	/**
	 * Indicates whether memory has entries.
	 * @return
	 */
	@Override
	public boolean hasEntries() {
		return hasEntries;
	}

	/**
	 * Returns mean value of all memory entries.
	 * @return
	 */
	@Override
	public Double getMeanOfAllEntries() {
		Double sum = 0.0;
		for(int i = 0; i < memoryArray.length; i++){
			if(memoryArray[i] != null){
				sum += memoryArray[i].value.doubleValue();
			}
		}
		return sum / new Integer(memoryArray.length).floatValue();
	}

	/**
	 * Indicates whether memory contains entry with given key.
	 * @param key Key of memory entry
	 * @return
	 */
	@Override
	public boolean containsKey(K key) {
		for(int i = 0; i < memoryArray.length; i++){
			if(memoryArray[i] != null && memoryArray[i].key.equals(key)){
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns all memory entries as HashMap. Aggregates values 
	 * for duplicate keys.
	 * @return
	 */
	@Override
	public HashMap<K, V> getAllEntries() {
		HashMap<K, V> returnedMemory = new HashMap<>();
		for(int i = 0; i < memoryArray.length; i++){
			if(memoryArray[i] != null){
				if(returnedMemory.containsKey(memoryArray[i].key)){
					returnedMemory.put(memoryArray[i].key, (V) addTwoValues(memoryArray[i].value, returnedMemory.get(memoryArray[i].key)));
				} else {
					returnedMemory.put(memoryArray[i].key, memoryArray[i].value);
				}
			}
		}
		return returnedMemory;
	}

	/**
	 * Returns the number of possible entries in memory (memory slots) - independent of usage.
	 * @return
	 */
	@Override
	public Integer getNumberOfEntries() {
		return memoryArray.length;
	}

	/**
	 * Returns aggregated value for given entry
	 * @param agent
	 * @return
	 */
	@Override
	public Float getValueForKey(K agent) {
		Float retValue = 0.0f;
		for(int i = 0; i < memoryArray.length; i++){
			if(memoryArray[i] != null && memoryArray[i].key.equals(agent)){
				retValue += memoryArray[i].value.floatValue();
			}
		}
		return retValue;
	}
	
	/**
	 * Use {@link #getKeyValuePairForHighestValue()} instead when using DiscreteNonAggregatingMemory.
	 */
	@Override
	public Entry<K, V> getEntryForHighestValue() {
		throw new RuntimeException("Use getKeyValuePairForHighestValue() in DiscreteNonAggregatingMemory, since it uses different memory structure than ForgetfulMemory.");
	}
	
	/**
	 * Use {@link #getKeyValuePairForLowestValue()} instead when using DiscreteNonAggregatingMemory.
	 */
	@Override
	public Entry<K, V> getEntryForLowestValue() {
		throw new RuntimeException("Use getKeyValuePairForLowestValue() in DiscreteNonAggregatingMemory, since it uses different memory structure than ForgetfulMemory.");
	}
	
	/**
	 * Returns key-value pair for highest value.
	 * @return
	 */
	public Pair getKeyValuePairForHighestValue() {
		return getExtremeKeyValueEntry(true);
	}
	
	/**
	 * Returns key-value pair for lowest value.
	 * @return
	 */
	public Pair getKeyValuePairForLowestValue() {
		return getExtremeKeyValueEntry(false);
	}

	/**
	 * Returns key for the highest value.
	 */
	@Override
	public K getKeyForHighestValue() {
		return getExtremeKeyValueEntry(true).key;
	}

	/**
	 * Returns key for the lowest value.
	 */
	@Override
	public K getKeyForLowestValue() {
		return getExtremeKeyValueEntry(false).key;
	}
	
	private Pair getExtremeKeyValueEntry(boolean highestOrLowest){
		K key = null;
		Float extremeValue = null;
		HashMap<K, Float> map = new HashMap<>();
		if(highestOrLowest){
			//highest
			extremeValue = -Float.MAX_VALUE;
		} else {
			//lowest
			extremeValue = Float.MAX_VALUE;
		}
		//aggregating all array entries
		for(int i = 0; i < memoryArray.length; i++){
			if(memoryArray[i] != null){
				if(map.containsKey(memoryArray[i].key)){
					map.put(memoryArray[i].key, memoryArray[i].value.floatValue() + map.get(memoryArray[i].key).floatValue());
				} else {
					map.put(memoryArray[i].key, memoryArray[i].value.floatValue());
				}
			}
		}
		//finally iterate over aggregated map
		for(Entry<K, Float> entry: map.entrySet()){
			if(highestOrLowest){
				//highest		
				if(entry.getValue() > extremeValue){
					extremeValue = entry.getValue();
					key = entry.getKey();
				}
			} else {
				//lowest
				if(entry.getValue() < extremeValue){
					extremeValue = entry.getValue();
					key = entry.getKey();
				}
			}
		}
		if (key == null) {
			return null;
		}
		return new Pair(key, (V)extremeValue);
	}
	
	public class Pair implements Comparable<Pair>{
		
		public K key = null;
		public V value = null;
		
		public Pair(K key, V value){
			this.key = key;
			this.value = value;
		}
		
		public K getKey() {
			return key;
		}
		
		public V getValue() {
			return value;
		}

		@Override
		public String toString() {
			return "Pair [key=" + key + ", value=" + value + "]";
		}

		@Override
		public int compareTo(DiscreteNonAggregatingMemory<K, V>.Pair o) {
			return new Double(this.getValue().doubleValue()).compareTo(new Double(o.getValue().doubleValue()));
		}
		
	}
	
	/**
	 * Returns a list of keys whose values fall into a given threshold selection.
	 * @param threshold Threshold (from zero) to be considered.
	 * @param greaterOrSmaller Indicates if values should be greater or smaller than threshold (true: greater)
	 * @param inclusiveThreshold Indicates if the threshold value is included or excluded from selection (true: inclusive)
	 * @return
	 */
	private List<K> getAllKeysForValuesWithThreshold(Float threshold, boolean greaterOrSmaller, boolean inclusiveThreshold){
		throw new RuntimeException("Threshold-based key aggregation not implemented for DiscreteNonAggregatingMemory.");
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
	
	@Override
	public Float getStandardDeviationOfAllEntries(){
		StatsCalculator<Float> calc = new StatsCalculator<Float>();
		for(int i = 0; i < memoryArray.length; i++){
			if(memoryArray[i] != null){
				calc.enterValue(STDDEV, memoryArray[i].value.floatValue());
			}
		}
		return calc.getStdDeviation(STDDEV);
	}
	
	/**
	 * Returns a map of entries whose keys start with a specified string. 
	 * Aggregates values that share the same key. 
	 */
	@Override
	public HashMap<K, V> getAllEntriesThatStartWith(
			String startOfKeyName) {
		HashMap<K, V> returnedMap = new HashMap<>();
		for(int i = 0; i < memoryArray.length; i++){
			if(memoryArray[i] != null && memoryArray[i].key.toString().startsWith(startOfKeyName)){
				if(returnedMap.containsKey(memoryArray[i].key)){
					returnedMap.put(memoryArray[i].key, (V) addTwoValues(memoryArray[i].value, returnedMap.get(memoryArray[i].key)));
				} else {
					returnedMap.put(memoryArray[i].key, memoryArray[i].value);
				}
			}
		}
		return returnedMap;
	}

	/**
	 * Returns a map of entries whose keys contain a specified string. 
	 * Aggregates values that share the same key. 
	 */
	@Override
	public HashMap<K, V> getAllEntriesThatContain(
			String containedInKeyName) {
		HashMap<K, V> returnedMap = new HashMap<>();
		for(int i = 0; i < memoryArray.length; i++){
			if(memoryArray[i] != null && memoryArray[i].key.toString().contains(containedInKeyName)){
				if(returnedMap.containsKey(memoryArray[i].key)){
					returnedMap.put(memoryArray[i].key, (V) addTwoValues(memoryArray[i].value, returnedMap.get(memoryArray[i].key)));
				} else {
					returnedMap.put(memoryArray[i].key, memoryArray[i].value);
				}
			}
		}
		return returnedMap;
	}
	
	/**
	 * Returns the number of maintained memory entries.
	 * @return
	 */
	public Integer getNumberOfMemoryEntries(){
		return this.numberOfEntries;
	}
	
	/**
	 * Updates the number of memory entries (to change memory behaviour at runtime).
	 * @param updatedNumberOfMemoryEntries New number of memory entries
	 */
	public void setNumberOfMemoryEntries(Integer updatedNumberOfMemoryEntries){
		this.numberOfEntries = updatedNumberOfMemoryEntries;
		//initialize array used for DiscreteNonAggregatingMemory
		this.memoryArray = new MemoryEntry[this.numberOfEntries];
		notifyMemoryChangeListeners();
	}

	/**
	 * Forgets excess entries at end of round. Parameter is 
	 * not of effect in this implementation. Uses memory 
	 * length specified in constructor.
	 */
	@Override
	public void forgetAtRoundEnd(float unusedParameterForDiscreteMemory) {
		//nothing happens as values are overridden by reiterating over array
	}

	@Override
	public String toString() {
		return "DiscreteNonAggregatingMemory: " + System.getProperty("line.separator") + 
				DataStructurePrettyPrinter.decomposeRecursively(memoryArray, null) + 
				System.getProperty("line.separator");
	}

}
