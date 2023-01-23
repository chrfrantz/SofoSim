package org.sofosim.structures;

public class Pair<K,V> implements Comparable<Pair<K,V>>{

	public K left;
	public V right;
	
	public Pair(K key, V value) {
		this.left = key;
		this.right = value;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((left == null) ? 0 : left.hashCode());
		result = prime * result + ((right == null) ? 0 : right.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Pair other = (Pair) obj;
		if (left == null) {
			if (other.left != null)
				return false;
		} else if (!left.equals(other.left))
			return false;
		if (right == null) {
			if (other.right != null)
				return false;
		} else if (!right.equals(other.right))
			return false;
		return true;
	}
	
	/**
	 * Compares numeric values by right value. Else compares on the output of the toString().
	 */
	@Override
	public int compareTo(Pair<K,V> o) {
		if (this.right instanceof Float || this.right instanceof Long || this.right instanceof Short || this.right instanceof Integer) {
			return new Double(((Number)this.right).doubleValue()).compareTo(new Double(((Number)o.right).doubleValue()));
		}
		return this.toString().compareTo(o.toString());
	}

	@Override
	public String toString() {
		return "[" + left + ", " + right + "]";
	}
	
}
