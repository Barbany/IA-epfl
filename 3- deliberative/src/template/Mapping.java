package template;
import logist.topology.Topology.City;

import java.util.HashMap;
import java.util.Iterator;

import logist.task.Task;
import logist.task.TaskSet;


@SuppressWarnings("serial")
public class Mapping extends HashMap<City, TaskSet>{
	private HashMap<City,TaskSet> taskDistribution; 
	private boolean mapType; 
	private static final Task[] EMPTYSET = {}; 
	
	public Mapping(TaskSet notDoneTasks, boolean pickup){
		// type of map: pickup or delivery
		this.mapType = pickup;
		
		Iterator<Task> it_tasks = notDoneTasks.iterator();
		while(it_tasks.hasNext()) {
			Task task = it_tasks.next();
			
			// add elements of the taskset to the mapping
			if(pickup) {
				this.add(task.pickupCity, task);
			}else {
				this.add(task.deliveryCity, task);
			}
			
		}
		
	}
	
	public void add(City city, Task task) {
		
		TaskSet cityTaskSet = TaskSet.create(EMPTYSET);
		// If the city already has other tasks, add it to the TaskSet 
		if (this.containsKey(city)){
		cityTaskSet = this.get(city);
		}
					
		// Include task in the taskSet
		cityTaskSet.add(task);
		this.put(city, cityTaskSet);
		
	}
	
public void removeTask(City city, Task task) {
		
		TaskSet cityTaskSet = this.get(city);
		// If the city already has other tasks, add it to the TaskSet 
		cityTaskSet.remove(task);
		this.put(city, cityTaskSet);
		
	}


	
}
