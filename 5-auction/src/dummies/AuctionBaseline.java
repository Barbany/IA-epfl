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
public class AuctionBaseline implements AuctionBehavior {
	// Basic information about the problem
	private Agent agent;
	private List<Vehicle> vehicles;
	private int numTasks;
	
	// Random number generator
	private Random random;
	
	// Solution representation
	private SLS plan;
	
	// TODO: REMOVE!! ONLY FOR PLOTS 
	List<Long> bidsTotal, minCostTotal;
		
	
	// Timeouts
	private long timeoutSetup, timeoutBid, timeoutPlan;
	
	/********************** Constants **********************/
	private static final double STD = 0.1;
	private static final double GAIN_MARGIN = 0.2;
	

	@Override
	public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
		// Initialize basic information about the problem
		this.agent = agent;
		this.vehicles = agent.vehicles();
		this.numTasks = 0; 
		
		bidsTotal = new ArrayList<Long>();
		minCostTotal = new ArrayList<Long>();
		
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
	}
	
	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		this.numTasks ++;
		
		if (winner == agent.id()) {
			// Assign plan in askPrice
			plan.consolidatePlan();
		}
		
		System.out.println("Number of processed tasks:" + numTasks);
	}
	
	@Override
	public Long askPrice(Task task) {
		double timeStart = System.currentTimeMillis();
		
		// Compute marginal cost for us
		long min_cost = plan.addTask(task, timeoutPlan);
		long bid = (long) (min_cost + 250 * 0.05 * (numTasks - 3));
		
		
		System.out.println("Baseline: -----------------");
		System.out.println("Baseline: Minimum cost is: " + min_cost );
		System.out.println("Baseline: Final bid is   : " + bid);
		System.out.println("Baseline: Number of tasks: " + numTasks);
		bidsTotal.add(bid);
		minCostTotal.add(min_cost);
		
		return bid;
	}

	/**
	 * This is called after all bids have been placed
	 */
	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		System.out.println("Baseline BID HISTORIC: " + bidsTotal + " ;");
		System.out.println("Baseline COST HISTORIC: " + minCostTotal + " ;");
        return plan.getFinalPlan(tasks);
	}

}
