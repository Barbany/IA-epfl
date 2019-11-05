package template;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.TaskSet;

/**
 * Stochastic Local Search Algorithm
 * 
 * @author Oriol Barbany & Natalie Bolon
 */
public class SLS {
	List<Vehicle> vehicles;
	TaskSet tasks;
	Random rn;

	/**
	 * Create SLS object. Call build to get the joint optimal plan
	 * 
	 * @param vehicles
	 * @param tasks
	 */
	public SLS(List<Vehicle> vehicles, TaskSet tasks) {
		this.vehicles = vehicles;
		this.tasks = tasks;
		this.rn = new Random();
	}

	/**
	 * Run SLS algorithm
	 * 
	 * @return Optimal plan in form of list of plan objects (one for each vehicle)
	 */
	public List<Plan> build(long timeOutPlan) {
		long timeStart, duration;
		double probability = 0.8;

		// Select Initial Solution
		Solution A = new Solution(vehicles);
		A.initSolutionMultiple(tasks); // for single vehicle solution: A.initSolutionSingle(tasks)
		double bestCost = A.totalCost(vehicles);
		Solution bestSolution = A;

		long maxDuration = 0;
		long totalDuration = 0;

		// Until termination condition met
		for (int i = 0; totalDuration + maxDuration < timeOutPlan*0.9; i++) {
			timeStart = System.currentTimeMillis();
			probability = 0.3 - Math.log(10 / (i + 1)) * 0.075; // logarithmic variation
			//probability = Math.min(0.3 + 0.1*i/75, 0.95); // linear variation

			A = localChoice(chooseNeighbors(A), probability);
			
			if (A.totalCost(vehicles) < bestCost) {
				bestCost = A.totalCost(vehicles);
				bestSolution = A;
			}

			duration = System.currentTimeMillis() - timeStart;
			if (maxDuration < duration) {
				maxDuration = duration;
			}
			totalDuration += duration;
		}

		System.out.println("Best plan cost: " + bestCost);
		return bestSolution.plans;

	}

	/**
	 * Choose one vehicle u.a.r. and perform local operators on this vehicle
	 * 
	 * @param A_old
	 * @return List of neighboring solutions
	 */
	private List<Solution> chooseNeighbors(Solution A_old) {
		List<Solution> N = new ArrayList<Solution>();

		// If there is a single vehicle, perform only permutations among task order
		if (vehicles.size() == 1) {
			N = changeOneVehicle(A_old, vehicles.get(0));
		} else {
			// Choose random vehicles from the ones that have a task
			List<Vehicle> usedVehicles = new ArrayList<Vehicle>();
			for (Vehicle v : vehicles) {
				if (A_old.nextActionVehicle.get(v) != null) {
					usedVehicles.add(v);
				}
			}
			usedVehicles.remove(null);
			Vehicle v_i = usedVehicles.get(rn.nextInt(usedVehicles.size()));

			// Applying the changing vehicle operator
			// Note that we are moving one whole task, so change pickup and delivery actions
			for (Vehicle v_j : vehicles) {
				if (!v_j.equals(v_i)) {
					// This has to be a pickup action since it's the first of a vehicle
					Action a = A_old.nextActionVehicle.get(v_i);
					assert (a.pickup);

					// Check if task fits in empty vehicle (in worst case append pickup and delivery
					// in beginning)
					if (a.task.weight <= v_j.capacity()) {
						Solution A = changingVehicles(A_old, v_i, v_j);
						if (A != null) {
							N.add(A);
							// Applying changing task order operator
							List<Solution> newN = changingOrder(A, v_j);
							for (Solution sol : newN) {
								N.add(sol);
							}
						}
					}
				}
			}
		}
		return N;
	}

	
	/**
	 * Generate neighbor solutions corresponding to all feasible permutations of tasks for a given vehicle
	 * This method is only used when we don't have a fleet but a vehicle
	 * 
	 * @param A_old
	 * @param v_i Vehicle in which we perform the task order modification
	 * @return List of neighboring solutions
	 */
	private List<Solution> changeOneVehicle(Solution A_old, Vehicle v_i) {
		List<Solution> N = new ArrayList<Solution>();

		Action pickup = A_old.nextActionVehicle.get(v_i);
		Action firstAction = A_old.nextActionVehicle.get(v_i);
		assert (pickup.pickup);

		Action delivery;
		Action prePickup = null, preDelivery = null;
		Solution A1;

		// Put pickup and delivery that we want to permute as first and second tasks
		// Then call changingOrder
		while (pickup != null) {
			// Find its correspondent delivery
			if (pickup.pickup) {
				delivery = A_old.nextAction.get(pickup);
				if (delivery.task.equals(pickup.task)) {
					// Delivery of task in pickup is the next action
					A1 = A_old.clone();
					if (!firstAction.equals(pickup)) {
						A1.nextActionVehicle.replace(v_i, pickup);
						A1.nextAction.replace(pickup, delivery);
						A1.nextAction.replace(delivery, firstAction);
						A1.nextAction.replace(prePickup, A_old.nextAction.get(delivery));
					}
					List<Solution> newN = changingOrder(A1, v_i);
					for (Solution sol : newN) {
						N.add(sol);
					}

				} else {
					// Delivery of task in pickup is not next action
					while (delivery != null && !delivery.task.equals(pickup.task)) {
						preDelivery = delivery;
						delivery = A_old.nextAction.get(delivery);
					}

					// Every pickup has to have a delivery (pairs)
					assert (delivery != null);
					A1 = A_old.clone();
					if (!firstAction.equals(pickup)) {
						A1.nextActionVehicle.replace(v_i, pickup);
						A1.nextAction.replace(prePickup, A_old.nextAction.get(pickup));
						A1.nextAction.replace(delivery, firstAction);
					} else {
						A1.nextAction.replace(delivery, A_old.nextAction.get(pickup));
					}
					A1.nextAction.replace(pickup, delivery);
					A1.nextAction.replace(preDelivery, A_old.nextAction.get(delivery));

					List<Solution> newN = changingOrder(A1, v_i);
					for (Solution sol : newN) {
						N.add(sol);
					}
				}
			}
			prePickup = pickup;
			pickup = A_old.nextAction.get(pickup);

		}
		return N;
	}

