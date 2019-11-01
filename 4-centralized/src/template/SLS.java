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
	public List<Plan> build() {
		long time_start = System.currentTimeMillis();
		// Select Initial Solution
		Solution A = new Solution(vehicles);
		A.selectInitialSolution(tasks);
		double bestCost = A.totalCost(vehicles);
		Solution bestSolution = A;
		double probability = 0.1;
		int maxIterations = 5000;
		List<Object> costEvolution = new ArrayList<Object>();
		List<Object> bestEvolution = new ArrayList<Object>();
		
		costEvolution.add(bestCost);
		bestEvolution.add(bestCost);

		// Until termination condition met
		// TODO: Run for all possible runtime
		for (int i = 0; i <= maxIterations; i++) {
			//probability = 0.9 - Math.log(1/(i+1))*0.1; 
			probability = 0.99;
			A = localChoice(chooseNeighbors(A), probability);
			costEvolution.add(A.totalCost(vehicles));
			if (A.totalCost(vehicles) < bestCost) {
				bestCost = A.totalCost(vehicles);
				bestSolution = A; 
			}
			bestEvolution.add(bestCost);
		}
	
		System.out.println("cost_99 = " + costEvolution +";");
		System.out.println("cost_99_best = " + bestEvolution+";");
		

		long time_end = System.currentTimeMillis();
		long duration = time_end - time_start;
		System.out.println("The plan was generated in " + duration + " milliseconds.");
		System.out.println("Best plan cost: " + bestCost);

		return bestSolution.plans;
	}

	/**
	 * Choose one vehicle u.a.r. and perform local operators on this vehicle
	 * 
	 * @param A_old
	 * @return
	 */
	private List<Solution> chooseNeighbors(Solution A_old) {
		List<Solution> N = new ArrayList<Solution>();

		// Choose random vehicles from the ones that have a task
		List<Vehicle> usedVehicles = new ArrayList<Vehicle>();
		for (Vehicle v : vehicles) {
			if (A_old.nextActionVehicle.get(v) != null) {
				usedVehicles.add(v);
			}
		}
		Vehicle v_i = usedVehicles.get(rn.nextInt(usedVehicles.size()));

		// Applying the changing vehicle operator
		// Note that we are moving one whole task, so change pickup and delivery actions
		for (Vehicle v_j : vehicles) {
			if (!v_j.equals(v_i)) {
				// This has to be a pickup action since it's the first of a vehicle
				Action a = A_old.nextActionVehicle.get(v_i);
				assert(a.pickup);

				// Check if task fits in empty vehicle (in worst case append pickup and delivery in beginning)
				if (a.task.weight <= v_j.capacity()) {
					Solution A = changingVehicles(A_old, v_i, v_j);
					if(A != null) {
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
		
		if(aux == null) {
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
				//int i = A2.printNumberOfTasks();
				A2.updatePlan(v);
				//if (i!= A2.printNumberOfTasks()) {
				//	System.out.println("HERE PROBLEM!");
				//}
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
		assert(pickup1.pickup);
		Action pickup2 = A1.nextActionVehicle.get(v_j);
		assert(pickup2.pickup);
		
		if(pickup1.task.weight > v_j.capacity()) {
			return null;
		}

		// Find delivery of task first picked up by v1 [pickup1, a, ..., delivery1, ...]
		Action a = A1.nextAction.get(pickup1);
		Action delivery1 = a;
		// If it's immediately delivered, just go to next one (that has to be a pickup)
		if (a.task.equals(pickup1.task)) {
			// delivery1 = a
			a = A1.nextAction.get(a);
			A1.nextActionVehicle.replace(v_i, a);
		} else {
			// a has to be a pickup, hence it will be the first one for v1
			A1.nextActionVehicle.replace(v_i, a);
			assert(a.pickup);

			// Iterate tasks until we find it
			boolean found = false;
			while (!found) {
				if(A1.nextAction.get(a) == null) {
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
		assert(!delivery1.pickup);

		// Assign pickup and delivery to vehicle 2
		if (pickup2 == null) {
			// Only one way to locate the task
			A1.nextActionVehicle.replace(v_j, pickup1);
			A1.nextAction.put(pickup1, delivery1);
			A1.nextAction.put(delivery1, null);
		} else {
			// TODO: Think good way to allocate pickup and delivery
			// Can interleave with other pickup-delivery pair
			A1.nextActionVehicle.replace(v_j, pickup1);
			A1.nextAction.replace(pickup1, delivery1);
			A1.nextAction.replace(delivery1, pickup2);
		}

		// TODO: Check-proof that A1 is valid according to constraints
		A1.updatePlan(v_i);
		A1.updatePlan(v_j);
		

		return A1;
	}

	private Solution localChoice(List<Solution> neighbors, double p) {
		Solution solution;
		Solution bestSolution = neighbors.get(0);
		double bestCost = neighbors.get(0).totalCost(vehicles);
		double cost;

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
