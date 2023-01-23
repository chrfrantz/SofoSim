package org.sofosim.forceLayout;

import java.awt.Color;
import java.util.HashMap;

/**
 * Receives updated information on cluster-color assignment for second level 
 * clustering. Register in ForceDirectedLayout.
 * 
 * @author cfrantz
 *
 */
public interface SecondaryColorsListener {

	void receiveUpdatedSecondaryColors(HashMap<String, Color> secondaryColors);
	
}
