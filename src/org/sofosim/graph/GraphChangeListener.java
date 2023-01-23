package org.sofosim.graph;

/**
 * Listener interface to be informed about graph updates, such as deletion 
 * or addition of vertices.
 * 
 * @author cfrantz
 *
 * @param <V>
 */
public interface GraphChangeListener<V> {

	void vertexAdded(V vertex);
	
	void vertexRemoved(V vertex);
	
}
