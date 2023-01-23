package org.sofosim.environment.memoryTypes;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.sofosim.environment.memoryTypes.util.ScaleDifferenceCalculator;

/**
 * Holds memory for memorized items as float value. Discounts this value (towards zero) 
 * upon call to discountForRound() with given discount factor.
 * If value for item hits a given minimum threshold, memory value is moved to aggregate 
 * value without association with original item. It then acts like a tendency in aggregate 
 * measures (e.g. mean), but is not associated with a given entry.  
 * 
 * @author Christopher Frantz
 *
 */
public class DiscountingMemory<K, V extends Number> extends ForgetfulMemory<K, V>{

	private Float discountFactor = 0.99f;
	private HashMap<K, V> trendMemory = null;
	private HashMap<K, V> lastValue = null;
	
	//prints debug output for discounting and memorizing
	private final boolean debug = false;
	
	/**
	 * Value that is treated as neutral for the sake of discounting.
	 */
	private Float zeroBase = 0f;
	
	/** 
	 * Holds the sum of otherwise deleted aggregate values (below deletionThreshold)
	 * (to avoid sudden 'memory loss' for all values if reaching below threshold;
	 * all memory that is not associated with a key anymore but still represents 
	 * some general bias of the owner ('experience')) 
	 */
	private Float aggregateMemoryBelowDeletionThreshold = 0f;
	
	/**
	 * Initializes a new empty DiscountMemory.
	 * @param discountFactor
	 * @param owner
	 */
	public DiscountingMemory(Float discountFactor, String owner){
		this.discountFactor = discountFactor;
		this.owner = owner;
	}
	
	/**
	 * Initializes a new DiscountMemory with existing values.
	 * @param entries
	 * @param discountFactor
	 * @param owner
	 */
	public DiscountingMemory(HashMap<K, V> entries, Float discountFactor, String owner){
		this(discountFactor, owner);
		this.memory = new LinkedHashMap<>(entries);
		notifyMemoryChangeListeners();
	}
	
	/**
	 * Memorize an agent score. Will add score to old score.
	 * @param agent
	 * @param value
	 */
	@Override
	public void memorize(K agent, V value){
		if(!memory.containsKey(agent)){
			if(debug){
				System.out.println(this.owner + ": Adding value " + value + " for key agent " + agent + ", zero base: " + this.zeroBase);
			}
			//memory.put(agent, value);
			memory.put(agent, (V) (this.zeroBase != 0 ? (this.zeroBase < 0 ? addTwoValues(value.floatValue(), Math.abs(this.zeroBase)) : subtractTwoValues(value.floatValue(), Math.abs(this.zeroBase))) : value));
		} else {
			if(debug){
				System.out.println(this.owner + ": Adding value " + value + " to existing value " + memory.get(agent) + " for key agent " + agent + ", zero base: " + this.zeroBase);
			}
			//memory.put(agent, memory.get(agent) + value);
			memory.put(agent, (V) (this.zeroBase != 0 ? (this.zeroBase < 0 ? addTwoValues(addTwoValues(value.floatValue(), memory.get(agent).floatValue()), Math.abs(this.zeroBase)) : subtractTwoValues(addTwoValues(value.floatValue(), memory.get(agent).floatValue()), Math.abs(this.zeroBase))) : value.floatValue() + memory.get(agent).floatValue()));
		}
		notifyMemoryChangeListeners();
	}
	
	/**
	 * Returns the mean of all memory entries.
	 * @return
	 */
	public Double getMeanOfAllEntries(){
		double sum = 0.0;
		try{
			for(Entry<K, V> entry: memory.entrySet()){
				sum = (Double) addTwoValues(sum, entry.getValue());
			}
			if(memory.size() == 0){
				return null;
			}
			//include discounted aggregate values in calculation
			return (sum + aggregateMemoryBelowDeletionThreshold) / (double)memory.size();
		} catch (ConcurrentModificationException e){
			//if you fail, try again :)
			return getMeanOfAllEntries();
		}
		
	}
	
