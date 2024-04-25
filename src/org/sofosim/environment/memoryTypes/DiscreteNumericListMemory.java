package org.sofosim.environment.memoryTypes;

import java.util.ArrayList;
import java.util.HashSet;
import org.sofosim.environment.memoryTypes.listeners.MemoryChangeListener;

/**
 * DiscreteListMemory that is initialized with a fixed number of entries.
 * Old entries are removed upon adding new values.
 * 
 * @author Christopher Frantz
 *
 */
public class DiscreteNumericListMemory implements NumericalListMemoryInterface<Float>{

	private final ArrayList<Float> memory = new ArrayList<Float>();
	private Integer numberOfEntries = null;
	private Float sum = 0f;
	private HashSet<MemoryChangeListener> listeners = new HashSet<>();

	public DiscreteNumericListMemory(int numberOfEntries){
		this.numberOfEntries = numberOfEntries;
	}

	@Override
	public void memorize(Float value) {
		if(value == null){
			throw new RuntimeException("Attempted to add null entry to ListMemory.");
		}
		//if memory full, ...
		if(memory.size() == numberOfEntries){
			//remove first entry
			sum -= memory.remove(0);
		}
		//in any case append new one (FIFO principle)
		memory.add(value);
		sum += value;
		notifyListeners();
	}

	@Override
	public Float getMeanOfAllEntries() {
		return sum / (float)memory.size();
	}

	@Override
	public ArrayList<Float> getAllEntries() {
		return this.memory;
	}
	
	private void notifyListeners(){
		for(MemoryChangeListener listener: listeners){
			listener.memoryChanged();
		}
	}

	@Override
	public void registerMemoryChangeListener(MemoryChangeListener listener) {
		if (listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	@Override
	public void deregisterMemoryChangeListener(MemoryChangeListener listener) {
		listeners.remove(listener);
	}

	@Override
	public String toString() {
		return "DiscreteListMemory [maxEntries="
				+ numberOfEntries + ", currentNumberOfEntries=" + memory.size() + ", sum=" + sum +
				", mean=" + getMeanOfAllEntries() + ", memory=" + memory + "]";
	}	
	
}
