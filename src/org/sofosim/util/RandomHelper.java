package org.sofosim.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.nzdis.micro.random.MersenneTwister;

/**
 * Class that simplifies random picking from data structures.
 * @author Christopher Frantz
 *
 */
public class RandomHelper {

    private static MersenneTwister random = null;
    
    /**
     * Indicates whether RNG has been set.
     * @return
     */
    public static boolean isRngSet() {
        return random != null;
    }
    
    /**
     * Sets the RNG for RandomHelper.
     * @param rng
     */
    public static void setRNG(MersenneTwister rng) {
        random = rng;
    }
    
    /**
     * Returns a single element from a collection of generic type instances.
     * Returns null if not enough elements to pick from collection.
     * @param set Set to pick from
     * @return returns null if input set is null or empty.
     */
    public static <T> T getRandomElement(final Collection<T> collection) {
        if (collection == null) {
            return null;
        }
        return getRandomElement(new ArrayList<T>(collection), new ArrayList<T>());
    }
    
    /**
     * Returns a single element from a collection of generic type instances.
     * Returns null if not enough elements to pick from collection.
     * @param collection Collection to pick from
     * @param exception String to be excluded from selection
     * @return returns null if input collection is null or empty.
     */
    public static <T> T getRandomElement(final Collection<T> collection, final T exception) {
        ArrayList<T> candidates = new ArrayList<>(collection);
        ArrayList<T> exceptionList = new ArrayList<>();
        exceptionList.add(exception);
        return getRandomElement(candidates, exceptionList);
    }
    
    /**
     * Returns a random normal within the boundaries specified in input.
     * @param origin Left value boundary
     * @param boundary Right value boundary
     * @return
     */
    public static float getRandomNormal(float origin, float boundary) {
    	if (random == null) {
    		throw new RuntimeException("Random number generator needs to be initialised before calling method.");
    	}
    	
    	// Perform draw
    	float draw = random.nextFloat(true, true);
    	
    	// Normalise to actual input range
    	float range = boundary - origin;
    	
    	if (range <= 0) {
    		throw new RuntimeException("Invalid origin and boundary specifications (<= 0) (Origin: " + origin + ", boundary: " + boundary + ").");
    	}
    	
    	// Return range and draw normalised to input boundaries
    	return origin + draw * range;
    }
    
    /**
     * Returns a random normal within the boundaries specified in input.
     * @param origin Left value boundary
     * @param boundary Right value boundary (included)
     * @return
     */
    public static int getRandomNormal(int origin, int boundary) {
    	if (random == null) {
    		throw new RuntimeException("Random number generator needs to be initialised before calling method.");
    	}

    	// Normalise to actual input range
    	int range = boundary - origin;
    	
    	if (range <= 0) {
    		throw new RuntimeException("Invalid origin and boundary specifications (<= 0).");
    	}
    	
    	// Perform draw
    	int draw = random.nextInt(range + 1);
    	
    	// Return range and draw normalised to input boundaries
    	return origin + draw;
    }
    
    /**
     * Returns a single element from collection of elements based on weight.
     * @param candidates
     * @param weights
     * @return
     */
    public static <T> T getRandomElementWithWeightedInput(final List<T> candidates, final Float... weights) {
    	// Check for empty input
    	if (candidates.isEmpty()) {
    		return null;
    	}
    	// Check for size equivalence of input and weights
    	if (candidates.size() != weights.length) {
    		throw new RuntimeException("Size of elements and corresponding weights must be equal. (" + 
    					candidates.size() + " items, " + weights.length + " weights)");
    	}
    	// Check that weights add up
    	float sum = 0f;
    	for (int i = 0; i < weights.length; i++) {
    		sum += weights[i];
    	}
    	if (sum < 0.99f || sum > 1.01f) {
    		throw new RuntimeException("Probability weights don't add up to 1. (Actual value: " + sum + ")");
    	}
    	// Check that RNG is not null
    	if (random == null) {
            throw new RuntimeException("RandomHelper: Random Number Generator not assigned.");
        }
    	// Determine bracket of random value in collection
    	float rdm = random.nextFloat(true, true);
    	// If the randomly drawn value is larger than sum of probabilities (aggregated previously), take last category.
    	if (rdm > sum) {
    		return candidates.get(weights.length - 1);
    	}
    	// Else just iterate and aggregate weights until random value falls into category
    	sum = weights[0];
    	int i = 0;
    	while (sum < rdm) {
    		i++;
    		sum += weights[i];
    	}
    	return candidates.get(i);
    }
    