	/**
	 * Memorizes the agent score as trend over all past interactions
	 * with this agent.
	 * @param agent
	 * @param value
	 */
	public void memorizeAsTrend(K agent, V value){
		if(lastValue == null){
			lastValue = new HashMap<>();
		}
		if(trendMemory == null){
			trendMemory = new HashMap<>();
		}
		if(lastValue.containsKey(agent)){
			if(trendMemory.containsKey(agent)){
				trendMemory.put(agent, (V) addTwoValues(trendMemory.get(agent).floatValue(), subtractTwoValues(value.floatValue(), lastValue.get(agent).floatValue())));
			} else {
				trendMemory.put(agent, (V) subtractTwoValues(value.floatValue(), lastValue.get(agent).floatValue()));
			}
		}
		//add as new element/overwrite old element
		lastValue.put(agent, value);
		notifyMemoryChangeListeners();
	}
	
	public V getTrendForAgent(String agent){
		return trendMemory.get(agent);
	}
	
	/**
	 * Indicates if memory about a given agent exists.
	 * @param agent
	 * @return
	 */
	public boolean containsKey(String agent){
		return memory.containsKey(agent);
	}
	
	/**
	 * Returns all memory entries.
	 * @return
	 */
	public HashMap<K, V> getAllEntries(){
		HashMap<K, V> adjustedMemory = new HashMap<>();
		if(this.zeroBase != 0f){
			for(Entry<K, V> entry: memory.entrySet()){
				adjustedMemory.put(entry.getKey(), (V) getValueForKey(entry.getKey()));//this.zeroBase != 0 ? (this.zeroBase < 0 ? entry.getValue() + Math.abs(this.zeroBase) : entry.getValue() - Math.abs(this.zeroBase)) : entry.getValue());
			}
			return adjustedMemory;
		} else {
			return memory;
		}
	}
	
	/**
	 * Returns the number of memory entries held.
	 * @return
	 */
	public Integer getNumberOfEntries(){
		return memory.size();
	}
	
	/**
	 * Indicates if memory is empty (no entry).
	 * @return
	 */
	public Boolean isEmpty(){
		return memory.isEmpty();
	}
	
	@Override
	public boolean containsKey(K key) {
		return memory.containsKey(key);
	}
	
	/**
	 * Returns the entry (i.e. overall valuation) for a given agent.
	 * @param agent
	 * @return
	 */
	@Override
	public Float getValueForKey(K agent){
		return (this.zeroBase != 0f ? this.zeroBase < 0 ? memory.get(agent).floatValue() - Math.abs(this.zeroBase) : memory.get(agent).floatValue() + Math.abs(this.zeroBase) : memory.get(agent).floatValue());
	}
	
	/**
	 * Returns the key for the highest memory value.
	 * @return
	 */
	@Override
	public K getKeyForHighestValue(){
		return getKeyForValue(true);
	}
	
	/**
	 * Returns the key for the lowest memory value.
	 * @return
	 */
	@Override
	public K getKeyForLowestValue(){
		return getKeyForValue(false);
	}
	
	/**
	 * Returns the key for the highest or lowest value in the memory.
	 * @param highestOrLowest true indicates highest value, false lowest value
	 * @return
	 */
	private K getKeyForValue(boolean highestOrLowest){
		ArrayList<Entry<K, V>> list = new ArrayList<Entry<K,V>>(memory.entrySet());
		K extremeKey = null;
		Float extremeValue = null;
		if(highestOrLowest){
			//highest
			extremeValue = -Float.MAX_VALUE;
			for(int i = 0; i < list.size(); i++){
				Entry<K, V> entry = list.get(i);
				if(entry.getValue().floatValue() > extremeValue.floatValue()){
					extremeValue = entry.getValue().floatValue();
					extremeKey = entry.getKey();
				}
			}
		} else {
			//lowest
			extremeValue = Float.MAX_VALUE;
			for(int i = 0; i < list.size(); i++){
				Entry<K, V> entry = list.get(i);
				if(entry.getValue().floatValue() < extremeValue.floatValue()){
					extremeValue = entry.getValue().floatValue();
					extremeKey = entry.getKey();
				}
			}
		}
		return extremeKey;
	}
	
