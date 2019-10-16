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

import template.Mapping;

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
		
		// Create map of tasks with Pickup cities
		Mapping pickUpMapping = new Mapping(tasks, true);
		// Create map of tasks with Delivery cities
		Mapping deliveryMapping = new Mapping(TaskSet.noneOf(tasks),false); 
		
		TaskSet openTasks = pickUpMapping.get(currentCity);
		
		queue.add(new State(new Plan(currentCity), currentCity, openTasks, tasks, vehicle.capacity(), visited));
		
		while(!queue.isEmpty()) {
			// Get current state
			State s = queue.poll();
			Task currentTask;
			
			// 1. Delivery of tasks
			TaskSet toDeliver = deliveryMapping.get(currentCity);
			
			// Check if open tasks have current city as destination
			Iterator<Task> it = toDeliver.iterator();
			while(it.hasNext()) {
				currentTask = it.next();
				s.freeSpace += currentTask.weight;
				s.plan.appendDelivery(currentTask);
				s.openTasks.remove(currentTask);
				
			}
			
			// Remove from the delivery mapping
			deliveryMapping.remove(currentCity);
			
			
			// 2. Check if state is terminal
			// If it's the case, add it to final Plan list
			if(s.newTasks.isEmpty() && s.openTasks.isEmpty()) {
				finalPlans.add(s.plan);
			}else {
				
				// 3. Get all combination of tasks
				List<TaskSet> powerSet = new LinkedList<TaskSet>();
				
				// Get current tasks that can be picked up 
				TaskSet potentialTasks = pickUpMapping.get(currentCity);
				it = potentialTasks.iterator();
				
				// Generate combinations
				for(int size=1; size <= potentialTasks.size(); size++) {
					powerSet.addAll(Utils.combination(potentialTasks, size));
					size ++;
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
								
								// Update mappings
								pickUpMapping.removeTask(currentCity, currentTask);
								deliveryMapping.add(currentTask.deliveryCity, currentTask);
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
