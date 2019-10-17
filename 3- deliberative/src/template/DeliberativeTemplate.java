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
		
		for(int i=0; i<tasks.size(); i++) {
			// add elements of the TaskSet to the mapping
			System.out.println(tasksArray[i]);
		}
		
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
			
			/* Debugging */
			System.out.println(s.toString());
			try {
				TimeUnit.SECONDS.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}/**/
			
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
				// 3. Pick all possible packets and go to each neighbor
				for(City neigh : s.currentCity.neighbors()) {
					
					CustomPlan planAux = s.plan.clone();
					
					if (s.pickupMapping.containsKey(s.currentCity)) {
						List<Task> availableTasks = s.pickupMapping.get(s.currentCity);
						
						Mapping pickupAux = s.pickupMapping.clone();
						Mapping deliveryAux = s.deliveryMapping.clone();
						
						// Reset cycle detection
						boolean[] visitedAux = new boolean[topology.cities().size()];
						visitedAux[s.currentCity.id] = true;
						
						switch(availableTasks.size()) {
						case 2:
							// If there are two packages:
							Task task1 = availableTasks.get(0);
							Task task2 = availableTasks.get(1);
							
							if(task1.weight + task2.weight <= s.freeSpace) {
								// If we can pick both, we will do it
								planAux.appendPickup(task1);
								planAux.appendPickup(task2);
								
								// Update mappings
								pickupAux.remove(s.currentCity, task1);
								deliveryAux.add(task1.deliveryCity, task1);
								
								pickupAux.remove(s.currentCity, task2);
								deliveryAux.add(task2.deliveryCity, task2);
								
								planAux.appendMove(neigh);
								queue.add(new State(planAux, neigh, pickupAux, deliveryAux, s.freeSpace - task1.weight - task2.weight,
										visitedAux));
							} else {
								// Otherwise, we create two neighbors, one by picking each package
								planAux.appendPickup(task1);
								
								pickupAux.remove(s.currentCity, task1);
								deliveryAux.add(task1.deliveryCity, task1);
								
								// First neighbor
								planAux.appendMove(neigh);
								queue.add(new State(planAux, neigh, pickupAux, deliveryAux, s.freeSpace - task1.weight,
										visitedAux));
								
								// Reset auxiliary mappings and plan
								pickupAux = s.pickupMapping.clone();
								deliveryAux = s.deliveryMapping.clone();
								planAux = s.plan.clone();
								
								planAux.appendPickup(task2);
								
								pickupAux.remove(s.currentCity, task2);
								deliveryAux.add(task2.deliveryCity, task2);
								
								// Second neighbor
								planAux.appendMove(neigh);
								queue.add(new State(planAux, neigh, pickupAux, deliveryAux, s.freeSpace - task2.weight,
										visitedAux));
							}
							break;
						case 1:
							// If there is one, pick it
							Task task = availableTasks.get(0);
							
							planAux.appendPickup(task);
							
							pickupAux.remove(s.currentCity, task);
							deliveryAux.add(task.deliveryCity, task);
							
							// First neighbor
							planAux.appendMove(neigh);
							queue.add(new State(planAux, neigh, pickupAux, deliveryAux, s.freeSpace - task.weight,
									visitedAux));
							break;
						default:
							throw new IllegalArgumentException("Case with 3 pickup options not considered");
						}
					} else {
						// Otherwise, go to a neighbor
						if(!s.visited[neigh.id]) {
							s.visited[neigh.id] = true;
							
							planAux = s.plan.clone();
							planAux.appendMove(neigh);
							
							queue.add(new State(planAux, neigh, s.pickupMapping.clone(), s.deliveryMapping.clone(),
									s.freeSpace, s.visited));
						}
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
