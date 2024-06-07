package org.sofosim.environment.stats;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class StatsCalculator<V extends Number> {

	
	private HashMap<String, ArrayList<V>> keyValueMap = new HashMap<String, ArrayList<V>>();
        
    private HashSet<StatsCalculatorListener> listeners = new HashSet<StatsCalculatorListener>();

    private StatsCalculatorView statsView = null;
    
    
    //// Aggregation methods
    
    /**
     * Aggregation method SUM for {@link #getAggregatedMap(String)}.
     * Sums values for a given key.
     */
    public static final String AGGREGATION_SUM = "SUM";
    /**
     * Aggregation method MEAN for {@link #getAggregatedMap(String)}.
     * Calculates mean of values for a given key.
     */
    public static final String AGGREGATION_MEAN = "MEAN";
    /**
     * Aggregation method MEDIAN for {@link #getAggregatedMap(String)}.
     * Calculates median for a given key.
     */
    public static final String AGGREGATION_MEDIAN = "MEDIAN";
    /**
     * Aggregation method COUNT for {@link #getAggregatedMap(String)}.
     * Counts values for a given key.
     */
    public static final String AGGREGATION_COUNT = "COUNT";
    /**
     * Aggregation method STDDEV for {@link #getAggregatedMap(String)}.
     * Calculates standard deviation for a given key.
     */
    public static final String AGGREGATION_STDDEV = "STDDEV";
    
    public StatsCalculator(){
        
    }
    
    public StatsCalculator(boolean startOutputWindow){
        if(startOutputWindow){
            startStatsCalculatorView();
        }
    }
        
	/**
	 * Consumes values that are used during the calculation of mean and std deviation.
	 * Use the same key (keyOfConcern) for all statistical methods such as 
	 * {@link #getMean(String)}, {@link #getMedian(String)}, {@link #getMinValue(String)}, 
	 * {@link #getMaxValue(String)}, {@link #getNumberOfEntries(String)}, {@link #getSumOfValues(String)}, 
	 * {@link #getStdDeviation(String)} to get the respective measure for values associated 
	 * with that key. 
	 * Note: Values that are null, NaNs or infinite are ignored.
	 * @param keyOfConcern Key identifying entered values
	 * @param value Value to be considered for statistical evaluation
	 */
	public void enterValue(String keyOfConcern, V value){
		if(value == null || Double.isNaN(value.doubleValue()) || Double.isInfinite(value.doubleValue())){
			return;
		}
		if(!keyValueMap.containsKey(keyOfConcern)){
			ArrayList<V> list = new ArrayList<V>();
			list.add(value);
			keyValueMap.put(keyOfConcern, list);
		} else {
			keyValueMap.get(keyOfConcern).add(value);
		}
		notifyListeners(null);
	}
	
	/**
	 * Variant of {@link #enterValue(String, Number)} accepting Boolean values.
	 * @param keyOfConcern Key identifying entered values
	 * @param value Value to be considered for statistical evaluation. True is 
	 * translated to 1, False to 0 when stored under key.
	 */
	public void enterValue(String keyOfConcern, boolean value){
		enterValue(keyOfConcern, value ? 1 : 0);
	}
		
	/**
	 * Variant of {@link #enterValue(String, Number)} accepting int values.
	 * @param keyOfConcern Key identifying entered values
	 * @param value Value to be considered for statistical evaluation
	 */
	public void enterValue(String keyOfConcern, int value){
		Integer intObj = new Integer(value);  
	    Number numObj = (Number)intObj;
	    enterValue(keyOfConcern, (V)numObj);
	}
	
	/**
	 * Adds multiple values at once for a given key.
	 * Note: For performance reasons no check on validity of individual 
	 * entries is undertaken. It only ensures that the added collection 
	 * of values is not null.
	 * @param keyOfConcern
	 * @param values
	 */
	public void enterValues(String keyOfConcern, ArrayList<V> values){
		if(values == null){
			return;
		}
		if(!keyValueMap.containsKey(keyOfConcern)){
			keyValueMap.put(keyOfConcern, values);
		} else {
			keyValueMap.get(keyOfConcern).addAll(values);
		}
		notifyListeners(null);
	}
	
	/**
	 * Adds multiple values at once for a given key. Converts a passed 
	 * array to ArrayList prior to adding.
	 * Note: For performance reasons no check on validity of individual 
	 * entries is undertaken. It only ensures that the added collection 
	 * of values is not null.
	 * @param keyOfConcern
	 * @param values
	 */
	public void enterValues(String keyOfConcern, int[] values){
		if(values == null){
			return;
		}
		ArrayList<Number> list = new ArrayList<Number>();
		for(int i = 0; i < values.length; i++){
			list.add(new BigDecimal(values[i]));
		}
		if(!keyValueMap.containsKey(keyOfConcern)){
			keyValueMap.put(keyOfConcern, (ArrayList<V>) list);
		} else {
			keyValueMap.get(keyOfConcern).addAll((Collection<? extends V>) list);
		}
		notifyListeners(null);
	}
	
	/**
	 * Adds multiple values at once for a given key.
	 * Note: For performance reasons no check on validity of individual 
	 * entries is undertaken. It only ensures that the added collection 
	 * of values is not null.
	 * @param keyOfConcern
	 * @param values
	 */
	public void enterValues(String keyOfConcern, Collection<V> values){
		if(values == null){
			return;
		}
		if(!keyValueMap.containsKey(keyOfConcern)){
			keyValueMap.put(keyOfConcern, new ArrayList<V>(values));
		} else {
			keyValueMap.get(keyOfConcern).addAll(values);
		}
        notifyListeners(null);
	}
	
	/**
	 * Adds multiple key-value pairs at the same time by decomposing input 
	 * value HashMap. Existing entries can be overwritten (i.e. key and 
	 * all values are replaced) or added (i.e. key is maintained and 
	 * values are added in addition to existing ones).
	 * @param keyValuePairs
	 * @param overwrite Indicates if existing values are overwritten, or values added to list of existing values.
	 */
	public void enterKeyValuePairs(HashMap<String, V> keyValuePairs, boolean overwrite) {
		for (Entry<String, V> entry: keyValuePairs.entrySet()) {
			if (overwrite) {
				keyValueMap.remove(entry.getKey());
			}
			enterValue(entry.getKey(), entry.getValue());
		}
	}
	
	/**
     * Adds multiple key-value pairs at the same time by decomposing input 
     * value LinkedHashMap. Existing entries can be overwritten (i.e. key and 
     * all values are replaced) or added (i.e. key is maintained and 
     * values are added in addition to existing ones).
     * @param keyValuePairs
     * @param overwrite Indicates if existing values are overwritten, or values added to list of existing values.
     */
	public void enterKeyValuePairs(LinkedHashMap<String, V> keyValuePairs, boolean overwrite) {
	    enterKeyValuePairs(new HashMap<String, V> (keyValuePairs), overwrite);
    }
	
	/**
	 * Adds multiple keys with same value.
	 * @param keys Collection of keys.
	 * @param value Value entered for each key.
	 */
	public void enterKeysWithSameValue(Collection<String> keys, V value) {
		for (String key: keys) {
			enterValue(key, value);
		}
	}
	
	/**
	 * Indicates whether key is contained in collection of keys.
	 * @param keyOfConcern
	 * @return
	 */
	public boolean containsKey(String keyOfConcern) {
		return keyValueMap.containsKey(keyOfConcern);
	}
	
	/**
	 * Returns all keys used in this calculator instance.
	 * @return
	 */
	public Set<String> getKeys(){
		return keyValueMap.keySet();
	}
	
	/**
	 * Returns a set of keys with a given prefix.
	 * @param prefix Prefix the returned keys have in common
	 * @return Set of keys
	 */
	public Set<String> getKeysWithPrefix(String prefix) {
		Set<String> keys = new HashSet<String>();
		for (String key: keyValueMap.keySet()) {
			if (key.startsWith(prefix)) {
				keys.add(key);
			}
		}
		return keys;
	}
	
	/**
	 * Returns all values for a given key, or null if no 
	 * values exist
	 * @param keyOfConcern
	 * @return
	 */
	public ArrayList<V> getValues(String keyOfConcern){
		if(keyValueMap.containsKey(keyOfConcern)){
			return keyValueMap.get(keyOfConcern);
		}
		return null;
	}
	
	/**
	 * Returns map of all keys with aggregated values as per 
	 * specified aggregation method.
	 * Methods: {@link #AGGREGATION_SUM}, {@link #AGGREGATION_MEDIAN}, 
	 * {@link #AGGREGATION_MEAN}, {@link #AGGREGATION_COUNT}, {@link #AGGREGATION_STDDEV}
	 * @param aggregationMethod Aggregation method
	 * @return
	 */
	public Map<String, Number> getAggregatedMap(String aggregationMethod) {
		return getAggregatedMap(aggregationMethod, -1);
	}
	
	/**
	 * Returns map of all keys with aggregated values as per 
	 * specified aggregation method.
	 * Methods: {@link #AGGREGATION_SUM}, {@link #AGGREGATION_MEDIAN}, 
	 * {@link #AGGREGATION_MEAN}, {@link #AGGREGATION_COUNT}, {@link #AGGREGATION_STDDEV}
	 * @param aggregationMethod Aggregation method
	 * @param topRankedValues Number of top-ranked entries (by values) to be included. 
	 *  -1 indicates that all entries are included.
	 * @return
	 */
	public Map<String, Number> getAggregatedMap(String aggregationMethod, int topRankedValues) {
		HashMap<String, Number> aggregatedEntries = new HashMap<String, Number>();
		switch (aggregationMethod) {
			case AGGREGATION_MEAN:
				for (String key: keyValueMap.keySet()) {
					aggregatedEntries.put(key, getMean(key));
				}
				break;
			case AGGREGATION_MEDIAN:
				for (String key: keyValueMap.keySet()) {
					aggregatedEntries.put(key, getMedian(key));
				}
				break;
			case AGGREGATION_SUM:
				for (String key: keyValueMap.keySet()) {
					aggregatedEntries.put(key, getSumOfValues(key));
				}
				break;
			case AGGREGATION_COUNT:
				for (String key: keyValueMap.keySet()) {
					aggregatedEntries.put(key, getNumberOfEntries(key));
				}
				break;
			case AGGREGATION_STDDEV:
                for (String key: keyValueMap.keySet()) {
                    aggregatedEntries.put(key, getStdDeviation(key));
                }
                break;
			default: throw new RuntimeException("StatsDataCalculator: Unknown aggregation method " + aggregationMethod);
		}
		
		LinkedHashMap<String, Number> result = (LinkedHashMap<String, Number>) sortByValue((Map)aggregatedEntries, false);
		if (topRankedValues != -1) {
			LinkedHashMap<String, Number> finalResult = new LinkedHashMap<String, Number>();
			int ct = 0;
			for (Entry<String,Number> entry: result.entrySet()) {
				finalResult.put(entry.getKey(), entry.getValue());
				ct++;
				if (ct == topRankedValues) {
					break;
				}
			}
			result = finalResult;
		}
		return result;
	}
	
	
	/**
	 * Returns the number of entries for given key, or
	 * null if that key does not exist.
	 * @param keyOfConcern
	 * @return
	 */
	public Integer getNumberOfEntries(String keyOfConcern){
		if(keyValueMap.containsKey(keyOfConcern)){
			return keyValueMap.get(keyOfConcern).size();
		}
		return null;
	}

	/**
	 * Returns the number of entries across all keys.
	 * @return
	 */
	public Integer getNumberOfEntries(){
		int count = 0;
		for (Entry<String, ArrayList<V>> entry: keyValueMap.entrySet()){
			count += entry.getValue().size();
		}
		return count;
	}
	
	/**
	 * Returns the sum of the values for a given key. 
	 * Returns null if key does not exist.
	 * @param keyOfConcern
	 * @return
	 */
	public Float getSumOfValues(String keyOfConcern){
		return getSumOfValues(keyOfConcern, false);
	}
	
	/**
	 * Returns the sum of the values for a given key. 
	 * If returnZeroIfNoEntry is false, returns null if key does not exist.
	 * If set to true, it will return 0 (for ease of use for numerical processing).
	 * @param keyOfConcern
	 * @param returnZeroIfNoEntry
	 * @return
	 */
	public Float getSumOfValues(String keyOfConcern, boolean returnZeroIfNoEntry){
		ArrayList<V> list = keyValueMap.get(keyOfConcern);
		if (list == null) {
			return (returnZeroIfNoEntry ? 0f : null);
		}
		Float sum = 0.0f;
		for(int i=0; i<list.size(); i++){
			sum += list.get(i).floatValue();
		}
		return sum;
	}

	/**
	 * Returns the sum of the values across all keys.
	 * @return
	 */
	public Float getSumOfValues(){
		float sum = 0f;
		for (Entry<String, ArrayList<V>> entry : keyValueMap.entrySet()) {
			sum += getSumOfValues(entry.getKey(), true);
		}
		return sum;
	}
	
	/**
	 * Returns the median for a data series identified by a given key.
	 * @param keyOfConcern
	 * @return
	 */
	public Number getMedian(String keyOfConcern){
		if(keyValueMap.containsKey(keyOfConcern)){
			ArrayList<V> list = keyValueMap.get(keyOfConcern); 
			Collections.sort(list, new ListComparable());
			if((list.size() % 2) == 0){
				return list.get(list.size() / 2);
			} else {
				//if balance, take mean of both
				return (list.get(list.size() / 2).doubleValue() + list.get((list.size() / 2 + 1)).doubleValue()) / 2;
			}
		}
		return null;
	}
	
	/**
	 * Returns the mean of a data series identified by a given key.
	 * @param keyOfConcern
	 * @return
	 */
	public Float getMean(String keyOfConcern){
		if(keyValueMap.containsKey(keyOfConcern)){
			ArrayList<V> list = keyValueMap.get(keyOfConcern);
			Float sum = getSumOfValues(keyOfConcern); 
			return sum/(float)list.size();
		}
		return null;
	}
	
	/**
	 * Returns the max. value for a data series identified by a given key.
	 * @param keyOfConcern
	 * @return
	 */
	public Double getMaxValue(String keyOfConcern){
		if(keyValueMap.containsKey(keyOfConcern)){
			ArrayList<V> list = keyValueMap.get(keyOfConcern);
			Double maxValue = null;
			for(int i=0; i<list.size(); i++){
				Double tempValue = list.get(i).doubleValue();
				if(maxValue == null || tempValue.doubleValue() > maxValue){
					maxValue = tempValue;
				}
			}
			return maxValue;
		}
		return null;
	}
	
	/**
	 * Returns the min. value for a data series identified by a given key.
	 * @param keyOfConcern
	 * @return
	 */
	public Double getMinValue(String keyOfConcern){
		if(keyValueMap.containsKey(keyOfConcern)){
			ArrayList<V> list = keyValueMap.get(keyOfConcern);
			Double minValue = null;
			for(int i=0; i<list.size(); i++){
				Double tempValue = list.get(i).doubleValue();
				if(minValue == null || tempValue.doubleValue() < minValue){
					minValue = tempValue;
				}
			}
			return minValue;
		}
		return null;
	}
	
	/**
	 * Returns the standard deviation of the previously entered values. 
	 * (corrected sample standard deviation)
	 * @param keyOfConcern
	 * @return
	 */
	public Float getStdDeviation(String keyOfConcern){
		if(keyValueMap.containsKey(keyOfConcern)){
			Float sum = getSumOfValues(keyOfConcern);
			ArrayList<V> list = keyValueMap.get(keyOfConcern);
			Float mean = sum/(float)list.size();
			Float sumOfSquares = 0.0f;
			for(int i=0; i<list.size(); i++){
				float diff = list.get(i).floatValue() - mean;
				sumOfSquares += (diff * diff);
			}
			//corrected sample standard deviation
			float fairness = new Float(java.lang.Math.sqrt((1 / (float)(list.size() - 1)) * sumOfSquares));
			return fairness;
		}
		return null;
	}
	
	/**
	 * Returns the frequency distribution for entries for a given key. 
	 * Returns null if key is not contained.
	 * @param keyOfConcern
	 * @return Frequency map of values
	 */
	public Map<V, Integer> getFrequencyDistribution(String keyOfConcern) {
		if (!keyValueMap.containsKey(keyOfConcern)) {
			return null;
		}
		HashMap<V, Integer> frequencyMap = new HashMap<>();
		for (V value : keyValueMap.get(keyOfConcern)) {
			if (frequencyMap.containsKey(value)) {
				frequencyMap.put(value, frequencyMap.get(value) + 1);
			} else {
				frequencyMap.put(value, 1);
			}
		}
		return frequencyMap;
	}
	
	/**
	 * Indicates if a value lies between (exclusive) a lower and upper limit.
	 * @param value
	 * @param lowerLimit
	 * @param upperLimit
	 * @return
	 */
	public boolean isBetweenXandY(V value, V lowerLimit, V upperLimit){
		ListComparable comparable = new ListComparable();
		if(comparable.compare(value, lowerLimit) > 0 
				&& comparable.compare(value, upperLimit) < 0){
			return true;
		}
		return false;
	}
	
	/**
	 * Indicates if a value lies between or equals (inclusive) a lower and upper limit.
	 * @param value
	 * @param lowerLimit
	 * @param upperLimit
	 * @return
	 */
	public boolean isBetweenOrEqualToXAndY(V value, V lowerLimit, V upperLimit){
		ListComparable comparable = new ListComparable();
		if(comparable.compare(value, lowerLimit) >= 0 
				&& comparable.compare(value, upperLimit) <= 0){
			return true;
		}
		return false;
	}
	
	/**
	 * Indicates if StatsCalculator instance has a given key.
	 * @param keyOfConcern Key to be checked
	 * @return
	 */
	public boolean hasKey(String keyOfConcern){
		return keyValueMap.containsKey(keyOfConcern);
	}
	
	/**
	 * Indicates if the StatsCalculator instance has 
	 * values for a given key.
	 * @param keyOfConcern
	 * @return
	 */
	public boolean hasEntriesForKey(String keyOfConcern) {
		return keyValueMap.containsKey(keyOfConcern) && 
				!keyValueMap.get(keyOfConcern).isEmpty();
	}
	
	/**
	 * Indicates if the StatsCalculator instance has 
	 * multiple values for a given key.
	 * @param keyOfConcern
	 * @return
	 */
	public boolean hasMultipleEntriesForKey(String keyOfConcern) {
		return keyValueMap.containsKey(keyOfConcern) && 
				keyValueMap.get(keyOfConcern).size() > 1;
	}
	
	/**
	 * Sorts a given map by value.
	 * Adapted from: http://stackoverflow.com/questions/109383/sort-a-mapkey-value-by-values-java
	 * @param map Map to be sorted
	 * @param ascending Indicates whether should be sorted ascending (true) or descending (false).
	 * @return LinkedHashMap sorted by value
	 */
	public static <K, M extends Comparable<? super M>> Map<K, M> 
    	sortByValue( Map<K, M> map, final boolean ascending ) {
	    
		List<Map.Entry<K, M>> list =
	        new LinkedList<>( map.entrySet() );
			    Collections.sort( list, new Comparator<Map.Entry<K, M>>() {
			        @Override
			        public int compare( Map.Entry<K, M> o1, Map.Entry<K, M> o2 )
			        {
			            return (o1.getValue()).compareTo(o2.getValue()) * 
			            		(ascending ? 1 : -1);
			        }
			    } );
	
	    Map<K, M> result = new LinkedHashMap<>();
	    for (Map.Entry<K, M> entry : list) {
	        result.put( entry.getKey(), entry.getValue() );
	    }
	    return result;
	}
	
	/**
	 * Clears all entries for a particular key to allow collection for next round.
	 * @param keyOfConcern
	 */
	public void clearEntries(String keyOfConcern){
		//clear list for next round
		if(keyValueMap.containsKey(keyOfConcern)){
			keyValueMap.get(keyOfConcern).clear();
		}
        notifyListeners(null);
	}
	
	public void clearAllEntries(){
		keyValueMap.clear();
		notifyListeners(null);
	}
	
    final public void startStatsCalculatorView(){
        statsView = new StatsCalculatorView(this);
        statsView.setVisible(true);
    }
	
	public void notifyListeners(String keyOfConcern){
            if(keyOfConcern == null || keyOfConcern.isEmpty()){
                for(StatsCalculatorListener list: listeners){
                    list.receiveStatsOutput(toString());
                }
            } else {
                System.err.println("StatsCalculator: Support for output of individual keys has not yet been added.");
            }
	}
	
	private class ListComparable implements Comparator<Number>{

		@Override
		public int compare(Number a, Number b) {
			return new BigDecimal(a.toString()).compareTo(new BigDecimal(b.toString()));
		}
		
	}
        
    public void registerListener(StatsCalculatorListener listener){
        listeners.add(listener);
    }
    
    public boolean hasRegisteredListeners(){
    	return !listeners.isEmpty();
    }
    
    public void shutdownListeners(){
        for(StatsCalculatorListener listener: listeners){
            listener.shutdown();
        }
        listeners.clear();
        //shut down yourself as well
        if(statsView != null){
        	statsView.dispose();
        }
    }

	private static final String LINE_SEPARATOR = System.getProperty("line.separator");
	
	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("StatsCalculator (" + keyValueMap.size() + " entries):").append(LINE_SEPARATOR);
		ArrayList<Entry<String, ArrayList<V>>> orderedList = new ArrayList<Entry<String, ArrayList<V>>>(keyValueMap.entrySet());
		Collections.sort(orderedList, new Comparator<Entry<String, ArrayList<V>>>(){

			@Override
			public int compare(Entry<String, ArrayList<V>> arg0,
					Entry<String, ArrayList<V>> arg1) {
				return arg0.getKey().compareTo(arg1.getKey());
			}
			
		});
		for(int i=0; i<orderedList.size(); i++){
			Entry<String, ArrayList<V>> entry = orderedList.get(i);
			buffer.append(entry.getKey()).append(": ").append(LINE_SEPARATOR);
			buffer.append(" ").append("Min: ").append(getMinValue(entry.getKey())).append("  || ");//.append(LINE_SEPARATOR);
			buffer.append(" ").append("Max: ").append(getMaxValue(entry.getKey())).append(LINE_SEPARATOR);
			buffer.append(" ").append("Count: ").append(getNumberOfEntries(entry.getKey())).append("  || ");//.append(LINE_SEPARATOR);
			buffer.append(" ").append("Sum: ").append(getSumOfValues(entry.getKey())).append(LINE_SEPARATOR);
			buffer.append(" ").append("Mean: ").append(getMean(entry.getKey())).append("  || ");//.append(LINE_SEPARATOR);
			buffer.append(" ").append("Std. dev.: ").append(getStdDeviation(entry.getKey())).append(LINE_SEPARATOR);
			buffer.append("==================").append(LINE_SEPARATOR);
		}
		return buffer.toString();
	}
	
}
