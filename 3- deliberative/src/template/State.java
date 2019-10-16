package template;

import template.CustomPlan;

import logist.topology.Topology.City;

public class State {
	public CustomPlan plan;
	public City currentCity;
	public Mapping deliveryMapping, pickupMapping;
	public int freeSpace;
	public boolean visited[];
	
	public State(CustomPlan plan, City currentCity, Mapping pickupMapping, Mapping deliveryMapping,
			int freeSpace, boolean visited[]) {
		this.plan = plan;
		this.deliveryMapping = deliveryMapping;
		this.pickupMapping = pickupMapping;
		this.freeSpace = freeSpace;
		this.visited = visited;
		this.currentCity = currentCity; 
	}
}