	/**
	 * Returns the highest memory value.
	 * @return
	 */
	public Float getHighestValue(){
		return getExtremeValue(true);
	}
	
	/**
	 * Returns the lowest memory value.
	 * @return
	 */
	public Float getLowestValue(){
		return getExtremeValue(false);
	}
	
	/**
	 * Returns the highest or lowest memory value depending on parameter.
	 * @param highestOrLowest true indicates highest memory value, false lowest memory value
	 * @return
	 */
	private Float getExtremeValue(boolean highestOrLowest){
		ArrayList<Entry<K, V>> list = new ArrayList<Entry<K, V>>(memory.entrySet());
		Float extremeValue = null;
		if(highestOrLowest){
			//highest
			extremeValue = -Float.MAX_VALUE;
			for(int i = 0; i < list.size(); i++){
				Float value = list.get(i).getValue().floatValue();
				if(value > extremeValue){
					extremeValue = value;
				}
			}
		} else {
			//lowest
			extremeValue = Float.MAX_VALUE;
			for(int i = 0; i < list.size(); i++){
				Float value = list.get(i).getValue().floatValue();
				if(value < extremeValue){
					extremeValue = value;
				}
			}
		}
		if(extremeValue == Float.MAX_VALUE || extremeValue == -Float.MAX_VALUE){
			return null;
		}
		//return extremeValue;// + this.zeroBase;
		return (this.zeroBase != 0f ? this.zeroBase < 0 ? extremeValue - Math.abs(this.zeroBase) : extremeValue + Math.abs(this.zeroBase) : extremeValue);
	}
	
	/**
	 * Updates the current memory discount factor to a new value.
	 * @param updatedMemoryDiscountFactor Updated discount factor
	 */
	public void setMemoryDiscountFactor(Float updatedMemoryDiscountFactor){
		this.discountFactor = updatedMemoryDiscountFactor;
	}
	
	/**
	 * Returns the current discount factor.
	 * @return
	 */
	public Float getDiscountFactor(){
		return this.discountFactor;
	}
	
