package org.sofosim.environment.memoryTypes;

import java.util.ArrayList;
import org.sofosim.environment.memoryTypes.listeners.MemoryChangeListener;

public interface ListMemoryInterface<T> {

	/**
	 * Memorizes a value.
	 * @param value
	 */
	public void memorize(T value);
	
	/**
	 * Returns all memory entries.
	 * @return
	 */
	public ArrayList<T> getAllEntries();
	
	/**
	 * Registers a listener for changes or updates in memory.
	 * Ensures that each listener is only registered once.
	 * @param listener
	 */
	public void registerMemoryChangeListener(MemoryChangeListener listener);

	/**
	 * Deregisters a change listener.
	 * @param listener
	 */
	public void deregisterMemoryChangeListener(MemoryChangeListener listener);
}
