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
import template.State;

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

	private Plan BSTPlan(Vehicle vehicle, TaskSet tasks) {
		City currentCity = vehicle.getCurrentCity();
		
		boolean visited[] = new boolean[topology.cities().size()];
		
		LinkedList<State> queue = new LinkedList<State>();
		LinkedList<Plan> finalPlans = new LinkedList<Plan>();
		
		visited[currentCity.id] = true;
		// TODO: Mapping pickup_city -> newTasks, delivery_city -> openTasks
		queue.add(new State(new Plan(currentCity), currentCity, TaskSet.create(null), tasks, vehicle.capacity(), visited));
		
		while(!queue.isEmpty()) {
			// Get current state
			State s = queue.poll();
			Task currentTask = null;
			
			// Check if open tasks have current city as destination
			Iterator<Task> it = s.openTasks.iterator();
			while(it.hasNext()) {
				currentTask = it.next();
				if(currentTask.deliveryCity.equals(s.currentCity)) {
					s.freeSpace += currentTask.weight;
					s.plan.appendDelivery(currentTask);
					s.openTasks.remove(currentTask);
				}
			}
			
			// Check if state is terminal
			// If it's the case, add it to final Plan list
			if(s.newTasks.isEmpty() && s.openTasks.isEmpty()) {
				finalPlans.add(s.plan);
			}
		
			// Get all combination of tasks
			int size = 1; 
			List<TaskSet> powerSet = new LinkedList<TaskSet>();
			it = s.newTasks.iterator();
			while (it.hasNext()) {
				if (it.next().deliveryCity.equals(s.currentCity)) {
					powerSet.addAll(Utils.combination(s.newTasks, size));
					size ++; 
				}	
			}
			
			// Check all combinations of neighbor cities and possible taken tasks
			for(City neigh : s.currentCity.neighbors()) {
				// Check all the possibilities involving at least one packet pickup
				Iterator<TaskSet> it_tasks = powerSet.iterator();
				
				// Iterate along the list of lists of tasks
				while(it_tasks.hasNext()) {
					TaskSet toDoTasks = it_tasks.next();
					
					// If feasible taskSet
					if (toDoTasks.weightSum() <= s.freeSpace) {
						Iterator<Task> it_toDoTasks = toDoTasks.iterator();
						
						TaskSet openTasksAux = s.openTasks.clone();
						TaskSet newTasksAux = s.newTasks.clone();
						// TODO: Here we should do a clone instead
						Plan planAux = s.plan.seal();
						
						// Iterate along the tasks in the list of tasks - add them to the plan
						while(it_toDoTasks.hasNext()) {							
							currentTask = it_toDoTasks.next();
							planAux.appendMove(neigh);
							newTasksAux.remove(currentTask);
							planAux.appendPickup(currentTask);
						}
						
						// Move to neighbor city in next iteration
						// Don't check for loops (and restart loop finder) since doing a pickup move cannot reach to a loop
						queue.add(new State(planAux, neigh, openTasksAux, newTasksAux, s.freeSpace - toDoTasks.weightSum(),
								new boolean[topology.cities().size()]));
					}
				}
				// Move without picking up any task
				if(!s.visited[neigh.id]) {
					// TODO: Here we should do a clone instead
					Plan planAux = s.plan.seal();
					planAux.appendMove(neigh);
					
					queue.add(new State(planAux, neigh, s.openTasks, s.newTasks, s.freeSpace, s.visited));	
				}
			}			
		}
		
		// All terminal states found
		// Choose best one
		Plan bestPlan = finalPlans.poll();
		Plan currentPlan;
		
		while(!finalPlans.isEmpty()) {
			currentPlan = finalPlans.poll();
			if(currentPlan.totalDistanceUnits() < bestPlan.totalDistanceUnits()) {
				bestPlan = currentPlan;
			}
		}
		
		return bestPlan;
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
