package org.sofosim.forceLayout;

import java.util.HashMap;
import org.sofosim.tags.Tag;

/**
 * Receives frequent update on current tag distribution in simulation. 
 * Register with ForceDirectedLayout.
 * 
 * @author cfrantz
 *
 */
public interface TagDistributionListener {

	void receiveTagDistribution(HashMap<Tag, Integer> tags);
	
}
