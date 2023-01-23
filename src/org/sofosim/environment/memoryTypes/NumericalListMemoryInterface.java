package org.sofosim.environment.memoryTypes;

public interface NumericalListMemoryInterface<T> extends ListMemoryInterface<T> {

	/**
	 * Returns the mean of all memory entries.
	 * @return
	 */
	public T getMeanOfAllEntries();
	
}
