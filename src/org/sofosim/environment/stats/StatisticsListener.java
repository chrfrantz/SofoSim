/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.sofosim.environment.stats;

import java.util.LinkedHashMap;

/**
 *
 * @author cfrantz
 */
public interface StatisticsListener {

    public void updateStats(StringBuffer buffer, LinkedHashMap<String,Object> structuredValues, boolean useMap);

}