	/**
	 * Discounts all memory entries for current round. Should only be 
	 * called once during a round. Discounts zero-based or towards 
	 * zero base if different one set using {@link #setZeroBasisForDiscounting(Float)
	 *  setZeroBasisForDiscounting}.
	 * @param thresholdForDeletion Add entry to aggregate value when going 
	 * below this threshold to shift from association to general bias.
	 */
	private void discountForRound(float thresholdForDeletion){
		
		//ensures that system always discounts from zero, even if different zero-base is set
		boolean alwaysDiscountFromRealZero = true;
		
		if(this.discountFactor != 1.0f){
			if(alwaysDiscountFromRealZero || this.zeroBase == 0f){
				Iterator<Entry<K, V>> it = memory.entrySet().iterator();
				while(it.hasNext()){
					Entry<K, V> entry = it.next();
					entry.setValue((V)new Float(entry.getValue().floatValue() * this.discountFactor));
					//delete values below certain threshold
					if(thresholdForDeletion != 0.0f){
						float discountedEntry = entry.getValue().floatValue();
						//if discounted value is in range of 0-threshold && 0+threshold --> move to aggregate value
						if(discountedEntry > (0 - thresholdForDeletion) && discountedEntry < thresholdForDeletion){
							//move to aggregate value
							aggregateMemoryBelowDeletionThreshold += discountedEntry; 
							memory.remove(entry.getKey());
							it = memory.entrySet().iterator();
						}
					}
				}
				/*
				 * discount aggregated value as well
				 */
				aggregateMemoryBelowDeletionThreshold *= this.discountFactor;
				notifyMemoryChangeListeners();
			} else {
				if(debug){
					System.out.println("=====\n" + owner + ": Zero base: " + this.zeroBase);
				}
				//different calculation method if zerobase is not 0
				Iterator<Entry<K, V>> it = memory.entrySet().iterator();
				while(it.hasNext()){
					Entry<K, V> entry = it.next();
					if(!entry.getValue().equals(this.zeroBase)){
						//calculate difference from zero base
						float diff = ScaleDifferenceCalculator.calculateDifferenceOnScale(entry.getValue().floatValue(), this.zeroBase);
						if(debug){
							System.out.println(owner + ": Input value: " + entry.getValue());
							System.out.println(owner + ": Difference on scale: " + diff);
						}
						//THIS:
						//calculate how much absolute reduction would be cause for that difference
						//float absChange = Math.abs(diff - diff * this.discountFactor);
						
						//OR THIS:
						//calculate discount relative to absolute zero point, but discount towards modified zero-base to avoid drift towards extreme
						//but correct only to modified zero base (no overshooting with too high absolute change values
						float absChange = Math.min(Math.abs(entry.getValue().floatValue()) - Math.abs(entry.getValue().floatValue() * this.discountFactor), diff);
						if(debug){
							System.out.println(owner + ": Absolute change: " + absChange);
						}
						//add to current value to discount towards zerobase
						if(debug){
							System.out.println(owner + ": Zero value: " + this.zeroBase);
							System.out.println(owner + ": Value before discounting: " + entry.getValue());
						}
						entry.setValue((V) (entry.getValue().floatValue() < this.zeroBase ? addTwoValues(entry.getValue(), absChange) : subtractTwoValues(entry.getValue(), absChange)));
						if(debug){
							System.out.println(owner + ": Value after discounting: " + entry.getValue());
						}
					}
					//if value equals zero base - no discounting
					if(thresholdForDeletion != 0.0){
						float discountedEntry = entry.getValue().floatValue();
						//if discounted value is in range of left and right threshold of zerobase --> move to aggregate value
						if(discountedEntry > (this.zeroBase - thresholdForDeletion) && discountedEntry < (this.zeroBase + thresholdForDeletion)){
							//move to aggregate value
							aggregateMemoryBelowDeletionThreshold += discountedEntry; 
							memory.remove(entry.getKey());
							it = memory.entrySet().iterator();
						}
					}
				}

				/*
				 *  discount aggregated value as well
				 */
				if(!aggregateMemoryBelowDeletionThreshold.equals(this.zeroBase)){
					float diff = ScaleDifferenceCalculator.calculateDifferenceOnScale(aggregateMemoryBelowDeletionThreshold, this.zeroBase);
					float change = diff - diff * this.discountFactor;
					if(aggregateMemoryBelowDeletionThreshold < this.zeroBase){
						//approximate upwards towards neutral base
						aggregateMemoryBelowDeletionThreshold += change;
					} else {
						//approximate downwards towards neutral base
						aggregateMemoryBelowDeletionThreshold -= change;
					}
				}
				notifyMemoryChangeListeners();
			}
		}
	}
	
	/**
	 * Sets the neutral value for discounting entries. 
	 * Use that method if the neutral value is != 0.
	 * Call {@link #forgetAtRoundEnd(float) forgetAtRoundEnd} after that.
	 * @param zeroBasis
	 */
	public void setZeroBasisForDiscounting(Float zeroBasis){
		System.err.println(owner + ": Setting zero base from " + this.zeroBase + " to " + zeroBasis);
		this.zeroBase = zeroBasis;
	}
	
	/**
	 * Discounts all memory entries for current round. Should only be 
	 * called once during a round.
	 * @param thresholdForDeletion Delete entry when going below this threshold
	 */
	@Override
	public void forgetAtRoundEnd(float thresholdForDeletion) {
		discountForRound(thresholdForDeletion);
	}

	@Override
	public String toString() {
		return "DiscountingMemory: " + super.toString() + ", aggregate value: " + aggregateMemoryBelowDeletionThreshold;
	}
	
}
