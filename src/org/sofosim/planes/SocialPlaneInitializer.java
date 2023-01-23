package org.sofosim.planes;

import java.util.Collection;
import org.sofosim.forceLayout.ForceDirectedLayout;

public interface SocialPlaneInitializer<V> {

	Collection<SocialPlane<V>> getSocialPlanes(ForceDirectedLayout<V,?> layout);
	
}
