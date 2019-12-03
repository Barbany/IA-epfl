package main;

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
public class OppModel implements AuctionBehavior {
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
	long savings, expenses, minCost, margin;
	int strategy;
	
	// Opponent's model
	private List<Vehicle> oppVehicles;
	private SLS oppPlan;
	private List<Integer> randomCities;
	private long savingsOpp, minBidOpp, minCostOpp;
	
	private static final double PERCENT_OPP = 0.2;

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
		
		// Initialize bank variables
		savings = 4000;
		expenses = 0;
		strategy = 1;

		// Initialize solution representation
		this.plan = new SLS(vehicles, timeoutPlan);
		
		/*************** Initialize opponent's model ***************/
		// Find possible initial cities for opponent
		ArrayList<City> emptyCities = new ArrayList<City>(topology.cities());
		for(Vehicle v: vehicles) {
			emptyCities.remove(v.homeCity());
		}
		
		Random rand = new Random();
		
		// Initialize vehicle to random city
		// Keep track of the random cities (then, we'll change them by "better case" cities)
		this.oppVehicles = new ArrayList<Vehicle>();
		this.randomCities = new ArrayList<Integer>();
		
		Integer i = 0;
		for(Vehicle v: vehicles) {
			City startCity = emptyCities.get(rand.nextInt(emptyCities.size()));
			emptyCities.remove(startCity);
			this.oppVehicles.add(new OpponentVehicle(v, startCity));
			this.randomCities.add(i);
			i = i + 1;
		}
		
		this.savingsOpp = 0;
		
		// Hardcoded initial value
		this.minBidOpp = Long.MAX_VALUE;
		
		// Initialize opponent's solution
		this.oppPlan = new SLS(oppVehicles, timeoutPlan);

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
		
		// Set margin
		if(numTasks == 1) {
			margin = 0;
			int maxCostPerKm = 0;
			
			for(Vehicle v: oppVehicles) {
				margin = Math.max(margin, (long) v.homeCity().distanceTo(previous.pickupCity));
				maxCostPerKm = Math.max(maxCostPerKm, v.costPerKm());
			}
			
			margin *= maxCostPerKm;
		}
		
		for(int i=0; i < bids.length; i++) {
			System.out.println("Bid "+ i + " " + bids[i]);
		}
		
		if (winner == agent.id()) {
			// Assign plan in askPrice
			plan.consolidatePlan();
			// Update expenses
			expenses = expenses + minCost - bids[winner];
			System.out.println("got from this " +  (minCost - bids[winner]));
			
			if (bids[1 - winner] < minBidOpp) { 
				minBidOpp = bids[1 - winner];
			}
		}else {
			oppPlan.consolidatePlan();
			
			savingsOpp += bids[winner] - minCostOpp;
			
			if (bids[winner] < minBidOpp) {
				minBidOpp = bids[winner];
			}
		}
		
		// For first three tasks
		if(numTasks <= 2) {
			if(expenses == 0)	
				savings += 100;
			else
				savings = (3 - numTasks) * 1200;
		} else if (numTasks <= 5){
			strategy = 2;
			savings = -expenses; 
		} else {
			strategy = 3;
			savings = -expenses; 
		}

		System.out.println("Expenses bank " + expenses + " with stategy " + strategy);
	}

	@Override
	public Long askPrice(Task task) {
		// Compute marginal cost for us
		minCost = plan.addTask(task, (long) ((1 - PERCENT_OPP) * timeoutBid));
		
		// Compute marginal cost for opponent
		minCostOpp = oppPlan.addTask(task, (long) (PERCENT_OPP * timeoutBid));
		
		// Compute bid
		long bid; 
		
		// Initial bids allow losing money
		if (numTasks < 3) {
			if (minCost > 0) {
				
				bid = (long) Math.floor(minCost - (pmf[task.pickupCity.id][task.deliveryCity.id]) * savings);
			} else {
				// If the cost is zero, start recovering loses
				bid = (long) Math.floor(minCost + (pmf[task.pickupCity.id][task.deliveryCity.id]) * expenses);
			}	
			System.out.println("Case 1. BID " + bid + " cost opp " + minCostOpp);
			bid = (long) Math.min(bid, 0.9 * minCostOpp);
		} else {
			
			if(savingsOpp + margin < -expenses) { // Winning: We can bid low so opponent doesn't get any task
				// Bid low but don't lose
				bid = (long) Math.max(minCost - 0.5*margin, minBidOpp);
				System.out.println("Case 2. BID " + bid + " cost opp " + minCostOpp);
				
			} else { // Not winning
				
				if(expenses == 0) {
					
					// Bid low, specially for "good" tasks
					// Change the 2 by 1 / (Threshold of losing money w.r.t. Opp's model)
					bid = (long) Math.floor(pmf[task.pickupCity.id][task.deliveryCity.id] * 2 *
							(minCostOpp - minCost) + minCost);
					System.out.println("Case 3. BID " + bid + " cost opp " + minCostOpp);
				} else if (minCost == 0){
					// If our cost is zero and so it is the opponent, try to decrease the margin to win
					if (minCostOpp == 0) {
						bid = (long) Math.max(0.6f *(savingsOpp -expenses), 100);
						System.out.println("Case 4.1 . BID " + bid + " cost opp " + minCostOpp);
					} else {
						// Bid slightly lower than min opponent
						bid = (long) Math.floor(0.9f * minCostOpp);
						
						System.out.println("Case 4.2 . BID " + bid + " cost opp " + minCostOpp + " multip " + 0.9 *minCostOpp);
						}
					
					
				} else {
					
					bid = (long) Math.max(minCost* 1.1f, 0.9f * minCostOpp);
					System.out.println("Case 5. BID " + bid + " cost opp " + minCostOpp);
				}
			}
		}

		System.out.println("Ozuna : -----------------");
		System.out.println("Ozuna : Minimum cost is  : " + minCost + " probability: " + pmf[task.pickupCity.id][task.deliveryCity.id]);
		System.out.println("Ozuna : Min cost opponent: " + minCostOpp);
		System.out.println("Ozuna : Final bid is     : " + bid);
		System.out.println("Ozuna : Number of tasks  : " + numTasks);
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
