package template;

import logist.topology.Topology.City;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import logist.task.Task;


@SuppressWarnings("serial")
public class Mapping extends HashMap<City, List<Task>> implements Cloneable{
	private List<Task> aux;
	
	// Constructor for Pickup
	public Mapping(Task[] tasksArray, int size){
		City currentCity;
		aux = new LinkedList<Task>();
		
		for(int i=0; i<size; i++) {
			// add elements of the TaskSet to the mapping
			currentCity = tasksArray[i].pickupCity;
			if(this.containsKey(currentCity)) {
				this.get(currentCity).add(tasksArray[i]);
			} else {
				aux.clear();
				aux.add(tasksArray[i]);
				this.put(currentCity, aux);	
			}
		}
	}
	
	// Constructor for Delivery
	public Mapping() {
		aux = new LinkedList<Task>();
	}
	
	public void add(City currentCity, Task task) {
		
		List<Task> cityTaskList;
		// If the city already has other tasks, add it to the TaskSet 
		if (this.containsKey(currentCity)){
			cityTaskList = this.get(currentCity);
			// Include task in the taskSet
			cityTaskList.add(task);
		} else {
			// Include task in the taskSet
			aux.clear();
			aux.add(task);
			this.put(currentCity, aux);	
		}
	}
	
	public Mapping clone() {
		return (Mapping) super.clone();
	}
}