    /**
     * Returns a single element from a list of generic type instances.
     * Returns null if not enough elements to pick from list.
     * @param list List to pick from
     * @return returns null if input list is null or empty.
     */
    public static <T> T getRandomElement(final List<T> list) {
        return getRandomElement(list, new ArrayList<T>());
    }
    
    /**
     * Returns a single element from a list of generic type instances.
     * Returns null if not enough elements to pick from list.
     * @param list List to pick from
     * @param exception String to be excluded from selection
     * @return returns null if input list is null or empty.
     */
    public static <T> T getRandomElement(final List<T> list, final T exception) {
        ArrayList<T> exceptionList = new ArrayList<>();
        exceptionList.add(exception);
        return getRandomElement(list, exceptionList);
    }
    
    /**
     * Returns a single element from a list of generic type instances.
     * Returns null if not enough elements to pick from list.
     * @param list List to pick from
     * @param exceptions Exception list
     * @return returns null if input list is null or empty.
     */
    public static <T> T getRandomElement(final List<T> list, final List<T> exceptions) {
        ArrayList<T> elements = getRandomElements(1, list, exceptions, true, false);
        return elements == null ? null : elements.get(0);
    }
    
    /**
     * Retrieves random element/s from a list of generic type instances.
     * Requires the exact number of elements, or returns null.
     * @param numberOfElements Number of elements to be retrieved
     * @param list List of generic type instances to pick from
     * @param uniquePick Indicates if only unique elements should be picked
     * @return Picked items - returns null if input list is null or empty.
     */
    public static <T> ArrayList<T> getRandomElements(final int numberOfElements, final List<T> list, final boolean uniquePick) {
        return getRandomElements(numberOfElements, list, null, uniquePick, false);
    }
    
    /**
     * Retrieves random element/s from a list of generic type instances.
     * Returns null if too few elements to satisfy 
     * @param numberOfElements Number of elements to be retrieved
     * @param list List of generic type instances to pick from
     * @param exceptions List of String elements to exclude from picking
     * @param uniquePick Indicates if only unique elements should be picked
     * @param allowLessPicks Indicates if fewer elements than numberOfElements are permissible
     * @return Picked items - returns null if input list is null or empty.
     */
    public static <T> ArrayList<T> getRandomElements(final int numberOfElements, final List<T> list, final List<T> exceptions, final boolean uniquePick, final boolean allowLessPicks) {
        if(random == null) {
            throw new RuntimeException("RandomHelper: Random Number Generator not assigned.");
        }
        
        if (list == null || list.isEmpty()) {
           return null; 
        }
        
        ArrayList<T> result = new ArrayList<>();
        
        ArrayList<T> filteredList = new ArrayList<>();
        
        if(exceptions == null || exceptions.isEmpty()) {
            //if nothing to filter, just copy
            filteredList.addAll(list);
        } else {
            //check for filtered items
            HashSet<T> excps = new HashSet<T>(exceptions);
            
            for(int i = 0; i < list.size(); i++) {
                if(!excps.contains(list.get(i))) {
                    filteredList.add(list.get(i));
                }
            }
        }
        
        if(filteredList.size() < numberOfElements) {
            //throw new RuntimeException("Too few elements in list (" + filteredList.size() + ") to satisfy requested items (" + numberOfElements + ").");
            return null;
        } else if(filteredList.size() == numberOfElements) {
            return filteredList;
        }
        
        //Pick first item
        Integer item = random.nextInt(filteredList.size());
        result.add(filteredList.get(item));
        
        //model unique pick (keep track of chosen ints)
        HashSet<Integer> uniqueVals = null;
        if(uniquePick) {
            uniqueVals = new HashSet<>();
            uniqueVals.add(item);
        }
        
        //Do the actual picking
        while(result.size() < numberOfElements) {
            item = random.nextInt(filteredList.size());
            
            if(uniquePick && !uniqueVals.contains(item)) {
                //check whether item has been picked before
                result.add(filteredList.get(item));
                uniqueVals.add(item);
            } else if(!uniquePick) {
                //if not necessarily unique, just add
                result.add(filteredList.get(item));
            }
            //item already contained
        }
        
        return result;
    }
    
}
