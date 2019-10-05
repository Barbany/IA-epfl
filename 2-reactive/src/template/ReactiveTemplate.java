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
	
	private static double EPSILON = 1e-5;

	private Random random;
	private double discount;
	private int numActions;
	private Agent myAgent;
	//private OpenSequenceGraph qValuesEvolution;
	
	// Policy: Random (0), Q-Learning (1)
	// TODO: Note that policy that takes all the packets is also very good
	private int policy = 0;
	
	private double[][][] Q;
	private double[] V;
	private double[] P_nopacket;
	
	private double[] Best_value;
	private City[] Best_neigh;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class,
				0.95);
		Integer policy_v = agent.readProperty("policy", Integer.class, 0);
		policy = policy_v;

		this.random = new Random();
		
		// Discount used as probability of picking up when random policy
		this.discount = discount;
		this.numActions = 1;
		this.myAgent = agent;
		
		// Run Offline Q-Learning
		// Given that there's only one vehicle, get first (and unique) element of list
		
		long start = System.nanoTime();
		reinforce(topology, td, agent.vehicles().get(0));
		long end = System.nanoTime();
		//finding the time difference and converting it into seconds
		float sec = (end - start); 
		System.out.println("Discount factor: " + discount);
		System.out.println("Elapsed time in training " + sec + "ns");
		System.out.println("\n \n");
		
		
	}
	
	public void reinforce(Topology topology, TaskDistribution td, Vehicle v) {
		// Initialize V values to 0 (default in Java)
		
		/*
		 * if (qValuesEvolution != null) { qValuesEvolution.dispose(); }
		 * qValuesEvolution = null;
		 * 
		 * qValuesEvolution = new OpenSequenceGraph("Evolution of Q vals", this);
		 */
		
		P_nopacket = new double[topology.cities().size()];
		V = new double[topology.cities().size()];
		Q = new double[topology.cities().size()][topology.cities().size()][2];
		
		// Compute probability of no having tasks at each city (so T(s,a,s') sums up to one for each s)
		for (City city_a : topology) {
			double p_nopacket = 0;
			for (City city_b : topology) {
				p_nopacket += td.probability(city_a, city_b);
			}
			P_nopacket[city_a.id] = 1 - p_nopacket;
		}
		
		
		double dif;
		double old_Qval;
		
		// Iterate UNTIL GOOD ENOUGH
		do {
			dif = 0;
			for (City city_a : topology) {				
				for (City city_b : topology) {					
					// Actions: Refuse (0) and Accept (1)
					
					if (city_a.hasNeighbor(city_b)) {
						// Refusing a packet can only lead to neighbor states
						old_Qval = Q[city_a.id][city_b.id][0];
						Q[city_a.id][city_b.id][0] = - city_a.distanceTo(city_b) * v.costPerKm() +
								discount * P_nopacket[city_a.id] / city_a.neighbors().size() * V[city_b.id];
						
						// Update V values if we find something better
						V[city_a.id] = Math.max(Q[city_a.id][city_b.id][0], V[city_a.id]);
						
						// Check difference in Q value update
						dif = Math.max(dif, Math.abs(Q[city_a.id][city_b.id][0] - old_Qval));
					}
					old_Qval = Q[city_a.id][city_b.id][1];
					
					Q[city_a.id][city_b.id][1] = td.reward(city_a, city_b) -
							city_a.distanceTo(city_b) * v.costPerKm() +
							discount * td.probability(city_a, city_b) * V[city_b.id];
					
					// Update V values if we find something better
					V[city_a.id] = Math.max(Q[city_a.id][city_b.id][1], V[city_a.id]);
					
					// Check difference in Q value update
					dif = Math.max(dif, Math.abs(Q[city_a.id][city_b.id][1] - old_Qval));
				}
			}
			//qValuesEvolution.addSequence("Discount " + this.discount, dif);
		} while (dif > EPSILON);
		
		// Compute Best vectors
		for (City city_a: topology.cities()) {
			double max_qval = -9999; // If 0 can lead to vehicle choosing the current city if q values are negative
			City max_neigh = city_a;
			
			for (City city_b: city_a.neighbors()) {
				if(max_qval < Q[city_a.id][city_b.id][0]) {
					max_qval = Q[city_a.id][city_b.id][0];
					max_neigh = city_b;
				}
			}
			Best_value[city_a.id] = max_qval;
			Best_neigh[city_a.id] = max_neigh;
		}
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action = null;

		//Random policy
		switch(policy) {
		case 0:
			
			// Random policy
			if (availableTask == null || random.nextDouble() > discount) {
				City currentCity = vehicle.getCurrentCity();
				action = new Move(currentCity.randomNeighbor(random));
			} else {
				action = new Pickup(availableTask);
			}
			break;
		case -1:
			// Dummy agent
			if (availableTask != null && random.nextDouble() > discount) {
				action = new Pickup(availableTask);
			} else {
				City currentCity = vehicle.getCurrentCity();
				action = new Move(currentCity.randomNeighbor(random));
			}

		default:
			// Off-line Q-Learning: Greedy policy
			City city_a = vehicle.getCurrentCity();
			
			if (availableTask != null) {
				if (Best_value[city_a.id] < Q[city_a.id][availableTask.deliveryCity.id][1] &&
						availableTask.weight <= vehicle.capacity()){
					//System.out.println("Pickup action");
					action = new Pickup(availableTask);	
				}
			} else {
				action = new Move(Best_neigh[city_a.id]);
			}
			break;
		}
		
		if (numActions >= 1) {
			System.out.println("The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
		}
		numActions++;
		
		return action;
	}
}
