package template;

import java.io.File;
//the list of imports
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
public class AuctionEstimateLow implements AuctionBehavior {
	// Basic information about the problem
	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private List<Vehicle> vehicles;
	private double numTasks;
	
	// Solution representation
	private SLS plan, planToBeat;
	
	// Timeouts
	private long timeoutSetup, timeoutBid, timeoutPlan;
	
	// Random number generator
	private Random random;
	
	// Preference matrix
	double[][] pmf;
	
	// Opponent's model
	private List<Vehicle> vehiclesToBeat;
	private long minCostToBeat; // estimated marginal cost for the current task
	private long maxCostToBeat; 
	private long costToBeat; 
	// Correction factor between bid and estimated marginal cost
	private double ratioToBeat = 1;
	private double stdToBeat;
	
	// Our model
	private long myCost; 
	private double myRatio = 1; 
	
	/*************************** Constants ***************************/
	// Fraction of time spent in computing our optimal plan
	private final static float TIME_FRACTION = 0.7f;
	

	@Override
	public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
		// Start timer
		long timeStart = System.currentTimeMillis();

		// Initialize basic information about the problem
		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.vehicles = agent.vehicles();
		this.numTasks = 0; 
		
		// Get the timeouts
        LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config" + File.separator + "settings_auction.xml");
        }
        catch (Exception exc) {
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
	 * @param timeout
	 */
	private void initPmf(float timeout) {
		this.pmf = new double[topology.size()][topology.size()];
		float timeStart, duration;
		float totalDuration = 0;
		float maxDuration = 0;
		
		// Weight probability of task by reward and connectivity of path (at least endpoints)
		// Normalize everything so we have an actual pmf (easier when generating randoms)
		for (City city1 : topology.cities()) {
			for (City city2 : topology.cities()) {
				timeStart = System.currentTimeMillis();
				
				pmf[city1.id][city2.id] = distribution.probability(city1, city2);

				duration = System.currentTimeMillis() - timeStart;
				if (maxDuration < duration) {
					maxDuration = duration;
				}
				totalDuration += duration;
				
				if (totalDuration + maxDuration < timeout*0.9) {
					return;
				}
			}
		}
	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		long bidToBeat;
		this.numTasks ++;
		long ratio = 1;
		
		if (winner == agent.id()) {
			// Assign plan in askPrice
			plan.consolidatePlan();
		} else {
			planToBeat.consolidatePlan();
		}
		
		// Analyze bids and winner bid
		long lowestBid = bids[winner];
		// select bid of the opponent
		if (agent.id() == 1) {
			bidToBeat = bids[0];
		} else {
			bidToBeat = bids[1];
		}
		
		
		
		// Analyze opponent bid
		if(minCostToBeat != 0) {
			ratio = bidToBeat/minCostToBeat;
		}
		
		// Update standard deviation of the estimate
		if(numTasks > 2) {
			stdToBeat = (numTasks - 2)/(numTasks - 1) * stdToBeat + (1/numTasks)*(ratioToBeat - ratio);
		}
		// Update estimate
		ratioToBeat = (1/numTasks)*(ratio - (numTasks - 1)*ratioToBeat);
		
		System.out.println("number of processed tasks:" + numTasks);
	}
	
	@Override
	public Long askPrice(Task task) {
		long timeStart = System.currentTimeMillis();
		long currentBid; 
		
		
		// Compute marginal cost for us
		System.out.println("here oke");
		myCost = plan.addTask(task, timeoutBid * TIME_FRACTION);
		// Compute marginal cost of the opponent for remaining time
		System.out.println("here oke the other");
		costToBeat = planToBeat.addTask(task, timeoutBid*0.9 - (System.currentTimeMillis() - timeStart)); 
		
		// Select highest bid
		currentBid = (long) Math.max(costToBeat*ratioToBeat, myCost*myRatio);
		
		return currentBid;
		
		
		
		// TODO: Think smart way to estimate price (use pmf)
		//System.out.println(min_cost);
		/** 
		if (costToBeat > myCost) {
			//System.out.println(agent.name() + " Bid for task "+ numTasks + " is " + expectedCost*0.9 + " with mincost " + min_cost);
			// raise bid up to a certain safety margin
			// TODO: Instead of 0.9, - 3 std ?
			System.out.println("Estimated cost " + costToBeat + "; Bidding cost " + (ratioToBeat- 3*stdToBeat)*minCostToBeat + "; My cost" + min_cost);
			return (long) (ratioToBeat- 3*stdToBeat)*costToBeat;
			
				
		} else {
			// TODO: Mirar si ens val la pena baixar la bid pero en general no!
			System.out.println("Estimated cost " + costToBeat + "; Bidding cost " + myCost);
			return (long) (myCost);
		}
		*/
		
	}

	/**
	 * This is called after all bids have been placed
	 */
	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        return plan.getFinalPlan(tasks);
	}

}
