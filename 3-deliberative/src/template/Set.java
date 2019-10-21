package template;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Build a Union-Find Disjoint Set of Objects of type T
 * 
 * @author Oriol Barbany & Natalie Bolon
 */
public class Set<T> {
	int i;
	HashMap<Integer, List<T>> map_fromInt;
	HashMap<T, Integer> map_toInt;
	
	/**
	 * Create an empty set
	 */
	public Set() {
		this.i = 0;
		map_fromInt = new HashMap<Integer, List<T>>();
		map_toInt = new HashMap<T, Integer>();
	}
	
	/**
	 * Add singleton set with element a
	 * @param a
	 */
	public void makeSet(T a) {
		List<T> aux = new LinkedList<T>();
		aux.add(a);
		map_fromInt.put(this.i, aux);
		map_toInt.put(a, this.i);
		this.i++;
	}

	/**
	 * Find which set contains object a
	 * @param a
	 * @return Identifier (int) of such set
	 */
	public int findSet(T a) {
		return map_toInt.get(a).intValue();
	}
	
	/**
	 * Join the sets containing a and b
	 * @param a
	 * @param b
	 */
	public void union(T a, T b) {
		int i_a = findSet(a);
		int i_b = findSet(b);
		if (i_a != i_b) {
			map_fromInt.get(i_a).addAll(map_fromInt.get(i_b));
			Iterator<T> it = map_fromInt.get(i_b).iterator();
			while(it.hasNext()) {
				map_toInt.replace(it.next(), i_a);
			}
			map_fromInt.remove(i_b);	
		}
	}
}
