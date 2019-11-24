package dummies;

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
import main.SLS;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 * 
 */
@SuppressWarnings("unused")
public class AuctionTemplate implements AuctionBehavior {
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
	
	// Preference matrix
	double[][] pmf;
	
	// Opponent's model
	private List<Vehicle> vehiclesToBeat;
	private List<Long> marginsCostToBeat = new ArrayList<Long>(); // actual bid / estimated marginal cost
	private long minCostToBeat; // estimated marginal cost for the current task
	// Correction factor between bid and estimated marginal cost
	private double marginToBeat;
	// 
	private double stdToBeat;
	
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
		
        // Initialize solution representation
		this.plan = new SLS(vehicles, timeoutPlan);
		
		// Initialize opponent's model
		this.vehiclesToBeat = agent.vehicles(); 
		this.marginToBeat = 1;
		this.stdToBeat = 0;
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
		long ratio = 0;
		
		if (winner == agent.id()) {
			// Assign plan in askPrice
			plan.consolidatePlan();
		} else {
			planToBeat.consolidatePlan();
		}
		
		// Analyze bids and winner bid
		long lowestBid = bids[winner];
		if (agent.id() == 1) {
			bidToBeat = bids[0];
		} else {
			bidToBeat = bids[1];
		}
		
		// Analyze opponent bid
		if(minCostToBeat != 0) {
			ratio = bidToBeat/minCostToBeat;
		}
		marginsCostToBeat.add(ratio);
		
		// Update standard deviation of the estimate
		if(numTasks > 2) {
			stdToBeat = (numTasks - 2)/(numTasks - 1) * stdToBeat + (1/numTasks)*(marginToBeat - ratio);
		}
		// Update estimate
		marginToBeat = (1/numTasks)*(ratio - (numTasks - 1)*marginToBeat);
		
		System.out.println("number of processed tasks:" + numTasks);
	}
	
	@Override
	public Long askPrice(Task task) {
		float timeStart = System.currentTimeMillis();
		
		
		// Compute marginal cost for us
		long min_cost = plan.addTask(task, timeoutBid * TIME_FRACTION);
		
		// Compute marginal cost of the opponent for remaining time
		minCostToBeat = planToBeat.addTask(task, timeoutBid - (System.currentTimeMillis() - timeStart)); 
		
		
		// Time spent in the following lines is negligible
		long expectedCost = (long) ((marginToBeat - Math.sqrt(stdToBeat))*minCostToBeat); //expected bid of the opponent
		
		System.out.println("Min cost for random basic" + min_cost);
		
		// TODO: Think smart way to estimate price (use pmf)
		//System.out.println(min_cost);
		if(min_cost == 0) {
			return (long) 300;
			}
		return (long) (min_cost*1.5);
			
		
	}

	/**
	 * This is called after all bids have been placed
	 */
	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        return plan.getFinalPlan(tasks);
	}

}
