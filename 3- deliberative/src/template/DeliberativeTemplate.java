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
import template.Utils;

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
		
		boolean [] loopDetector = new boolean[topology.cities().size()];
		
		//return BSTAux(new Plan(current), current, (TaskSet) Collections.<Task>emptySet(), tasks, vehicle.capacity());
		return BSTAux(new Plan(current), current, openTasks, tasks, vehicle.capacity(), loopDetector);

	}
	
	private Plan BSTAux(Plan plan, City current, TaskSet openTasks, TaskSet newTasks, int freeSpace, boolean[] loopDetector) { // error: no pot fer cast a TaskSet; deixar com set normal
		Task currentTask = null;
		
		if(loopDetector[current.id]) {
			return null; 
		}
		
		// Indicate we already visited the city
		loopDetector[current.id] = true;
		
		
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
		//List<Plan> comb = CombinationTasks(newTasks.iterator(), current, freeSpace, Arrays.asList(plan), plan);
		// List<List<Task>> emptyListTask = new ArrayList<>();
		int size = 1; 
	
		// Get all combination of tasks
		List<TaskSet> powerSet = new LinkedList<TaskSet>();
		it = newTasks.iterator();
		while (it.hasNext()) {
			if (it.next().deliveryCity.equals(current)) {
				powerSet.addAll(Utils.combination(newTasks, size));
				size ++; 
			}
			
		}
		
		
		
		Plan currentPlan = plan;
		// Check all combinations of neighbor cities and possible taken tasks
		for(City neigh : current.neighbors()) {
			// Check all the possibilities involving at least one packet pickup
			Iterator<TaskSet> it_tasks = powerSet.iterator();
			
			
			// Iterate along the list of lists of tasks
			while(it_tasks.hasNext()) {
				TaskSet toDoTasks = it_tasks.next();
				
				// If feasible taskSet
				if (toDoTasks.weightSum() <= freeSpace) {
					Iterator<Task> it_toDoTasks = toDoTasks.iterator();
					
					// Iterate along the tasks in the list of tasks - add them to the plan
					while(it_toDoTasks.hasNext()) {
						currentTask = it_toDoTasks.next();
						openTasks.add(currentTask);
						newTasks.remove(currentTask);
						plan.appendPickup(currentTask);
						
						
						
					}
					
					// Reset 
					plan.appendMove(neigh); // Move to neighbor city
					plan = BSTAux(plan, neigh, openTasks, newTasks, freeSpace, new boolean[topology.cities().size()]); //Update plan
					
					if(plan!= null) {
						// If the new plan has lower cost than the actual or the actual is empty
						if(currentPlan.totalDistance() > plan.totalDistance() || currentPlan.totalDistance() == 0) {
							currentPlan = plan; 
							}
						
					}
					
					
	
				}
			}
			// Move without picking up any task
			plan.appendMove(neigh);
			
			plan = BSTAux(plan, neigh, openTasks, newTasks, freeSpace, loopDetector); 
			
			// Check if we found a better plan
			if(plan!= null) {
				// If the new plan has lower cost than the actual or the actual is empty
				if(currentPlan.totalDistance() > plan.totalDistance() || currentPlan.totalDistance() == 0) {
					currentPlan = plan; 
					}
				
			}
		}
		/*for (City city : current.pathTo(task.pickupCity))
			plan.appendMove(city);

		plan.appendPickup(task);

		// move: pickup location => delivery location
		for (City city : task.path())
			plan.appendMove(city);

		plan.appendDelivery(task);*/
		return currentPlan ;
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
