package template;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

public class SLS {
	List<Vehicle> vehicles;
	TaskSet tasks;
	Random rn;

	public SLS(List<Vehicle> vehicles, TaskSet tasks) {
		this.vehicles = vehicles;
		this.tasks = tasks;
		this.rn = new Random();
	}

	public List<Plan> build() {
		// Select Initial Solution
		Solution A = SelectInitialSolution();

		//boolean end_condition = false;
		for (int i=0; i<= 1000; i++) {
			A = localChoice(chooseNeighbors(A));
		}
		return A.plans;
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

	private Solution SelectInitialSolution() {
		long time_start = System.currentTimeMillis();

		Solution A = new Solution();

		int indexBiggestVehicle = -1;
		int maxCapacity = 0;

		for (int i = 0; i < vehicles.size(); i++) {
			if (vehicles.get(i).capacity() > maxCapacity) {
				indexBiggestVehicle = i;
				maxCapacity = vehicles.get(i).capacity();
			}
		}

		// Assign all tasks to biggest vehicles
		Plan planBiggestVehicle = this.naivePlan(A, vehicles.get(indexBiggestVehicle));

		// Initialize plan and nextTask for vehicles
		for (int i = 0; i < vehicles.size(); i++) {
			if (i == indexBiggestVehicle) {
				A.plans.add(planBiggestVehicle);
			} else {
				A.plans.add(Plan.EMPTY);
				A.nextTaskVehicle.put(vehicles.get(i), null);
				A.vehicleTaskSet.put(vehicles.get(i), TaskSet.noneOf(tasks));
			}
		}

		long time_end = System.currentTimeMillis();
		long duration = time_end - time_start;
		System.out.println("The plan was generated in " + duration + " milliseconds.");

		return A;
	}

	private Plan naivePlan(Solution A, Vehicle vehicle) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		A.vehicleTaskSet.put(vehicle, tasks);

		Task previousTask = null;
		for (Task task : tasks) {
			if (previousTask != null) {
				// Next task to be delivered
				A.nextTask.put(previousTask, task);
			} else {
				// First task to be delivered
				A.nextTaskVehicle.put(vehicle, task);
			}

			// move: current city => pickup location
			for (City city : current.pathTo(task.pickupCity)) {
				plan.appendMove(city);
			}

			plan.appendPickup(task);

			// move: pickup location => delivery location
			for (City city : task.path()) {
				plan.appendMove(city);
			}

			plan.appendDelivery(task);

			// set current city
			current = task.deliveryCity;

			// Update previous task
			previousTask = task;
		}

		// Last task is followed by null
		A.nextTask.put(previousTask, null);

		return plan;
	}

	private List<Solution> chooseNeighbors(Solution A_old) {
		List<Solution> N = new ArrayList<Solution>();

		// Choose random vehicles from the ones that have a task
		List<Vehicle> usedVehicles = new ArrayList<Vehicle>();
		for (Vehicle v : vehicles) {
			if (A_old.nextTaskVehicle.get(v) != null) {
				usedVehicles.add(v);
			}
		}
		Vehicle v_i = usedVehicles.get(rn.nextInt(usedVehicles.size()));

		// Applying the changing vehicle operator
		for (Vehicle v_j : vehicles) {
			if (!v_j.equals(v_i)) {
				Task t = A_old.nextTaskVehicle.get(v_i);

				if (t.weight <= v_j.capacity()) {
					Solution A = ChangingVehicles(A_old, v_i, v_j);
					N.add(A);
				}

			}
		}

		// Applying changing task order operator
		int length = A_old.vehicleTaskSet.get(v_i).size() - 1;
		if (length >= 2) {
			for (int tIdx1 = 1; tIdx1 < length; tIdx1++) {
				for (int tIdx2 = tIdx1 + 1; tIdx2 <= length; tIdx2++) {
					Solution A = changingTaskOrder(A_old, v_i, tIdx1, tIdx2);
					N.add(A);
				}
			}
		}
		return N;
	}

	private Solution ChangingVehicles(Solution A, Vehicle v1, Vehicle v2) {
		Solution newSolution = A.clone();
		Task t1 = A.nextTaskVehicle.get(v1);
		Task t2 = A.nextTaskVehicle.get(v2);

		newSolution.nextTaskVehicle.put(v1, A.nextTask.get(t1));
		newSolution.nextTask.put(t1, t2);
		newSolution.nextTaskVehicle.put(v2, t1);

		newSolution.updateTime(v1);
		newSolution.updateTime(v2);

		newSolution.taskVehicle.put(t1, v2);
		
		newSolution.updatePlan(v1);
		newSolution.updatePlan(v2);

		return newSolution;
	}

	private Solution changingTaskOrder(Solution A, Vehicle v_i, int tIdx1, int tIdx2) {
		Solution A1 = A.clone();
		
		if(tIdx1 == 29) {
			System.out.println("hi");
		}

		Task tPre1 = A1.nextTaskVehicle.get(v_i);
		Task t1 = A1.nextTask.get(tPre1);
		int count = 1;
		while (count < tIdx1) {
			tPre1 = t1;
			t1 = A1.nextTask.get(t1);
			count++;
		}
		Task tPost1 = A1.nextTask.get(t1);

		Task tPre2 = t1;
		Task t2 = A1.nextTask.get(t1);
		while (count < tIdx2) {
			tPre2 = t2;
			t2 = A1.nextTask.get(t2);
			count++;
		}
		Task tPost2 = A1.nextTask.get(t2);

		// Exchanging two tasks
		if (tPost1.equals(t2)) {
			// The task t2 is delivered immediately after t1
			A1.nextTask.replace(tPre1, t2);
			A1.nextTask.replace(t2, t1);
			A1.nextTask.replace(t1, tPost1);
		} else {
			A1.nextTask.replace(tPre1, t2);
			A1.nextTask.replace(tPre2, t1);
			A1.nextTask.replace(t2, tPost1);
			A1.nextTask.replace(t1, tPost2);
		}

		A1.updateTime(v_i);
		A1.updatePlan(v_i);
		
		return A1;
	}
}
