package org.sofosim.environment.memoryTypes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

/**
 * Discrete Memory does not discount memory information 
 * but maintains a memory queue of constant length following 
 * the FIFO principle. Only exception: If feedback information 
 * related to a key (e.g. agent name, action) that is still in 
 * the queue (not yet discarded/forgotten) is entered, both values 
 * are added and stored added as a new memory element, i.e. memorised
 * as the most recent experience. Previous references to the existing
 * entry are deleted. 
 * These operational semantics are unlike DiscreteNonAggregatingMemory, 
 * in which individual values are not aggregated. 
 * 
 * @author Christopher Frantz
 *
 */
public class DiscreteAggregatingMemory<K, V extends Number> extends ForgetfulMemory<K, V>{

	private Integer numberOfEntries = null;
	
	public DiscreteAggregatingMemory(Integer numberOfEntries){
		this.numberOfEntries = numberOfEntries;
	}
	
	public DiscreteAggregatingMemory(Integer numberOfEntries, String owner){
		this(numberOfEntries);
		this.owner = owner;
	}
	
	public DiscreteAggregatingMemory(HashMap<K, V> entries, Integer numberOfEntries, String owner){
		this(numberOfEntries, owner);
		this.memory = new LinkedHashMap<>(entries);
		notifyMemoryChangeListeners();
	}

	/**
	 * Memorizes a value for a given agent and adds it 
	 * to eventual previous values associated with this agent, 
	 * and adds it as a new element to the queue.
	 */
	@Override
	public void memorize(K agent, V value) {
		//delete value first to ensure that new entry is added to end position
		V previous = memory.remove(agent);
		//now enter new entry at end
		memory.put(agent, (V) addTwoValues(value, (previous != null ? previous : 0)));
		//notify change listeners
		notifyMemoryChangeListeners();
	}

	@Override
	public Double getMeanOfAllEntries() {
		Double sum = 0.0;
		for(V value: memory.values()){
			sum = new BigDecimal(addTwoValues(sum, value).toString()).doubleValue();
		}
		return sum / (double)memory.size();
	}
	
	@Override
	public boolean containsKey(K key) {
		return memory.containsKey(key);
	}

	/**
	 * Returns complete memory map.
	 * @return
	 */
	public HashMap<K, V> getAllEntries() {
		return memory;
	}

	/**
	 * Returns the actual number of entries in memory.
	 * @return
	 */
	@Override
	public Integer getNumberOfEntries() {
		return memory.size();
	}

	@Override
	public Float getValueForKey(K agent) {
		return memory.get(agent).floatValue();
	}

	@Override
	public K getKeyForHighestValue() {
		return getExtremeKeyValueEntry(true).key;
	}

	@Override
	public K getKeyForLowestValue() {
		return getExtremeKeyValueEntry(false).key;
	}
	
	private PairValueComparison<K, Number> getExtremeKeyValueEntry(boolean highestOrLowest){
		K key = null;
		Float extremeValue = null;
		if(highestOrLowest){
			//highest
			extremeValue = -Float.MAX_VALUE;
		} else {
			//lowest
			extremeValue = Float.MAX_VALUE;
		}
		for(Entry<K, V> entry: memory.entrySet()){
			if(highestOrLowest){
				//highest
				if(entry.getValue().floatValue() > extremeValue){
					extremeValue = entry.getValue().floatValue();
					key = entry.getKey();
				}
			} else {
				//lowest
				if(entry.getValue().floatValue() < extremeValue){
					extremeValue = entry.getValue().floatValue();
					key = entry.getKey();
				}
			}
		}
		return new PairValueComparison<K, Number>(key, (V)extremeValue);
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
	}

	/**
	 * Forgets excess entries at end of round. Parameter is 
	 * not of effect in this implementation. Uses memory 
	 * length specified in constructor.
	 */
	@Override
	public void forgetAtRoundEnd(float unusedParameterForDiscreteMemory) {
		boolean change = false;
		while(memory.size() > numberOfEntries && memory.size() > 0){
			//remove first entry
			K key = memory.keySet().iterator().next();
			memory.remove(key);
			if (!change) {
				change = true;
			}
		}
		// Notifier listeners about changes
		if (change) {
			notifyMemoryChangeListeners();
		}
	}

	@Override
	public String toString() {
		return "DiscreteAggregatingMemory: " + super.toString();
	}

}
