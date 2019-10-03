package template;

import java.util.Random;

import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class ReactiveTemplate implements ReactiveBehavior {

	private Random random;
	private double discount;
	private int numActions;
	private Agent myAgent;
	private double[][][] Q;
	private double[] V;
	private double[] P_nopacket;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class,
				0.95);

		this.random = new Random();
		this.discount = discount;
		this.numActions = 0;
		this.myAgent = agent;
		
		// Run Offline Q-Learning
		// Given that there's only one vehicle, get first (and unique) element of list
		reinforce(topology, td, agent.vehicles().get(0));
	}
	
	public void reinforce(Topology topology, TaskDistribution td, Vehicle v) {
		// Compute probability of no having tasks at each city (so T(s,a,s') sums up to one for each s)
		for (City city_a : topology) {
			double p_nopacket = 0;
			for (City city_b : topology) {
				p_nopacket += td.probability(city_a, city_b);
			}
			P_nopacket[city_a.id] = 1 - p_nopacket;
		}
		
		// Initialize V values
		// TODO: Check that this is a good initialization (o/w try with -max_distance * cost_km)
		for (City city : topology) {
			V[city.id] = 0;
		}
		
		// TODO: Iterate UNTIL GOOD ENOUGH
		for (int i = 0; i < 500; i++) {
			for (City city_a : topology) {				
				for (City city_b : topology) {					
					// Actions: Refuse (0) and Accept (1)
					
					if (city_a.hasNeighbor(city_b)) {
						// Refuse only with neighbors
						Q[city_a.id][city_b.id][0] = - city_a.distanceTo(city_b) * v.costPerKm() +
								discount * P_nopacket[city_a.id] / city_a.neighbors().size() * V[city_b.id];
						
						// Update V values if we find something better
						if (Q[city_a.id][city_b.id][0] > V[city_a.id]) {
							V[city_a.id] = Q[city_a.id][city_b.id][0];
						}
					}
					
					// TODO: Check that the weight incurs a multiplicative cost by distance
					Q[city_a.id][city_b.id][1] = td.reward(city_a, city_b) -
							city_a.distanceTo(city_b) * v.costPerKm() * td.weight(city_a, city_b) +
							discount * td.probability(city_a, city_b) * V[city_b.id];
					
					if (Q[city_a.id][city_b.id][1] > V[city_a.id]) {
						V[city_a.id] = Q[city_a.id][city_b.id][1];
					}
				}
			}
		}
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;

		/* Random policy
		if (availableTask == null || random.nextDouble() > pPickup) {
			City currentCity = vehicle.getCurrentCity();
			action = new Move(currentCity.randomNeighbor(random));
		} else {
			action = new Pickup(availableTask);
		}
		*/
		action = null;
		
		if (numActions >= 1) {
			System.out.println("The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
		}
		numActions++;
		
		return action;
	}
}
