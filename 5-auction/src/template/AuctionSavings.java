package template;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
//the list of imports
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import logist.LogistSettings;
import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.config.Parsers;
import logist.agent.Agent;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 * 
 */
@SuppressWarnings("unused")
public class AuctionSavings implements AuctionBehavior {
	// Basic information about the problem
	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private List<Vehicle> vehicles;
	private int numTasks;

	// Random number generator
	private Random random;

	// Solution representation
	private SLS plan;

	// Timeouts
	private long timeoutSetup, timeoutBid, timeoutPlan;

	// Preference matrix
	double[][] pmf;
	
	// Bank variables
	long savings, expenses, minCost; 
	int strategy; 

	@Override
	public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
		// Start timer
		long timeStart = System.currentTimeMillis();

		// Initialize basic information about the problem
		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.vehicles = agent.vehicles();

		// Get the timeouts
		LogistSettings ls = null;
		try {
			ls = Parsers.parseSettings("config" + File.separator + "settings_auction.xml");
		} catch (Exception exc) {
			System.out.println("There was a problem loading the configuration file.");
		}
		timeoutSetup = ls.get(LogistSettings.TimeoutKey.SETUP);
		timeoutBid = ls.get(LogistSettings.TimeoutKey.BID);
		timeoutPlan = ls.get(LogistSettings.TimeoutKey.PLAN);
		timeoutBid = 5*1000;
		timeoutPlan = 5*1000;
		
		// Initialize bank variables
		savings = 3000;
		expenses = 0;
		strategy = 1; 

		// Initialize solution representation
		this.plan = new SLS(vehicles, timeoutPlan);

		// Compute preference matrix spending at most the rest of the setup time
		initPmf(timeoutSetup - (System.currentTimeMillis() - timeStart));
	}

	/**
	 * Compute a matrix indicating the preferred tasks without exceeding the timeout
	 * 
	 * @param timeout
	 */
	private void initPmf(double timeout) {
		this.pmf = new double[topology.size()][topology.size()];
		double timeStart, duration;
		double totalDuration = 0;
		double maxDuration = 0;

		// Check which cities are more present in expectation inside a shortest path
		String fname = Integer.toString(topology.hashCode()) + ".pmf";
		double[] connectivity = new double[topology.size()];

		for (City city1 : topology.cities()) {
			for (City city2 : topology.cities()) {
				
				double prob = distribution.probability(city1, city2);

				for (City interCity : city1.pathTo(city2)) {
					connectivity[interCity.id] += prob;
				}
			}
		}

		// Check maximum and minimum values to normalize
		double max = 0, min = Double.MAX_VALUE, cumSum;
		for (City city1 : topology.cities()) {
			for (City city2 : topology.cities()) {
				double prob = distribution.probability(city1, city2);

				cumSum = 0;
				for (City interCity : city1.pathTo(city2)) {
					cumSum += connectivity[interCity.id];
				}
				pmf[city1.id][city2.id] = cumSum;
				
				if(max < cumSum) {
					max = cumSum;
				} else if(min > cumSum) {
					min = cumSum;
				}
			}
		}

		// Normalize the pmf from 0 to 1
		for (City city1 : topology.cities()) {
			for (City city2 : topology.cities()) {
				timeStart = System.currentTimeMillis();

				pmf[city1.id][city2.id] = (pmf[city1.id][city2.id] - min) / (max - min);
			}
		}
	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		numTasks++;
		
		
		for(int i=0; i < bids.length; i++) {
			System.out.println("Bid "+ i + " " + bids[i]);
		}
		
		if (winner == agent.id()) {
			// Assign plan in askPrice
			plan.consolidatePlan();
			// Update expenses
			expenses = expenses + minCost - bids[winner];
			System.out.println("got from this " +  (minCost - bids[winner]));
		}
		
		// For first three tasks
		if(numTasks <= 2) {
			savings = (2 - numTasks) * 1200;
		} else if (numTasks <= 5){
			strategy = 2;
			//savings = (long) -Math.floor(0.5*expenses);
			savings = -expenses; 
		} else {
			strategy = 3;
			savings = -expenses; 
		}

		System.out.println("Expenses bank " + expenses + " with stategy " + strategy);
	}

	@Override
	public Long askPrice(Task task) {
		double timeStart = System.currentTimeMillis();

		// Compute marginal cost for us
		minCost = plan.addTask(task, timeoutBid);

		System.out.println("Bank: Minimum cost is: " + minCost + " propobility: " + pmf[task.pickupCity.id][task.deliveryCity.id]);
		
		long bid; 
		if (expenses > 0) {
			bid = (long) Math.floor(minCost + (pmf[task.pickupCity.id][task.deliveryCity.id] - 0.1*strategy*numTasks) * savings);
			System.out.println("extra payment " + (pmf[task.pickupCity.id][task.deliveryCity.id] - 0.1*strategy*numTasks) * savings);
		} else{
			if (minCost < 1000) {
				System.out.println("bidding min");
				bid = (long) Math.floor(minCost - (pmf[task.pickupCity.id][task.deliveryCity.id] - 0.4*strategy) * savings);
			} else {
				bid = (long) Math.floor(minCost - (pmf[task.pickupCity.id][task.deliveryCity.id] - 0.2*strategy) * savings);
			}
			
		}
		
		
		System.out.println("Bank: Final bid is: " + bid);
		
		return bid;
	}

	/**
	 * This is called after all bids have been placed
	 */
	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		return plan.getFinalPlan(tasks);
	}

}
