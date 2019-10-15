package template;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import logist.task.*;

public class Utils {
	public static List<TaskSet> combination(TaskSet values, int size) {

	    if (0 == size) {
	        return Collections.<TaskSet> emptyList();
	    }

	    if (values.isEmpty()) {
	        return Collections.emptyList();
	    }

	    List<TaskSet> combination = new LinkedList<TaskSet>();

	    Task actual = values.iterator().next();

	    TaskSet subSet = values.clone();
	    subSet.remove(actual);

	    List<TaskSet> subSetCombination = combination(subSet, size - 1);

	    for (TaskSet set : subSetCombination) {
	    	TaskSet newSet = set.clone();
	    	Task[] aux = {actual}; 
	    	TaskSet.union(newSet, TaskSet.create(aux));
	        combination.add(newSet);
	    }
	    combination.addAll(combination((TaskSet) subSet, size));

	    return combination;
	}
}