	/**
	 * Generate neighbors with different task order
	 * 
	 * @param A solution
	 * @param v vehicle in which we perform the order modification
	 */
	private List<Solution> changingOrder(Solution A, Vehicle v) {
		
		List<Solution> solutions = new ArrayList<Solution>();
		Solution A1 = A.clone();
		Solution A11 = A.clone();
		Solution A2;
		Action pick = A.nextActionVehicle.get(v);
		Action drop = A.nextAction.get(pick);
		Action aux = A.nextAction.get(drop);
		Action aux2;
		int capacity = v.capacity();

		if (aux == null) {
			return solutions;
		}

		A11.nextActionVehicle.put(v, aux);
		boolean pickFirst = true;
		// Compute length of the list of pickup and delivery actions
		while (aux != null) {
			// Update capacity
			if (pickFirst) {
				A1.nextActionVehicle.put(v, pick);
				A1.nextAction.put(pick, aux);
				capacity += pick.capacity;
				pickFirst = false;
			} else {
				A1 = A11.clone();
				A1.nextAction.put(aux, pick);
				A1.nextAction.put(pick, A.nextAction.get(aux));
				capacity += aux.capacity;
			}

			int auxCapacity = capacity;
			aux = A.nextAction.get(aux);

			aux2 = pick;
			while (aux2 != null) {
				auxCapacity += aux2.capacity;
				if (auxCapacity < 0) {
					break;
				}
				A2 = A1.clone();
				A2.nextAction.put(aux2, drop);
				A2.nextAction.put(drop, A1.nextAction.get(aux2));
				A2.updatePlan(v);
				solutions.add(A2);

				aux2 = A1.nextAction.get(aux2);
			}
		}
		return solutions;
	}

	/**
	 * Put first task of vehicle 1 to vehicle 2
	 * 
	 * @param A
	 * @param v_i
	 * @param v_j
	 * @return
	 */
	private Solution changingVehicles(Solution A, Vehicle v_i, Vehicle v_j) {
		Solution A1 = A.clone();
		// Get first actions (pickups) for each vehicle
		Action pickup1 = A1.nextActionVehicle.get(v_i);
		assert (pickup1.pickup);
		Action pickup2 = A1.nextActionVehicle.get(v_j);
		assert (pickup2.pickup);

		if (pickup1.task.weight > v_j.capacity()) {
			return null;
		}

		// Find delivery of task first picked up by v1 [pickup1, a, ..., delivery1, ...]
		Action a = A1.nextAction.get(pickup1);
		Action delivery1 = a;
		// If it's immediately delivered, just go to next one (that has to be a pickup)
		if (a.task.equals(pickup1.task)) {
			// delivery1 = a
			a = A1.nextAction.get(a);
			A1.nextActionVehicle.put(v_i, a);
		} else {
			// a has to be a pickup, hence it will be the first one for v1
			A1.nextActionVehicle.put(v_i, a);
			assert (a.pickup);

			// Iterate tasks until we find it
			boolean found = false;
			while (!found) {
				if (A1.nextAction.get(a) == null) {
					throw new NullPointerException("This should not happen");
				}

				if (A1.nextAction.get(a).task.equals(pickup1.task)) {
					// Delivery found: Update initial value
					delivery1 = A1.nextAction.get(a);
					// Link task previous to delivery to the next to delivery
					A1.nextAction.replace(a, A1.nextAction.get(delivery1));
					found = true;
				} else {
					a = A1.nextAction.get(a);
				}
			}
		}
		assert (!delivery1.pickup);

		// Assign pickup and delivery to vehicle 2
		if (pickup2 == null) {
			// Only one way to locate the task
			A1.nextActionVehicle.put(v_j, pickup1);
			A1.nextAction.put(pickup1, delivery1);
			A1.nextAction.put(delivery1, null);
		} else {
			// Can interleave with other pickup-delivery pair
			A1.nextActionVehicle.replace(v_j, pickup1);
			A1.nextAction.replace(pickup1, delivery1);
			A1.nextAction.replace(delivery1, pickup2);
		}

		A1.updatePlan(v_i);
		A1.updatePlan(v_j);

		return A1;
	}

	/**
	 * Select next neighbor to explore
	 * 
	 * @param neighbors. List of Solutions
	 * @param p. Probability of selecting best solution among the neighbors provided
	 * @return Solution
	 */
	private Solution localChoice(List<Solution> neighbors, double p) {
		Solution solution;
		Solution bestSolution = neighbors.get(0);
		double bestCost = neighbors.get(0).totalCost(vehicles);
		double cost;
		// System.out.println("neighbors: " + neighbors.size());

		Iterator<Solution> it_neighbors = neighbors.iterator();
		while (it_neighbors.hasNext()) {
			solution = it_neighbors.next();
			cost = solution.totalCost(vehicles);
			if (cost < bestCost) {
				bestCost = solution.totalCost(vehicles);
				bestSolution = solution;
			}
		}

		if (rn.nextDouble() < p) {
			return bestSolution;
		} else {
			return neighbors.get(rn.nextInt(neighbors.size()));
		}
	}
}
