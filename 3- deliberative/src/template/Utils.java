package template;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import logist.task.*;
import logist.topology.Topology.City;

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
	
	public static HashMap<City, TaskSet> createTaskMap(TaskSet notDoneTasks){
		// Create mapping between cities and tasks
		HashMap<City, TaskSet> taskDistribution = new HashMap<City, TaskSet>(); 
		Iterator<Task> it_tasks = notDoneTasks.iterator();
		while(it_tasks.hasNext()) {
			Task task = it_tasks.next();
			TaskSet cityTaskSet = TaskSet.noneOf(notDoneTasks); 
			
			// If the city already has other tasks, add it to the TaskSet 
			if (taskDistribution.containsKey(task.pickupCity)){
				cityTaskSet = taskDistribution.get(task.pickupCity);
				
			}
			
			// Include task in the taskSet
			cityTaskSet.add(task);
			taskDistribution.put(task.pickupCity, cityTaskSet);
		}
		
		return taskDistribution;
	}
	
}


