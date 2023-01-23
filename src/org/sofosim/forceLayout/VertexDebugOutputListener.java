package org.sofosim.forceLayout;

public interface VertexDebugOutputListener<V> {

	/**
	 * Returns the vertex the listener should be informed about.
	 * @return
	 */
	V getVertexOfInterest();
	
	/**
	 * Is called to deliver debug output relevant to the vertex of interest.
	 * @param output
	 */
	void receiveForceDebugOutput(StringBuffer output);
	
}
