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
public class AuctionTemplate implements AuctionBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private Random random;
	private List<Vehicle> vehicles;
	private List<Vehicle> vehiclesToBeat;
	
	private long timeoutSetup, timeoutBid, timeoutPlan;
	private SLS plan, planToBeat;
	double[][] pmf;
	
	private List<Long> marginsCostToBeat = new ArrayList<Long>(); // actual bid / estimated marginal cost
	private long minCostToBeat; // estimated marginal cost for the current task
	private long marginToBeat = 1; // correction factor between bid and estimated marginal cost
	private long stdToBeat = 0;
	private int numTasks;
	

	@Override
	public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
		long timeStart = System.currentTimeMillis();

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		
		this.vehicles = agent.vehicles();
		this.vehiclesToBeat = agent.vehicles(); 
		
		this.marginToBeat = 1; 
		this.numTasks = 0; 
		
		
		
		// this code is used to get the timeouts
        LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config" + File.separator + "settings_auction.xml");
        }
        catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }
        // the plan method cannot execute more than timeout_plan milliseconds
        timeoutSetup = ls.get(LogistSettings.TimeoutKey.SETUP);
        timeoutBid = ls.get(LogistSettings.TimeoutKey.BID);
        timeoutPlan = ls.get(LogistSettings.TimeoutKey.PLAN);

		long seed = -9019554669489983951L * agent.id();
		this.random = new Random(seed);
		
		this.plan = new SLS(vehicles, timeoutBid, timeoutPlan);
		this.planToBeat = new SLS(vehiclesToBeat, timeoutBid, timeoutPlan);
		
		initPmf(timeoutSetup - (System.currentTimeMillis() - timeStart));
	}
	
	private void initPmf(float timeout) {
		this.pmf = new double[topology.size()][topology.size()];
		float timeStart, duration;
		float totalDuration = 0;
		float maxDuration = 0;
		
		// TODO: Weight probability of task by reward and connectivity of path (at least endpoints)
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
		
		//System.out.println(bids.toString());
		if (winner == agent.id()) {
			// Assign plan in askPrice
			plan.consolidatePlan();
		} else {
			planToBeat.consolidatePlan();
		}
		// TODO: Analyze bids and winner bid
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
		
		// Update std of the estimate
		if(numTasks > 2) {
			stdToBeat = (numTasks - 2)/(numTasks - 1) * stdToBeat + (1/numTasks)*(marginToBeat - ratio);
		}
		// Update estimate
		marginToBeat = (1/numTasks)*(ratio - (numTasks - 1)*marginToBeat);
		
		System.out.println("number of processed tasks:"+numTasks);
		
		
	}
	
	@Override
	public Long askPrice(Task task) {
		// Insert new task into current plan
		long min_cost = plan.addTask(task); // marginal cost for us
		minCostToBeat = planToBeat.addTask(task); //marginal cost of the opponent 
		
		long expectedCost = (marginToBeat - stdToBeat)*minCostToBeat; //expected bid of the opponent
		
		System.out.println(min_cost);
		
		// TODO: Think smart way to estimate price (use pmf)
		//System.out.println(min_cost);
		if (agent.name().contentEquals("auction-random") ){
			if(min_cost == 0) {
				return (long) 300;
			} 
			return (long) (min_cost*1.5);
			
		} else {
			if (expectedCost > min_cost) {
				System.out.println(agent.name() + " Bid for task "+ numTasks + " is " + expectedCost*0.9 + " with mincost " + min_cost);
				// raise bid up to a certain safety margin
				return (long) (expectedCost*0.9);
				
			} else {
				// Mirar si ens val la pena baixar la bid pero en general no! 
				
				System.out.println(agent.name() + " Bid for task "+ numTasks + " is " + min_cost + " with mincost " + min_cost);
				return (long) (min_cost);
				
			}
		}
	}

	/**
	 * This is called after all bids have been placed
	 */
	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        return plan.getFinalPlan(tasks);
	}

	private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		for (Task task : tasks) {
			// move: current city => pickup location
			for (City city : current.pathTo(task.pickupCity))
				plan.appendMove(city);

			plan.appendPickup(task);

			// move: pickup location => delivery location
			for (City city : task.path())
				plan.appendMove(city);

			plan.appendDelivery(task);

			// set current city
			current = task.deliveryCity;
		}
		return plan;
	}
}
