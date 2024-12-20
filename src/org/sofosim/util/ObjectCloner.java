package org.sofosim.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Mechanism to provide ad-hoc deep copies based on Java serialization.
 * 
 * Source: http://www.javaworld.com/article/2077578/learn-java/java-tip-76--an-alternative-to-the-deep-copy-technique.html
 *
 */
public class ObjectCloner {

	private ObjectCloner() {
	}

	/**
	 * Returns a deep copy of a given object.
	 * @param oldObj
	 * @return
	 * @throws Exception
	 */
	public static Object deepCopy(Object oldObj) throws Exception {
		ObjectOutputStream oos = null;
		ObjectInputStream ois = null;
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			oos = new ObjectOutputStream(bos);
			// serialize and pass the object
			oos.writeObject(oldObj);
			oos.flush();
			ByteArrayInputStream bin = new ByteArrayInputStream(
					bos.toByteArray());
			ois = new ObjectInputStream(bin);
			// return the new object
			return ois.readObject();
		} catch (Exception e) {
			System.out.println("Exception in ObjectCloner: " + e);
			throw (e);
		} finally {
			if(oos != null) {
				oos.close();
			}
			if(ois != null) {
				ois.close();
			}
		}
	}

}
