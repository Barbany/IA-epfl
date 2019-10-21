package template;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class Set<T> {
	int i;
	HashMap<Integer, List<T>> map_fromInt;
	HashMap<T, Integer> map_toInt;
	
	public Set() {
		this.i = 0;
		map_fromInt = new HashMap<Integer, List<T>>();
		map_toInt = new HashMap<T, Integer>();
	}
	
	public void makeSet(T a) {
		List<T> aux = new LinkedList<T>();
		aux.add(a);
		map_fromInt.put(this.i, aux);
		map_toInt.put(a, this.i);
		this.i++;
	}

	public int findSet(T a) {
		return map_toInt.get(a).intValue();
	}
	
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
