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
	 * @return Optimal plan in form of list of plan objects (one for each vehicle)
	 */
	public List<Plan> build() {
		long time_start = System.currentTimeMillis();
		// Select Initial Solution
		Solution A = new Solution(vehicles);
		A.selectInitialSolution(tasks);

		// Until termination condition met
		// TODO: Check e.g. number of changes or improvement and add it to termination condition
		for (int i=0; i<= 10000; i++) {
			A = localChoice(chooseNeighbors(A));
		}
		
		long time_end = System.currentTimeMillis();
		long duration = time_end - time_start;
		System.out.println("The plan was generated in " + duration + " milliseconds.");
		
		return A.plans;
	}
	
	/**
	 * Choose one vehicle u.a.r. and perform local operators on this vehicle
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

				// Check if task fits in empty vehicle
				if (a.task.weight <= v_j.capacity()) {
					Solution A = changingVehicles(A_old, v_i, v_j);
					N.add(A);
				}
			}
		}

		// Applying changing task order operator
		int length = 0;
		Action a = A_old.nextActionVehicle.get(v_i);
		while(a != null) {
			a = A_old.nextAction.get(a);
			length++;
		}
		
		if (length >= 2) {
			for (int tIdx1 = 1; tIdx1 < length; tIdx1++) {
				for (int tIdx2 = tIdx1 + 1; tIdx2 <= length; tIdx2++) {
					// TODO: Check that it's a valid change according to constraints
					// e.g. cannot deliver before picking up
					Solution A = changingTaskOrder(A_old, v_i, tIdx1, tIdx2);
					N.add(A);
				}
			}
		}
		return N;
	}
	
	/**
	 * Put first task of vehicle 1 to vehicle 2
	 * @param A
	 * @param v1
	 * @param v2
	 * @return
	 */
	private Solution changingVehicles(Solution A, Vehicle v1, Vehicle v2) {		
		Solution A1 = A.clone();
		// Get first actions (pickups) for each vehicle
		Action pickup1 = A1.nextActionVehicle.get(v1);
		Action pickup2 = A1.nextActionVehicle.get(v2);

		// Find delivery of task first picked up by v1
		Action a = A1.nextAction.get(pickup1);
		Action delivery1 = a;
		// If it's immediately delivered, just go to next one (that has to be a pickup)
		if(a.task.equals(pickup1.task)) {
			a = A1.nextAction.get(a);
			A1.nextActionVehicle.replace(v1, a);
			
			// Compute decrease factor in time
			int decrease = 0;
			if(A1.time.get(delivery1).equals(A1.time.get(a))) {
				// Time was [1, 2, 2, ...] so decrease by 1 and get (first two actions deleted) -> [1, ...]
				decrease = 1;
			} else {
				// Time was [1, 2, 3, ...] so decrease by 2 and get (first two actions deleted) -> [1, ...]
				decrease = 2;
			}
			
			// Update time for vehicle 1
			while(a != null) {
				int t = A1.time.get(a);
				A1.time.replace(a, t - decrease);
				a = A1.nextAction.get(a);
			}
		} else {
			// a has to be a pickup, hence it will be the first one for v1
			A1.nextActionVehicle.replace(v1, a);
			
			// Iterate tasks until we find it
			boolean found = false;
			while(!found) {
				if(A1.nextAction.get(a).task.equals(pickup1.task)) {
					// Delivery found: Update initial value
					delivery1 = A1.nextAction.get(a);
					// Link task previous to delivery to the next to delivery
					A1.nextAction.replace(a, A1.nextAction.get(delivery1));
					found = true;
				} else {
					a = A1.nextAction.get(a);
				}
			}
			
			// TODO: Can do function in solution with predicate as parameter e.g. (a) -> A1.time.get(delivery1).equals(A1.time.get(a))
			// Now we have [pickup1, ...(part1)..., delivery1, ...(part2)...]
			// Compute decrease factor in time for part1
			int decrease = 0;
			a = A1.nextAction.get(pickup1);
			if(A1.time.get(pickup1).equals(A1.time.get(a))) {
				// Time was [1, 1, ...] so decrease by 0 and get (first two actions deleted) -> [1, ...]
				decrease = 0;
			} else {
				// Time was [1, 2, ...] so decrease by 1 and get (first two actions deleted) -> [1, ...]
				decrease = 1;
			}
						
			// Update time for vehicle 1, part1
			while(!a.equals(delivery1)) {
				int t = A1.time.get(a);
				A1.time.replace(a, t - decrease);
				a = A1.nextAction.get(a);
			}
			
			// Now we have [pickup1, ...(part1)..., delivery1, ...(part2)...]
			// Compute decrease factor in time for part1
			decrease = 0;
			a = A1.nextAction.get(delivery1);
			if(A1.time.get(delivery1).equals(A1.time.get(a))) {
				// Time was [1, 1, ...] so decrease by 0 and get (first two actions deleted) -> [1, ...]
				decrease = 0;
			} else {
				// Time was [1, 2, ...] so decrease by 1 and get (first two actions deleted) -> [1, ...]
				decrease = 1;
			}
					
			// Update time for vehicle 1, part1
			while(a != null) {
				int t = A1.time.get(a);
				A1.time.replace(a, t - decrease);
				a = A1.nextAction.get(a);
			}
		}
		
		// Assign pickup and delivery to vehicle 2
		if(pickup2 == null) {
			// Only one way to locate the task
			A1.nextActionVehicle.replace(v2, pickup1);
			A1.nextAction.put(pickup1, delivery1);
			A1.nextAction.put(delivery1, null);
		} else {
			// TODO: Think good way to allocate pickup and delivery
			// Can interleave with other pickup-delivery pair
		}
		
		// TODO: Check-proof that A1 is valid according to constraints
		A1.updatePlan(v1);
		A1.updatePlan(v2);

		return A1;
	}

	private Solution localChoice(List<Solution> neighbors) {
		Solution solution;
		Solution bestSolution = neighbors.get(0);
		double bestCost = neighbors.get(0).totalCost(vehicles);
		double cost;
		double p = 0.9;

		Iterator<Solution> it_neighbors = neighbors.iterator();
		while (it_neighbors.hasNext()) {
			solution = it_neighbors.next();
			cost = solution.totalCost(vehicles);
			if ( cost < bestCost) {
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

	private Solution changingTaskOrder(Solution A, Vehicle v_i, int tIdx1, int tIdx2) {
		Solution A1 = A.clone();
		/*
		Task tPre1 = A1.nextActionVehicle.get(v_i);
		Task t1 = A1.nextAction.get(tPre1);
		int count = 1;
		while (count < tIdx1) {
			tPre1 = t1;
			t1 = A1.nextAction.get(t1);
			count++;
		}
		Task tPost1 = A1.nextAction.get(t1);

		Task tPre2 = t1;
		Task t2 = A1.nextAction.get(t1);
		count++;
		while (count < tIdx2) {
			tPre2 = t2;
			t2 = A1.nextAction.get(t2);
			count++;
		}
		Task tPost2 = A1.nextAction.get(t2);

		// Exchanging two tasks
		if (tPost1.equals(t2)) {
			// The task t2 is delivered immediately after t1
			A1.nextAction.replace(tPre1, t2);
			A1.nextAction.replace(t2, t1);
			A1.nextAction.replace(t1, tPost2);
		} else {
			A1.nextAction.replace(tPre1, t2);
			A1.nextAction.replace(tPre2, t1);
			A1.nextAction.replace(t2, tPost1);
			A1.nextAction.replace(t1, tPost2);
		}

		A1.updateTime(v_i);
		A1.updatePlan(v_i);*/
		
		return A1;
	}
}
