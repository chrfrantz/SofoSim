package org.sofosim.forceLayout;

import org.apache.commons.collections15.Transformer;

public interface FeedbackTransformer<V,E> extends Transformer<V,E>{

	public void feedback(V key, E val);
	
}
