package template;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

public class PlanBuilderBFS {
	
	LinkedList<State> queue;
	HashMap<String, Double> visited;

	public PlanBuilderBFS(Vehicle vehicle, TaskSet tasks) {
		
		queue  = new LinkedList<State>(); 
		visited = new HashMap<String, Double>();
		
		queue.add(new State(new CustomPlan(vehicle.getCurrentCity()), vehicle.getCurrentCity(),
				new Mapping(tasks, true), new Mapping(vehicle.getCurrentTasks(), false), vehicle.capacity() - vehicle.getCurrentTasks().weightSum()));
		
		
	}
	
	public Plan BFSPlan() {
		
		// List of final plans
		LinkedList<CustomPlan> finalPlans = new LinkedList<CustomPlan>();
		
		System.out.println("Start BFS");
		

		while (!queue.isEmpty()) {
			// Get current state
			State s = queue.poll();
			
			s.deliverTasks();

			// 2. Check if state is terminal
			// If it's the case, add it to final Plan list
			if (s.deliveryMapping.isEmpty() && s.pickupMapping.isEmpty()) {
				finalPlans.add(s.plan);
			} else {
				// 3. Pickup of tasks
				pickupTasks(s);
			}
		}

		System.out.println("Finished BFS");
		
		// All terminal states found
		// Choose optimal one
		CustomPlan bestPlan = finalPlans.poll();
		CustomPlan currentPlan;

		while (!finalPlans.isEmpty()) {
			currentPlan = finalPlans.poll();
			if (currentPlan.totalDistanceUnits() < bestPlan.totalDistanceUnits()) {
				bestPlan = currentPlan;
			}
		}
		System.out.println("Distance of the plan: "+ bestPlan.totalDistance());
		return bestPlan.asPlan();
	}
	
	/**
	 * Auxiliary functions to pickup tasks at the current state s
	 * This contemplates all possible (feasible) combinations of neighbors and tasks
	 * E.g. if we have 2 possible tasks at one location, for each neighbor
	 * evaluate the state for {No pickup, Pickup of 1, Pickup of 2, Pickup of 1&2}
	 * @param s
	 */
	private void pickupTasks(State s) {
		for (City neigh : s.currentCity.neighbors()) {
			if (s.pickupMapping.containsKey(s.currentCity)) {						
				List<Task> availableTasks = s.pickupMapping.get(s.currentCity);
				List<List<Task>> powerSet = new LinkedList<List<Task>>();

				// Get all possible combinations of tasks
				for (int i = 1; i <= availableTasks.size(); i++) {
					powerSet.addAll(Utils.combination(availableTasks, i));
				}

				// Iterate along combinations of tasks
				Iterator<List<Task>> it_listTask = powerSet.iterator();

				while (it_listTask.hasNext()) {
					// Clone mappings for each case
					CustomPlan planAux = s.plan.clone();
					Mapping pickupAux = s.pickupMapping.clone();
					Mapping deliveryAux = s.deliveryMapping.clone();

					List<Task> toDoTasks = it_listTask.next();
					Iterator<Task> it_task = toDoTasks.iterator();

					// Check feasibility of the combination
					int weightSum = 0;
					while (it_task.hasNext()) {
						weightSum += it_task.next().weight;
					}

					// If feasible, add to the plan
					if (weightSum <= s.freeSpace) {
						it_task = toDoTasks.iterator();
						while (it_task.hasNext()) {
							Task currentTask = it_task.next();

							planAux.appendPickup(currentTask);
							pickupAux.removeTask(s.currentCity, currentTask);
							deliveryAux.add(currentTask.deliveryCity, currentTask);
						}
					}
					planAux.appendMove(neigh);
					
					appendToQueue(new State(planAux, neigh, pickupAux, deliveryAux, s.freeSpace - weightSum));
				}
			}
			
			// No task
			CustomPlan planAux = s.plan.clone();
			Mapping pickupAux = s.pickupMapping.clone();
			Mapping deliveryAux = s.deliveryMapping.clone();
			planAux.appendMove(neigh);
			
			appendToQueue(new State(planAux, neigh, pickupAux, deliveryAux, s.freeSpace));
		}
	}
	
	/**
	 * Append State s to the queue
	 * If state has already been visited, only consider it if it has better cost than previous one(s)
	 * @param s
	 */
	private void appendToQueue(State s) {
		if (visited.containsKey(s.hash)) {
			// Only visit it if current plan improves others in this state
			if(visited.get(s.hash) > s.plan.totalDistance()) {
				visited.replace(s.hash, new Double(s.plan.totalDistance()));
				queue.add(s);
			}
		} else {
			visited.put(s.hash, new Double(s.plan.totalDistance()));
			queue.add(s);
			
		}
	}
	

}
