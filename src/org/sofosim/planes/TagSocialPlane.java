package org.sofosim.planes;

import java.awt.Color;
import java.util.Collection;
import java.util.HashMap;
import org.sofosim.forceLayout.ForceDirectedLayout;
import org.sofosim.tags.Tag;

public abstract class TagSocialPlane<V> extends SocialPlane<V> {

	protected HashMap<String, Double> tagValence = null;

	public TagSocialPlane(ForceDirectedLayout layout, String nameOfPlane,
			boolean perceptionallyConstrained, Color planeColor,
			Float weightFactor, Boolean drawLinks, HashMap<String, Double> tagValence) {
		super(layout, nameOfPlane, perceptionallyConstrained, planeColor, weightFactor, drawLinks);
		this.tagValence = tagValence;
	}

	public abstract Collection<Tag> getTags(V individual);

}
