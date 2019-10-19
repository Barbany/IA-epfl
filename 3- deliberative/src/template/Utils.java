package template;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Class with generic methods
 * 
 * @author Oriol Barbany & Natalie Bolon
 */
public class Utils {
	/**
	 * Get all possible combinations of the list values of type T of up to a given size
	 * @param <T>
	 * @param values
	 * @param size
	 * @return
	 */
	public static <T> List<List<T>> combination(List<T> values, int size) {

	    if (0 == size) {
	        return Collections.singletonList(Collections.<T> emptyList());
	    }

	    if (values.isEmpty()) {
	        return Collections.emptyList();
	    }

	    List<List<T>> combination = new LinkedList<List<T>>();

	    T actual = values.iterator().next();

	    List<T> subSet = new LinkedList<T>(values);
	    subSet.remove(actual);

	    List<List<T>> subSetCombination = combination(subSet, size - 1);

	    for (List<T> set : subSetCombination) {
	        List<T> newSet = new LinkedList<T>(set);
	        newSet.add(0, actual);
	        combination.add(newSet);
	    }

	    combination.addAll(combination(subSet, size));

	    return combination;
	}
}


