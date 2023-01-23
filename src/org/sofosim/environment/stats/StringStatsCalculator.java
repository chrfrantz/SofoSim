package org.sofosim.environment.stats;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

public class StringStatsCalculator<V> {

	
	private HashMap<String, ArrayList<V>> keyValueMap = new HashMap<String, ArrayList<V>>();
        
    private HashSet<StatsCalculatorListener> listeners = new HashSet<StatsCalculatorListener>();
	
    private StringStatsCalculatorView view = null;
        
    public StringStatsCalculator(){
        
    }
    
    public StringStatsCalculator(boolean startOutputWindow){
        if(startOutputWindow){
            startStatsCalculatorView();
        }
    }
        
	/**
	 * Consumes values that are used during the calculation of the std deviation.
	 * Use the same key (keyOfConcern) for both methods (i.e. this and getStdDeviation) 
	 * to retrieve the std. dev. of the respective values.
	 * @param keyOfConcern
	 * @param value
	 */
	public void enterValues(String keyOfConcern, V value){
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
	 * Adds multiple values at once for a given key.
	 * @param keyOfConcern
	 * @param values
	 */
	public void enterValues(String keyOfConcern, ArrayList<V> values){
		if(!keyValueMap.containsKey(keyOfConcern)){
			keyValueMap.put(keyOfConcern, values);
		} else {
			keyValueMap.get(keyOfConcern).addAll(values);
		}
        notifyListeners(null);
	}
	
	/**
	 * Adds multiple values at once for a given key.
	 * @param keyOfConcern
	 * @param values
	 */
	public void enterValues(String keyOfConcern, Collection<V> values){
		if(!keyValueMap.containsKey(keyOfConcern)){
			keyValueMap.put(keyOfConcern, new ArrayList<V>(values));
		} else {
			keyValueMap.get(keyOfConcern).addAll(values);
		}
        notifyListeners(null);
	}
	
	/**
	 * Returns all entries for a given key.
	 * @param keyOfConcern
	 * @return
	 */
	public ArrayList<V> getEntries(String keyOfConcern){
		if(keyValueMap.containsKey(keyOfConcern)){
			return keyValueMap.get(keyOfConcern);
		}
		return null;
	}
	
	/**
	 * Returns the number of data for given key.
	 * @param keyOfConcern
	 * @return
	 */
	public Integer getNumberOfEntries(String keyOfConcern){
		if(keyValueMap.containsKey(keyOfConcern)){
			return keyValueMap.get(keyOfConcern).size();
		}
		return 0;
	}
	
	/**
	 * Returns the sum of the entries for a given key.
	 * @param keyOfConcern
	 * @return
	 */
	/*public Float getSumOfEntries(String keyOfConcern){
		ArrayList<V> list = keyValueMap.get(keyOfConcern);
		Float sum = 0.0f;
		for(int i=0; i<list.size(); i++){
			sum += list.get(i).floatValue();
		}
		return sum;
	}*/
	
	/**
	 * Returns the median for a data series identified by a given key.
	 * @param keyOfConcern
	 * @return
	 */
	/*public Number getMedian(String keyOfConcern){
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
	}*/
	
	/**
	 * Returns the mean of a data series identified by a given key.
	 * @param keyOfConcern
	 * @return
	 */
	/*public Float getMean(String keyOfConcern){
		if(keyValueMap.containsKey(keyOfConcern)){
			ArrayList<V> list = keyValueMap.get(keyOfConcern);
			Float sum = getSumOfEntries(keyOfConcern); 
			return sum/(float)list.size();
		}
		return null;
	}*/
	
	/**
	 * Returns the max. value for a data series identified by a given key.
	 * @param keyOfConcern
	 * @return
	 */
	/*public Double getMaxValue(String keyOfConcern){
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
	}*/
	
	/**
	 * Returns the min. value for a data series identified by a given key.
	 * @param keyOfConcern
	 * @return
	 */
	/*public Double getMinValue(String keyOfConcern){
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
	}*/
	
	/**
	 * Returns the standard deviation of the previously entered values.
	 * @param keyOfConcern
	 * @return
	 */
	/*public Float getStdDeviation(String keyOfConcern){
		if(keyValueMap.containsKey(keyOfConcern)){
			Float sum = getSumOfEntries(keyOfConcern);
			ArrayList<V> list = keyValueMap.get(keyOfConcern);
			Float mean = sum/(float)list.size();
			Float sumOfSquares = 0.0f;
			for(int i=0; i<list.size(); i++){
				float diff = list.get(i).floatValue() - mean;
				sumOfSquares += (diff * diff);
			}
			float fairness = new Float(java.lang.Math.sqrt(sumOfSquares/list.size()));
			return fairness;
			//double unfairness = (maxVal-minVal)/maxVal;
		}
		return null;
	}*/
	
	/**
	 * Indicates if a value lies between (exclusive) a lower and upper limit.
	 * @param value
	 * @param lowerLimit
	 * @param upperLimit
	 * @return
	 */
	/*public boolean isBetweenXandY(V value, V lowerLimit, V upperLimit){
		ListComparable comparable = new ListComparable();
		if(comparable.compare(value, lowerLimit) > 0 
				&& comparable.compare(value, upperLimit) < 0){
			return true;
		}
		return false;
	}*/
	
	/**
	 * Indicates if a value lies between or equals (inclusive) a lower and upper limit.
	 * @param value
	 * @param lowerLimit
	 * @param upperLimit
	 * @return
	 */
	/*public boolean isBetweenOrEqualToXAndY(V value, V lowerLimit, V upperLimit){
		ListComparable comparable = new ListComparable();
		if(comparable.compare(value, lowerLimit) >= 0 
				&& comparable.compare(value, upperLimit) <= 0){
			return true;
		}
		return false;
	}*/
	
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
    	view = new StringStatsCalculatorView(this);
    	view.setVisible(true);
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
	
	public void shutdownListeners(){
        for(StatsCalculatorListener listener: listeners){
            listener.shutdown();
        }
        listeners.clear();
        //shut down yourself as well
        if(view != null){
        	view.dispose();
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

	private static final String LINE_SEPARATOR = System.getProperty("line.separator");
	
	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("StringStatsCalculator: ").append(LINE_SEPARATOR);
		ArrayList<Entry<String, ArrayList<V>>> orderedList = new ArrayList<Entry<String, ArrayList<V>>>(keyValueMap.entrySet());
		Collections.sort(orderedList, new Comparator<Entry<String, ArrayList<V>>>(){

			@Override
			public int compare(Entry<String, ArrayList<V>> arg0,
					Entry<String, ArrayList<V>> arg1) {
				return arg0.getKey().compareTo(arg1.getKey());
			}
			
		});
		//for(Entry<String, ArrayList<V>> entry: keyValueMap.entrySet()){
		for(int i=0; i<orderedList.size(); i++){
			Entry<String, ArrayList<V>> entry = orderedList.get(i);
			buffer.append(entry.getKey()).append(": ").append(LINE_SEPARATOR);
			//buffer.append(" ").append("Min: ").append(getMinValue(entry.getKey())).append("  || ");//.append(LINE_SEPARATOR);
			//buffer.append(" ").append("Max: ").append(getMaxValue(entry.getKey())).append(LINE_SEPARATOR);
			buffer.append(" ").append("Count: ").append(getNumberOfEntries(entry.getKey())).append("  || ");//.append(LINE_SEPARATOR);
			//buffer.append(" ").append("Sum: ").append(getSumOfEntries(entry.getKey())).append(LINE_SEPARATOR);
			//buffer.append(" ").append("Mean: ").append(getMean(entry.getKey())).append("  || ");//.append(LINE_SEPARATOR);
			//buffer.append(" ").append("Std. dev.: ").append(getStdDeviation(entry.getKey())).append(LINE_SEPARATOR);
			buffer.append("==================").append(LINE_SEPARATOR);
		}
		return buffer.toString();
		//return "StatsCalculator [keyValueMap=" + keyValueMap + "]";
	}
	
}
