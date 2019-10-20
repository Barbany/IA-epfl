package template;

import java.util.Iterator;
import java.util.List;

import logist.task.Task;
import logist.topology.Topology.City;

/**
 * Class representing a comparable state
 * 
 * @author Oriol Barbany & Natalie Bolon
 */
public class State implements Comparable<State> {
	public CustomPlan plan;
	public City currentCity;
	public Mapping deliveryMapping, pickupMapping;
	public int freeSpace;
	public int futureCost;
	public String hash;

	public State(CustomPlan plan, City currentCity, Mapping pickupMapping, Mapping deliveryMapping, int freeSpace) {
		this.plan = plan;
		this.deliveryMapping = deliveryMapping;
		this.pickupMapping = pickupMapping;
		this.freeSpace = freeSpace;
		this.currentCity = currentCity;
		this.futureCost = computeH();
		this.computeHash();
	}

	public String toString() {
		return "City: " + currentCity.toString() + ", Plan: " + plan.toString();
	}

	@Override
	public int compareTo(State arg0) {
		// Returns a negative integer, zero, or a positive integer as this object is
		// less than, equal to, or greater than the specified object.
		// We do the reverse as we want reverse sorting
		double metric = this.plan.totalDistance() + this.futureCost - arg0.plan.totalDistance() - arg0.futureCost;
		if (metric > 0) {
			return 1;
		} else if (metric < 0) {
			return -1;
		} else {
			return 0;
		}
	}

	/**
	 * Compute heuristics
	 * @return
	 */
	public int computeH() {
		int cost = 0;
		Iterator<City> it = pickupMapping.keySet().iterator();
		while (it.hasNext()) {
			cost += currentCity.distanceTo(it.next());
		
		}
		return cost;
	}

	/**
	 * Function that delivers all possible tasks of the current state
	 * and modifies the deliveryMapping accordingly
	 */
	public void deliverTasks() {
		if (this.deliveryMapping.containsKey(this.currentCity)) {
			List<Task> toDeliver = this.deliveryMapping.get(this.currentCity);

			// Check if open tasks have current city as destination
			Iterator<Task> it = toDeliver.iterator();
			while (it.hasNext()) {
				Task currentTask = it.next();
				if (this.currentCity != currentTask.deliveryCity) {
					throw new IllegalArgumentException("Non correct mapping");
				}
				this.freeSpace += currentTask.weight;
				this.plan.appendDelivery(currentTask);
			}

			// Remove from the delivery mapping
			this.deliveryMapping.remove(this.currentCity);
		}
	}

	/**
	 * Compute hash that defines current state
	 */
	private void computeHash() {
		//hash = "" + this.deliveryMapping.hashCode() + this.pickupMapping.hashCode() + currentCity.id;
		hash = "" + this.plan.totalDistance() + this.futureCost;
	}
}
