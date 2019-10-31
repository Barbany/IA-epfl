package template;

import logist.task.Task;
import logist.topology.Topology.City;

public abstract class Action {
	public Task task;
	public City city;
	public boolean pickup;
	public int capacity;
	
	public static final class Pickup extends Action{
		public Pickup(Task task) {
			this.task = task;
			this.city = task.pickupCity;
			this.pickup = true;
			this.capacity = -task.weight;
		}
	}
	
	public static final class Delivery extends Action{
		public Delivery(Task task) {
			this.task = task;
			this.city = task.deliveryCity;
			this.pickup = false;
			this.capacity = task.weight; 
		}
	}
}
