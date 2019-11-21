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

	// Random number generator
	private Random random;

	// Solution representation
	private SLS plan;

	// Timeouts
	private long timeoutSetup, timeoutBid, timeoutPlan;

	// Preference matrix
	double[][] pmf;

	// Opponent's model
	private SLS planToBeat;
	private List<Vehicle> vehiclesToBeat;
	private List<Long> marginsCostToBeat = new ArrayList<Long>(); // actual bid / estimated marginal cost
	private long minCostToBeat; // Minimum opponent's bid seen so far
	private long estimatedCost; // estimated marginal cost for the current task

	/*************************** Constants ***************************/
	// Fraction of time spent in computing our optimal plan
	private final static float TIME_FRACTION = 0.7f;
	// Margin of iterations until reaching timeout
	private static final int MARGIN = 50;
	// Limits of interval where preference matrix is used
	private static final double PMF_MIN = 0.9, PMF_MAX = 1.1;

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

		// Seed randomness
		long seed = -9019554669489983951L * agent.id();
		this.random = new Random(seed);

		// Initialize solution representation
		this.plan = new SLS(vehicles, timeoutPlan);

		// Initialize opponent's model
		// Opponent has as many vehicles as we do but initial city is unknown
		// Initial cities are taken u.a.r. without replacement
		ArrayList<City> initCities = new ArrayList<City>(topology.cities());
		for (Vehicle v : this.vehicles) {
			initCities.remove(v.getCurrentCity());
		}

		vehiclesToBeat = new ArrayList<Vehicle>();
		for (Vehicle v : this.vehicles) {
			City initCity = initCities.get(this.random.nextInt(initCities.size()));
			vehiclesToBeat.add(new OpponentVehicle(v, initCity));
			initCities.remove(initCity);
		}
		this.planToBeat = new SLS(vehiclesToBeat, timeoutPlan);

		// Compute preference matrix spending at most the rest of the setup time
		initPmf(timeoutSetup - (System.currentTimeMillis() - timeStart));
	}

	/**
	 * Compute a matrix indicating the preferred tasks without exceeding the timeout
	 * 
	 * @param timeout
	 */
	@SuppressWarnings("resource")
	private void initPmf(float timeout) {
		this.pmf = new double[topology.size()][topology.size()];
		float timeStart, duration;
		float totalDuration = 0;
		float maxDuration = 0;

		// Check which cities are more present in expectation inside a shortest path
		// If topology has been already seen, load precomputed pmf
		String fname = Integer.toString(topology.hashCode()) + ".pmf";
		File f = new File(fname);
		if (f.exists()) {// read in the data
			Scanner input;
			try {
				input = new Scanner(f);
				for (int i = 0; i < topology.size(); ++i) {
					for (int j = 0; j < topology.size(); ++j) {
						if (input.hasNextInt()) {
							pmf[i][j] = input.nextInt();
						}
					}
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		} else {
			double[] connectivity = new double[topology.size()];

			for (City city1 : topology.cities()) {
				for (City city2 : topology.cities()) {
					timeStart = System.currentTimeMillis();

					double prob = distribution.probability(city1, city2);

					for (City interCity : city1.pathTo(city2)) {
						connectivity[interCity.id] += prob;
					}

					duration = System.currentTimeMillis() - timeStart;
					if (maxDuration < duration) {
						maxDuration = duration;
					}
					totalDuration += duration;

					if (totalDuration + MARGIN * maxDuration < timeout) {
						return;
					}
				}
			}

			// Check maximum and minimum values to normalize
			double max = 0, min = Double.MAX_VALUE, cumSum;
			for (City city1 : topology.cities()) {
				for (City city2 : topology.cities()) {
					timeStart = System.currentTimeMillis();

					double prob = distribution.probability(city1, city2);

					cumSum = 0;
					for (City interCity : city1.pathTo(city2)) {
						cumSum += connectivity[interCity.id];
					}
					pmf[city1.id][city2.id] = cumSum;

					if (cumSum > max) {
						max = cumSum;
					} else if (cumSum < min) {
						min = cumSum;
					}

					duration = System.currentTimeMillis() - timeStart;
					if (maxDuration < duration) {
						maxDuration = duration;
					}
					totalDuration += duration;

					if (totalDuration + MARGIN * maxDuration < timeout) {
						return;
					}
				}
			}

			// Normalize the pmf from 0 to 1
			BufferedWriter bw;
			try {
				bw = new BufferedWriter(new FileWriter(fname));
				for (City city1 : topology.cities()) {
					for (City city2 : topology.cities()) {
						timeStart = System.currentTimeMillis();

						pmf[city1.id][city2.id] = (pmf[city1.id][city2.id] - min) / (max - min);

						bw.write(pmf[city1.id][city2.id] + ",");

						duration = System.currentTimeMillis() - timeStart;
						if (maxDuration < duration) {
							maxDuration = duration;
						}
						totalDuration += duration;

						if (totalDuration + MARGIN * maxDuration < timeout) {
							return;
						}
					}
					bw.newLine();
				}
				bw.flush();
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		this.numTasks++;
		if (winner == agent.id()) {
			// Assign plan in askPrice
			plan.consolidatePlan();
		} else {
			planToBeat.consolidatePlan();
		}

		System.out.println("Number of processed tasks:" + numTasks);
	}

	@Override
	public Long askPrice(Task task) {
		float timeStart = System.currentTimeMillis();

		// Compute marginal cost for us
		long minCost = plan.addTask(task, timeoutBid * TIME_FRACTION);
		
		System.out.println("Minimum cost is: " + minCost);

		// Compute marginal cost of the opponent for remaining time
		minCostToBeat = planToBeat.addTask(task, timeoutBid - (System.currentTimeMillis() - timeStart));
		
		double bid = minCost * pmf[task.pickupCity.id][task.deliveryCity.id] * (PMF_MAX - PMF_MIN) + PMF_MIN;
		
		// TODO: How to use opponent's estimated marginal cost/minCostToBeat?		
		long finalBid = (long) Math.floor(bid);

		System.out.println("Bid is: " + finalBid);
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
