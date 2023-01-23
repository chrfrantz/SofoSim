package org.sofosim.environment.memoryTypes;

import java.util.HashMap;
import java.util.List;

/**
 * This Memory Interface represents a generic interface for 
 * different memory types, such as a discounting memory or 
 * discrete memory that maintains a constant number of entries 
 * without discounting the values.
 * 
 * @author cfrantz
 *
 */
public interface AssociativeMemoryInterface<K, V> {

	
	/**
	 * Memorize some key's (e.g. agent, action sequence, ...) score. 
	 * Will add score to old score.
	 * @param key
	 * @param value
	 */
	public void memorize(K key, V value);
	
	/**
	 * Returns the mean of all memory entries.
	 * @return
	 */
	public Double getMeanOfAllEntries();
	
	/**
	 * Returns all memory entries.
	 * @return
	 */
	public HashMap<K,V> getAllEntries();
	
	/**
	 * Returns all entries that start with the given String.
	 * @param startOfKeyName String key names should start with
	 * @return
	 */
	public HashMap<K,V> getAllEntriesThatStartWith(String startOfKeyName);
	
	/**
	 * Returns all entries that have the given String as part of their key name.
	 * @param containedInKeyName String that is contained in returned key names
	 * @return
	 */
	public HashMap<K,V> getAllEntriesThatContain(String containedInKeyName);
	
	/**
	 * Returns the number of memory entries held.
	 * @return
	 */
	public Integer getNumberOfEntries();
	
	/**
	 * Returns the entry (key and value) for the highest value.
	 * @return
	 */
	public java.util.Map.Entry<K, V> getEntryForHighestValue();
	
	/**
	 * Returns the entry (key and value) for the lowest value.
	 * @return
	 */
	public java.util.Map.Entry<K, V> getEntryForLowestValue();
	
	/**
	 * Returns an value for a given agent.
	 * @param key
	 * @return
	 */
	public Float getValueForKey(K key);
	
	/**
	 * Returns the key for the highest memory value.
	 * @return
	 */
	public K getKeyForHighestValue();
	
	/**
	 * Returns the key for the lowest memory value.
	 * @return
	 */
	public K getKeyForLowestValue();
	
	/**
	 * Indicates if memory contains value for key.
	 * @return
	 */
	public boolean containsKey(K key);
	
	/**
	 * Indicates if memory has entries.
	 * @return
	 */
	public boolean hasEntries();
	
	/**
	 * Returns the standard deviation of all memory entries.
	 * @return
	 */
	public Float getStandardDeviationOfAllEntries();
	
	/**
	 * Returns a list of keys whose values are greater than a given threshold.
	 * @param threshold
	 * @return
	 */
	public List<K> getAllKeysForValuesGreaterThan(float threshold);
	
	/**
	 * Returns a list of keys whose values are smaller than a given threshold.
	 * @param threshold
	 * @return
	 */
	public List<K> getAllKeysForValuesSmallerThan(float threshold);
	
	/**
	 * Returns a list of keys whose values are greater than or equal to a given threshold.
	 * @param threshold
	 * @return
	 */
	public List<K> getAllKeysForValuesGreaterThanOrEqualTo(float threshold);
	
	/**
	 * Returns a list of keys whose values are smaller than or equal to a given threshold.
	 * @param threshold
	 * @return
	 */
	public List<K> getAllKeysForValuesSmallerThanOrEqualTo(float threshold);

	/**
	 * Forgets entries at round end. This operation 
	 * depends on the memory implementation (e.g. discounting memory values, deleting).
	 * @param thresholdForDeletion
	 */
	public void forgetAtRoundEnd(float thresholdForDeletion);
	
}
