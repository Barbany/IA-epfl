package template;

import template.Utils;
import template.State;
import template.Mapping;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

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
		
		// Handle TaskSet in Array form in order to more rapidly access Tasks
		// We won't modify this but only access to its elements
		Task[] tasksArray = new Task[tasks.size()];
		tasks.toArray(tasksArray);
		
		LinkedList<State> queue = new LinkedList<State>();
		LinkedList<CustomPlan> finalPlans = new LinkedList<CustomPlan>();
		
		visited[currentCity.id] = true;
		
		queue.add(new State(new CustomPlan(currentCity), currentCity, new Mapping(tasksArray, tasks.size()), new Mapping(),
				vehicle.capacity(), visited));
		
		System.out.println("Start BST");
		
		// Auxiliary variables that will be used
		Iterator<Task> it;
		Task currentTask;
		
		while(!queue.isEmpty()) {
			// Get current state
			State s = queue.poll();
			
			/* Debugging
			System.out.println(s.toString());
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}*/
			
			// 1. Delivery of tasks
			if(s.deliveryMapping.containsKey(s.currentCity)) {
				List<Task> toDeliver = s.deliveryMapping.get(s.currentCity);
				
				// Check if open tasks have current city as destination
				it = toDeliver.iterator();
				while(it.hasNext()) {
					currentTask = it.next();
					s.freeSpace += currentTask.weight;
					s.plan.appendDelivery(currentTask);
					s.pickupMapping.remove(s.currentCity, currentTask);
					
				}
				
				// Remove from the delivery mapping
				s.deliveryMapping.remove(s.currentCity);
			}
			
			
			// 2. Check if state is terminal
			// If it's the case, add it to final Plan list
			if(s.deliveryMapping.isEmpty() && s.pickupMapping.isEmpty()) {
				finalPlans.add(s.plan);
			}else {
				
				// 3. Get all combination of tasks
				List<List<Task>> powerSet = new LinkedList<List<Task>>();
				
				// Get current tasks that can be picked up 
				if (s.pickupMapping.containsKey(s.currentCity)) {
					List<Task> potentialTasks = s.pickupMapping.get(s.currentCity);
					it = potentialTasks.iterator();

					// Generate combinations
				    for (int i = 1; i <= potentialTasks.size(); i++) {
				    	powerSet.addAll(Utils.combination(potentialTasks, i));	
				    }	
				}
				// Else, powerSet is empty
				
				// Check all combinations of neighbor cities and possible taken tasks
				for(City neigh : s.currentCity.neighbors()) {
					// Check all the possibilities involving at least one packet pickup
					Iterator<List<Task>> it_tasks = powerSet.iterator();
					
					// Iterate along the list of lists of tasks
					while(it_tasks.hasNext()) {
						List<Task> toDoTasks = it_tasks.next();
						
						// If feasible taskSet
						int weightSum = 0;
						it = toDoTasks.iterator();
						while(it.hasNext()) {
							weightSum += it.next().weight;
						}
						
						if (weightSum <= s.freeSpace) {							
							Mapping pickupAux = s.pickupMapping.clone();
							Mapping deliveryAux = s.deliveryMapping.clone();
							CustomPlan planAux = s.plan.clone();
							
							planAux.appendMove(neigh);
							
							// Iterate along the tasks in the list of tasks - add them to the plan
							it = toDoTasks.iterator();
							while(it.hasNext()) {							
								currentTask = it.next();
								
								planAux.appendPickup(currentTask);
								pickupAux.remove(s.currentCity, currentTask);
								deliveryAux.add(currentTask.deliveryCity, currentTask);
							}
							
							// Move to neighbor city in next iteration
							// Don't check for loops (and restart loop finder) since doing a pickup move cannot reach to a loop
							queue.add(new State(planAux, neigh, pickupAux, deliveryAux, s.freeSpace - weightSum,
									new boolean[topology.cities().size()]));
						}
					}
					// Move without picking up any task
					if(!s.visited[neigh.id]) {
						CustomPlan planAux = s.plan.clone();
						planAux.appendMove(neigh);
						
						queue.add(new State(planAux, neigh, s.pickupMapping.clone(), s.deliveryMapping.clone(),
								s.freeSpace, s.visited));	
					}
				}	
				
				
			}
		
					
		}
		
		System.out.println("Finished BST");
		
		// All terminal states found
		// Choose best one
		CustomPlan bestPlan = finalPlans.poll();
		CustomPlan currentPlan;
		
		while(!finalPlans.isEmpty()) {
			currentPlan = finalPlans.poll();
			if(currentPlan.totalDistanceUnits() < bestPlan.totalDistanceUnits()) {
				bestPlan = currentPlan;
			}
		}
		
		return bestPlan.asPlan();
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
