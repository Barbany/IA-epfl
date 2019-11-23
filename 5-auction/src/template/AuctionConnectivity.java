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
public class AuctionConnectivity implements AuctionBehavior {
	// Basic information about the problem
	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private List<Vehicle> vehicles;
	private int numTasks;

	// Solution representation
	private SLS plan;

	// Timeouts
	private long timeoutSetup, timeoutBid, timeoutPlan;

	// Preference matrix
	double[][] pmf;
	
	// Limits of interval where preference matrix is used
	private double pmfMin;
	private double pmfMax;

	/*************************** Constants ***************************/
	// Margin of iterations until reaching timeout
	private static final int MARGIN = 50;
	// Number of iterations when we only have gains
	private static final double IT_GAIN = 10.0;
	// Initial values of margins
	private static final double PMF_MIN = 0.4;
	private static final double PMF_MAX = 0.5;

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

		// Initialize solution representation
		this.plan = new SLS(vehicles, timeoutPlan);

		// Initialize opponent's model
		// Opponent has as many vehicles as we do but initial city is unknown
		// Initial cities are taken u.a.r. without replacement
		ArrayList<City> initCities = new ArrayList<City>(topology.cities());
		for (Vehicle v : this.vehicles) {
			initCities.remove(v.getCurrentCity());
		}
		
		// Initialize margins
		pmfMin = PMF_MIN;
		pmfMax = PMF_MAX;

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
		// If topology has been already seen, load precomputed pmf
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
		if (winner == agent.id()) {
			// Assign plan in askPrice
			plan.consolidatePlan();
			this.numTasks++;
		}

		System.out.println("Number of won tasks:" + numTasks);
	}

	@Override
	public Long askPrice(Task task) {
		// Compute marginal cost for us
		long minCost = plan.addTask(task, timeoutBid);

		System.out.println("Conn: Minimum cost is: " + minCost);
		
		// Update pmf margins
		if(numTasks < IT_GAIN) {
			pmfMin += (1.0 - PMF_MIN) / IT_GAIN;
			pmfMax += (1.2 - PMF_MAX) / IT_GAIN;
		}

		double bid = minCost * (pmf[task.pickupCity.id][task.deliveryCity.id] * (pmfMin - pmfMax) + pmfMax);

		long finalBid = (long) Math.floor(bid);

		System.out.println("Conn: Final bid is: " + finalBid);
		return finalBid;
	}

	/**
	 * This is called after all bids have been placed
	 */
	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		return plan.getFinalPlan(tasks);
	}

}
