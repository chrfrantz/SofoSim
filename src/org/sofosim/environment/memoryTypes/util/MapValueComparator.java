package org.sofosim.environment.memoryTypes.util;

import java.util.Comparator;
import java.util.Map;

/**
 * Compares maps based on entry value and sorts in descending order.
 * @param <K>
 * @param <V>
 */
public class MapValueComparator <K,
        V extends Float>
        implements Comparator<Map.Entry<K, V>> {

    public int compare(Map.Entry<K, V> a, Map.Entry<K, V> b) {
        int cmp1 = a.getValue().compareTo(b.getValue());
        if (cmp1 != 0) {
            return cmp1 * -1;
        } else {
            return 0;
        }
    }

}
