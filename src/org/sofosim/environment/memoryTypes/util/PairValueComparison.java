package org.sofosim.environment.memoryTypes.util;

import org.sofosim.environment.memoryTypes.ForgetfulMemory;

import java.util.Objects;

/**
 * Pair structure that assesses equality based on key, and compares based on value (for ordering).
 * @param <K> Key instance
 * @param <V> Associated value
 */
public class PairValueComparison<K, V extends Number> implements Comparable<PairValueComparison<K, Number>>{

    public K key = null;
    public V value = null;

    public PairValueComparison(K key, V value){
        this.key = key;
        this.value = value;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Pair [key=" + key + ", value=" + value + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PairValueComparison)) return false;
        PairValueComparison<K, V> that = (PairValueComparison<K, V>) o;
        return Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(key);
    }

    @Override
    public int compareTo(PairValueComparison<K, Number> o) {
        return new Double(this.getValue().doubleValue()).compareTo(new Double(o.getValue().doubleValue()));
    }
}
