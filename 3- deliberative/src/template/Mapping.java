package template;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import logist.topology.Topology.City;
import logist.task.Task;
import logist.task.TaskSet;


/**
 * Mapping from city to a List of tasks related to it
 * 
 * @author Oriol Barbany & Natalie Bolon
 */
@SuppressWarnings("serial")
public class Mapping extends HashMap<City, List<Task>> implements Cloneable{
	private List<Task> aux;
	
	/** Constructor for Mapping
	 * @param tasks TaskSet of current tasks (to pickup or deliver)
	 * @param pickup Boolean that is true for pickup (so mapping is pickupCity -> List<Task>)
	 * For delivery, set this to false. Resulting mapping is deliveryCity -> List<Task>
	 */
	public Mapping(TaskSet tasks, boolean pickup){
		City currentCity;
		aux = new LinkedList<Task>();
		
		Iterator<Task> it = tasks.iterator();
		
		if(pickup) {
			while(it.hasNext()) {
				Task nextTask = it.next();
				// add elements of the TaskSet to the mapping
				currentCity = nextTask.pickupCity;
				if(this.containsKey(currentCity)) {
					this.get(currentCity).add(nextTask);
				} else {
					aux = new LinkedList<Task>();
					aux.add(nextTask);
					this.put(currentCity, aux);	
				}
			}	
		} else {
			while(it.hasNext()) {
				Task nextTask = it.next();
				// add elements of the TaskSet to the mapping
				currentCity = nextTask.deliveryCity;
				if(this.containsKey(currentCity)) {
					this.get(currentCity).add(nextTask);
				} else {
					aux = new LinkedList<Task>();
					aux.add(nextTask);
					this.put(currentCity, aux);	
				}
			}
		}
	}
	
	
	/**
	 * Create empty mapping
	 */
	public Mapping() {
		aux = new LinkedList<Task>();
	}
	
	/**
	 * If pickup mapping, parameter pickup city (sim. for delivery)
	 * @param currentCity
	 * @param task
	 */
	public void add(City currentCity, Task task) {
		List<Task> cityTaskList;
		// If the city already has other tasks, add it to the TaskSet 
		if (this.containsKey(currentCity)){
			cityTaskList = this.get(currentCity);
			// Include task in the taskSet
			cityTaskList.add(task);
		} else {
			// Include task in the taskSet
			aux = new LinkedList<Task>();
			aux.add(task);
			this.put(currentCity, aux);	
		}
	}
	
	/**
	 * If pickup mapping, parameter is pickup city (sim. for delivery)
	 * This method removes the key if the resulting list is empty
	 * @param currentCity
	 * @param task
	 */
	public void removeTask(City currentCity, Task task) {
		List<Task> l = this.get(currentCity);
		l.remove(task);
		if(l.size() == 0) {
			this.remove(currentCity);
		}
	}
	
	/**
	 * Clone the current mapping so its modifications won't change the original mapping
	 * @return Cloned mapping
	 */
	public Mapping clone() {
		Mapping ret = new Mapping();
		for (City c: this.keySet()) {
			ret.put(c, new LinkedList<Task>(this.get(c)));
		}
		return ret;
	}
}
