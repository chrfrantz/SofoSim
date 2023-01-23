package org.sofosim.environment.memoryTypes;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;


public class CountingDiscountingMemory<K, V extends Number> extends DiscountingMemory<K, V> {

	HashMap<K, Integer> memoryCount = new HashMap<>();
	
	public CountingDiscountingMemory(Float discountFactor, String owner) {
		super(discountFactor, owner);
	}

	/**
	 * Memorize an agent score. Will add score to old score and 
	 * additionally memorize number of calls for the given key.
	 * @param key
	 * @param value
	 */
	@Override
	public void memorize(K key, V value){
		saveCountForKey(key);
		super.memorize(key, value);
	}
	
	/**
	 * Increments count for memory value associated with key 
	 * by 1.
	 * @param key Memory key associated with count
	 */
	private void saveCountForKey(K key){
		if(!memoryCount.containsKey(key)){
			memoryCount.put(key, 1);
		} else {
			memoryCount.put(key, memoryCount.get(key) + 1);
		}
	}
	
	/**
	 * Increments count for memory value associated with key 
	 * by one or other value if specified. If value is null,
	 * count is increments by 1.
	 * @param key Memory key associated with count
	 * @param value Increment on current count for memory entry
	 */
	private void saveCountForKey(K key, Integer value){
		if(!memoryCount.containsKey(key)){
			memoryCount.put(key, (value == null ? 1 : value));
		} else {
			memoryCount.put(key, memoryCount.get(key) + (value == null ? 1 : value));
		}
	}
	
	/**
	 * Returns the number of calls for a particular 
	 * memory key. 
	 * @param key
	 * @return
	 */
	public Integer getCountForKey(String key){
		return memoryCount.get(key);
	}
	
	/**
	 * Merges a second memory into the current one.
	 * @param countingMemory
	 */
	public void mergeMemory(CountingDiscountingMemory<K, V> countingMemory){
		for(Entry<K, V> entry: countingMemory.memory.entrySet()){
			if(memory.containsKey(entry.getKey())){
				memory.put(entry.getKey(), (V) addTwoValues(memory.get(entry.getKey()), entry.getValue()));
			} else {
				memory.put(entry.getKey(), entry.getValue());
			}
			//also add up memorization counts
			saveCountForKey(entry.getKey(), countingMemory.memoryCount.get(entry.getKey()));
		}
	}
	
	/**
	 * Merges two memories and returns a new combined memory instance. The owner of the 
	 * first memory will be set as owner of the new memory.
	 * @param countingMemoryOne First memory
	 * @param countingMemoryTwo Second memory
	 */
	public static CountingDiscountingMemory mergeCountingMemories(CountingDiscountingMemory countingMemoryOne, CountingDiscountingMemory<String, Float> countingMemoryTwo, boolean invertAddition){
		CountingDiscountingMemory baseMemory = new CountingDiscountingMemory(countingMemoryOne.getDiscountFactor(), countingMemoryOne.owner);
		baseMemory.memory = (LinkedHashMap<String, Float>) countingMemoryOne.memory.clone();
		baseMemory.memoryCount = (HashMap<String, Integer>) countingMemoryOne.memoryCount.clone();
		for(Entry<String, Float> entry: countingMemoryTwo.memory.entrySet()){
			if(baseMemory.containsKey(entry.getKey())){
				if(invertAddition){
					baseMemory.memory.put(entry.getKey(), subtractTwoValues((Number) baseMemory.memory.get(entry.getKey()), entry.getValue()));
				} else {
					baseMemory.memory.put(entry.getKey(), addTwoValues((Number) baseMemory.memory.get(entry.getKey()), entry.getValue()));
				}
			} else {
				baseMemory.memory.put(entry.getKey(), entry.getValue());
			}
			//also add up memorization counts
			baseMemory.saveCountForKey(entry.getKey(), countingMemoryTwo.memoryCount.get(entry.getKey()));
		}
		return baseMemory;
	}
	
	@Override
	public String toString(){
		return "Counting" + super.toString() + ", counts: " + memoryCount;
	}

}
