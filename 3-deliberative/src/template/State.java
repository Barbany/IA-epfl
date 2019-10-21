package template;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

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
	public double futureCost;
	//public double actualCost; 
	public String hash;

	public State(CustomPlan plan, City currentCity, Mapping pickupMapping, Mapping deliveryMapping, int freeSpace) {
		this.plan = plan;
		this.deliveryMapping = deliveryMapping;
		this.pickupMapping = pickupMapping;
		this.freeSpace = freeSpace;
		this.currentCity = currentCity;
		this.futureCost = computeH();
		//this.actualCost = computeCost();
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
	 * Compute cost of the state
	 * */
	
	
	
	/**
	 * Compute heuristics
	 * @return
	 */
	public double computeH() {
		PriorityQueue<Edge<City>> edges = new PriorityQueue<Edge<City>>();
		List<City> cities = new LinkedList<City>();
		Iterator<Task> it;
		Set<City> s = new Set<City>();
		Task currentTask;
		
		// Get cities involved in some open or future task
		for(List<Task> tasks : pickupMapping.values()) {
			it = tasks.iterator();
			while(it.hasNext()) {
				currentTask = it.next();
				if(!cities.contains(currentTask.deliveryCity)) {
					cities.add(currentTask.deliveryCity);
					s.makeSet(currentTask.deliveryCity);
				}
				if(!cities.contains(currentTask.pickupCity)) {
					cities.add(currentTask.pickupCity);
					s.makeSet(currentTask.pickupCity);
				}
			}
		}
		
		for(List<Task> tasks : deliveryMapping.values()) {
			it = tasks.iterator();
			while(it.hasNext()) {
				currentTask = it.next();
				if(!cities.contains(currentTask.deliveryCity)) {
					cities.add(currentTask.deliveryCity);
					s.makeSet(currentTask.deliveryCity);
				}
			}
		}
		
		// Create edges between those cities
		for(int i=0; i < cities.size(); i++) {
			for(int j=i+1; j < cities.size(); j++) {
				edges.add(new Edge<City>(cities.get(i), cities.get(j), cities.get(i).distanceTo(cities.get(j))));
			}
		}
		
		// Compute actual MST
		double cost = 0;
		List<City> visitedCities = new LinkedList<City>();
		while(visitedCities.size() < cities.size() && edges.size() > 0) {
			Edge<City> e = edges.poll();
			if(s.findSet(e.a) != s.findSet(e.b)) {
				cost += e.weight;
				s.union(e.a, e.b);
			}
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
		hash = "" + this.deliveryMapping.hashCode() + this.pickupMapping.hashCode() + currentCity.id;
		//hash = "" + this.plan.totalDistance() + this.futureCost;
	}
}
