package template;

import template.CustomPlan;

import java.util.Iterator;

import logist.topology.Topology.City;

public class State implements Comparable<State>{
	public CustomPlan plan;
	public City currentCity;
	public Mapping deliveryMapping, pickupMapping;
	public int freeSpace;
	public boolean visited[];
	public int futureCost;
	
	public State(CustomPlan plan, City currentCity, Mapping pickupMapping, Mapping deliveryMapping,
			int freeSpace, boolean visited[]) {
		this.plan = plan;
		this.deliveryMapping = deliveryMapping;
		this.pickupMapping = pickupMapping;
		this.freeSpace = freeSpace;
		this.visited = visited;
		this.currentCity = currentCity;
		this.futureCost = computeH();
	}
	
	public String toString() {
		return "City: " + currentCity.toString() + ", Plan: " + plan.toString();
	}

	@Override
	public int compareTo(State arg0) {
		// Returns a negative integer, zero, or a positive integer as this object is
		// less than, equal to, or greater than the specified object.
		double metric = this.plan.totalDistance() + this.futureCost - arg0.plan.totalDistance() - arg0.futureCost;
		//double metric = this.plan.totalDistance()  - arg0.plan.totalDistance() ;
		if(metric > 0) {
			return -1;
		} else if (metric < 0) {
			return 1;
		} else {
			return 0;
		}
	}
	
	public int computeH() {
		int cost = 0; 
		Iterator<City> it = pickupMapping.keySet().iterator();
		while (it.hasNext()) {
			cost += currentCity.distanceTo(it.next());
		}
		return cost;
	}
}
