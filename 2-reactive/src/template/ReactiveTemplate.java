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
	private int policy;
	
	private double[][][] Q;
	private double[] V;
	private double[] P_nopacket;
	
	private double[] Best_value;
	private City[] Best_neigh;

	/**
	 * Setup function is called internally by the main of the logist package
	 * This function calls the Q-learning algorithm in case we choose this policy, and prints
	 * its training time
	 * 
	 * @param topology (Topology): Describes the graph
	 * @param td (TaskDistribution): Gives statistics about probabilities of task and expected weights and rewards
	 * @param agent (Agent): Gives information about agent such as its policy, which
	 * can either be [0: Random, -1: Dummy, (Anything else): Q-Learning], the discount factor, which
	 * will be the actual discount factor for Q-Learning and the probability of picking a task for random policy,
	 * and vehicle.
	 */
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class, 0.95);
		
		// Reads the policy from the agents.xml file.
		// If the property is not present it defaults to Q-Learning
		Integer policy_v = agent.readProperty("policy", Integer.class, 1);
		policy = policy_v;

		// Create Random number generator
		this.random = new Random();
		
		this.discount = discount;
		this.numActions = 1;
		this.myAgent = agent;
		
		System.out.println("Discount factor: " + discount);
		
		if (policy != 0 && policy != -1) {
			long start = System.nanoTime();
			
			// Run Offline Q-Learning
			// Given that there's only one vehicle, get first (and unique) element of list
			reinforce(topology, td, agent.vehicles().get(0));
			
			long end = System.nanoTime();
			//finding the time difference and converting it into seconds
			System.out.println("Elapsed time in training " + (end - start) * 1e-9 + "s");	
		}
		System.out.println("\n \n");
		
	}
	
	/**
	 * Q-Learning function
	 * When acting, we will use the vectors Best_value for the best Q-value for a given city with a Reject action
	 * associated, the Best_neigh for the city where the previous maximum is attained, and the Q tensor
	 * having all the Q-values for the case a pickup is possible. It can be accessed as
	 * Q[source_city][destination_city][0/1], where the first 2 indices are the IDs of the cities
	 * and the last one is 0 for rejecting a task and 1 for accepting it
	 * @param topology (Topology): Describes the graph
	 * @param td (TaskDistribution): Gives statistics about probabilities of task and expected weights and rewards
	 * @param v (Vehicle): Gives information such as cost per kilometer and capacity
	 */
	public void reinforce(Topology topology, TaskDistribution td, Vehicle v) {
		// Initialize arrays to 0	
		P_nopacket = new double[topology.cities().size()];
		V = new double[topology.cities().size()];
		Q = new double[topology.cities().size()][topology.cities().size()][2];
		Best_value = new double[topology.cities().size()];
		
		Best_neigh = new City[topology.cities().size()];
		
		// Compute probability of no having tasks at each city (so T(s,a,s') sums up to one for each s)
		// This value will be repeatedly used later on
		for (City city_a : topology) {
			double p_nopacket = 0;
			for (City city_b : topology) {
				p_nopacket += td.probability(city_a, city_b);
			}
			P_nopacket[city_a.id] = 1 - p_nopacket;
		}
		
		// Iterate UNTIL GOOD ENOUGH
		double dif;
		double old_Qval;
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
					// We can accept a packet leading to any other city (except current) and in the case
					// that we can carry its (expected) weight
					if (td.weight(city_a, city_b) <= v.capacity() && city_a.id != city_b.id) {
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
			}
		} while (dif > EPSILON);
		
		// Compute Best vectors
		for (City city_a: topology.cities()) {
			double max_qval = -9999999; // If 0 can lead to vehicle choosing the current city if q values are negative
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

	/**
	 * The act function is internally called by logist for every step
	 * Policy for actions can either be [0: Random, -1: Dummy, (Anything else): Q-Learning] depending on
	 * the specifications of the agent that owns the vehicle.
	 * @param vehicle (Vehicle): Gives information such as cost per kilometer and capacity
	 * @param availableTask (Task): Description of the task if available (if not, null)
	 * @return Action: Move to the next city according to the chosen policy
	 */
	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action = null;

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
			// Dummy policy - only picks tasks to be delivered in a neighbor city
			City currentCity = vehicle.getCurrentCity();
			
			if (availableTask != null && currentCity.hasNeighbor(availableTask.deliveryCity) ) {
				action = new Pickup(availableTask);
			} else {
				action = new Move(currentCity.randomNeighbor(random));
			}

		default:
			// Off-line Q-Learning: Greedy policy
			City city_a = vehicle.getCurrentCity();
			
			// Best action in case delivery is not possible or not optimal
			action = new Move(Best_neigh[city_a.id]);
			
			if (availableTask != null) {
				if (Best_value[city_a.id] < Q[city_a.id][availableTask.deliveryCity.id][1] 
						&& availableTask.weight <= vehicle.capacity()){
					// Delivery is possible and optimal
					action = new Pickup(availableTask);	
				}
			}
			break;
		}
		
		if (numActions >= 1) {
			System.out.println("The total profit after "+numActions+
					" actions is "+myAgent.getTotalProfit()+" (average profit: "+
					(myAgent.getTotalProfit() / (double)numActions)+")");
		}
		numActions++;
		
		return action;
	}
}
