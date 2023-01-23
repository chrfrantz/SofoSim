/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sofosim.environment.stats;

/**
 *
 * @author cfrantz
 */
public interface StatsCalculatorListener {
	
        /**
         * Sends data to listener for its use
         * @param output 
         */
	void receiveStatsOutput(String output);
	
        /**
         * Shuts down the listener
         */
	void shutdown();
}
