package template;

/* import table */
import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.*;

/**
 * An optimal planner for one vehicle.
 */
@SuppressWarnings("unused")
public class DeliberativeTemplate implements DeliberativeBehavior {

	enum Algorithm { BFS, ASTAR }
	
	/* Environment */
	Topology topology;
	TaskDistribution td;
	
	/* the properties of the agent */
	Agent agent;
	int capacity;

	/* the planning class */
	Algorithm algorithm;
	
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		this.topology = topology;
		this.td = td;
		this.agent = agent;
		
		// initialize the planner
		int capacity = agent.vehicles().get(0).capacity();
		String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");
		
		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());
		
		// ...
	}
	
	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		Plan plan;

		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			// ...
			plan = naivePlan(vehicle, tasks);
			break;
		case BFS:
			plan = BSTPlan(vehicle, tasks);
			//plan = naivePlan(vehicle, tasks);
			break;
		default:
			throw new AssertionError("Should not happen.");
		}		
		return plan;
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

	/**
	 * Function that returns an optimal plan computed with BST
	 * This function uses the recursive method BSTAux
	 * @param vehicle
	 * @param tasks
	 * @return Optimal plan
	 */
	private Plan BSTPlan(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		// return BSTAux(new Plan(current), current, (TaskSet) Collections.<Task>emptySet(), tasks, vehicle.capacity()); 
		TaskSet openTasks = tasks.clone();
		openTasks.clear(); // empty set of tasks since it has not done any task yet. 
		
		//return BSTAux(new Plan(current), current, (TaskSet) Collections.<Task>emptySet(), tasks, vehicle.capacity());
		return BSTAux(new Plan(current), current, openTasks, tasks, vehicle.capacity());

	}
	
	private Plan BSTAux(Plan plan, City current, TaskSet openTasks, TaskSet newTasks, int freeSpace) { // error: no pot fer cast a TaskSet; deixar com set normal
		Task currentTask = null;
		
		// Check if open tasks have current city as destination
		Iterator<Task> it = openTasks.iterator();
		while(it.hasNext()) {
			
			currentTask = it.next();
			if(currentTask.deliveryCity.equals(current)) {
				freeSpace += currentTask.weight;
				plan.appendDelivery(currentTask);
				openTasks.remove(currentTask);
			}
		}
		
		// Check if state is terminal
		if(newTasks.isEmpty() && openTasks.isEmpty()) {
			return plan;
		}

		
		//List<Plan> comb = CombinationTasks(newTasks.iterator(), current, freeSpace, Collections.<Plan>singletonList(plan), plan);
		List<Plan> comb = CombinationTasks(newTasks.iterator(), current, freeSpace, Arrays.asList(plan), plan);
		
		Plan currentPlan = plan;
		// Check all combinations of neighbor cities and possible taken tasks
		for(City neigh : current.neighbors()) {
			// Check all the possibilities involving at least one packet pickup
			Iterator<Plan> it_plan = comb.iterator();
			while(it_plan.hasNext()) {
				currentPlan = it_plan.next();
				openTasks.add(currentTask);
				newTasks.remove(currentTask);
				plan.appendPickup(currentTask); //Shouldn't it be Pickup?  - EXTRA (changed to Pickup)
				
				plan.appendMove(neigh); // Move to neighbor city
				plan = BSTAux(plan, neigh, openTasks, newTasks, freeSpace); //Update plan
			}
			plan.appendMove(neigh);
			plan = BSTAux(plan, neigh, openTasks, newTasks, freeSpace); // Perform no Pickup action - EXTRA // Update plan
			// Check option not picking up any packet
		}
		/*for (City city : current.pathTo(task.pickupCity))
			plan.appendMove(city);

		plan.appendPickup(task);

		// move: pickup location => delivery location
		for (City city : task.path())
			plan.appendMove(city);

		plan.appendDelivery(task);*/
		return plan;
	}
	
	/**
	 * Return all combinations of tasks available in a city
	 * E.g. If tasks 1,2,3 available, return {1,2,3,1+2,1+3,2+3,1+2+3}
	 * @param it
	 * @param openTasks
	 * @param newTasks
	 * @param current
	 * @param freeSpace
	 * @return
	 */
	
	
	private List<Plan> CombinationTasks(Iterator<Task> it, City current, int freeSpace, List<Plan> plans, Plan currentPlan) {
		
		
		while(it.hasNext()) {
			Task currentTask = it.next();
			// Consider what to do with tasks that we can actually pick up
			if(currentTask.pickupCity.equals(current) && currentTask.weight <= freeSpace) {
				// Leave it
				CombinationTasks(it, current, freeSpace, plans, currentPlan);
				
				// Take it
				currentPlan.appendPickup(currentTask);
				System.out.println("Plan " + currentPlan.toString());
				System.out.println("List Plan " + plans.size());
				plans.add(currentPlan);
				CombinationTasks(it, current, freeSpace - currentTask.weight, plans, currentPlan); 
			}
		}
		currentPlan = new Plan(current); 
		return plans;
	}

	@Override
	public void planCancelled(TaskSet carriedTasks) {
		
		if (!carriedTasks.isEmpty()) {
			// This cannot happen for this simple agent, but typically
			// you will need to consider the carriedTasks when the next
			// plan is computed.
		}
	}
}
