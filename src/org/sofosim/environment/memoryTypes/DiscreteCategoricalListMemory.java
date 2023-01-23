package org.sofosim.environment.memoryTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.nzdis.micro.util.DataStructurePrettyPrinter;
import org.sofosim.environment.memoryTypes.listeners.MemoryChangeListener;

public class DiscreteCategoricalListMemory<K> implements ListMemoryInterface<K> {

	private ArrayList<K> memory = new ArrayList<K>();
	private HashMap<K,Integer> memoryMap = new HashMap<>(); 
	private Integer numberOfEntries = null;
	private HashSet<MemoryChangeListener> listeners = new HashSet<>();

	public DiscreteCategoricalListMemory(int numberOfEntries){
		this.numberOfEntries = numberOfEntries;
	}
	
	@Override
	public void memorize(K value) {
		if(value == null){
			throw new RuntimeException("Attempted to add null entry to ListMemory.");
		}
		// If memory full, ...
		if (memory.size() == numberOfEntries) {
			// ... remove first entry
			K oldEntry = memory.remove(0);
			// Remove old entry from index as well
			if (memoryMap.containsKey(oldEntry)) {
				memoryMap.put(oldEntry, memoryMap.get(oldEntry) - 1);
				if (memoryMap.get(oldEntry).equals(0)) {
					memoryMap.remove(oldEntry);
				}
			}
		}
		// Add value
		memory.add(value);
		// Create index
		if (memoryMap.containsKey(value)) {
			memoryMap.put(value, memoryMap.get(value) + 1);
		} else {
			memoryMap.put(value, 1);
		}
	}
	
	/**
	 * Returns a distribution of values across the memory.
	 * @return
	 */
	public HashMap<K, Float> getValueDistribution() {
		HashMap<K, Float> map = new HashMap<>();
		for (Entry<K, Integer> entry: memoryMap.entrySet()) {
			map.put(entry.getKey(), entry.getValue()/(float)memory.size());
		}
		return map;
	}
	
	/**
	 * Returns fraction of memory entries related to given value.
	 * @param value
	 * @return
	 */
	public Float getFractionForKey(K value) {
		return !memoryMap.containsKey(value) ? 0f : 
			memoryMap.get(value)/(float)memory.size();
	}
	
	/**
	 * Returns relative fractions of memory entries for given values 
	 * (i.e., fractions relative to other given entries, not all entries).
	 * @param keySelection Keys which the distribution is calculated from (i.e. subset of all keys) and for.
	 * @return Map with key containing keySelection value, and value containing corresponding fraction.
	 */
	public Map<K, Float> getFractionsForKeys(K... keySelection) {
		HashMap<K, Float> dist = new HashMap<>();
		int sum = 0;
		for (int i = 0; i < keySelection.length; i++) {
			if (memoryMap.containsKey(keySelection[i])) {
				sum += memoryMap.get(keySelection[i]);
				dist.put(keySelection[i], (float)memoryMap.get(keySelection[i]));
			}
		}
		for (Entry<K, Float> entry: dist.entrySet()) {
			dist.put(entry.getKey(), sum == 0 ? 0 : entry.getValue()/(float)sum);
		}
		return dist;
	}
	
	/**
	 * Returns relative fractions of memory entries for given values 
	 * (i.e., fractions relative to other given entries, not all entries).
	 * @param targetKey Key for which relative fraction is to be returned.
	 * @param keySelection Keys which the distribution is calculated from.
	 * @return Fraction of targetKey value in keySelection values
	 */
	public Float getFractionForKey(K targetKey, K... keySelection) {
		int sum = 0;
		// If the search key is not contained, simply abort
		if (!memoryMap.containsKey(targetKey)) {
			return 0f;
		}
		for (int i = 0; i < keySelection.length; i++) {
			if (memoryMap.containsKey(keySelection[i])) {
				sum += memoryMap.get(keySelection[i]);
				// omit check whether target key is in map for performance reasons
			}
		}
		return sum == 0 ? 0 : memoryMap.get(targetKey) / (float)sum;
	}
	
	@Override
	public ArrayList<K> getAllEntries() {
		return memory;
	}

	@Override
	public void registerMemoryChangeListener(MemoryChangeListener listener) {
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	@Override
	public void deregisterMemoryChangeListener(MemoryChangeListener listener) {
		listeners.remove(listener);
	}
	
	@Override
	public String toString() {
		return new StringBuffer("DiscreteCategoricalListMemory: ").append(memory.size())
				.append("; Distribution: ").append(getValueDistribution()).toString();
	}

}
