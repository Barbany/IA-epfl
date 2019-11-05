package template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import logist.LogistException;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

public class Solution implements Cloneable {
	public List<Plan> plans;
	List<Vehicle> vehicles;
	public HashMap<Action, Vehicle> actionVehicle;
	public HashMap<Action, Action> nextAction;
	public HashMap<Vehicle, Action> nextActionVehicle;

	public Solution(List<Vehicle> vehicles) {
		this.plans = new ArrayList<Plan>();
		this.vehicles = vehicles;
		this.actionVehicle = new HashMap<Action, Vehicle>();
		this.nextAction = new HashMap<Action, Action>();
		this.nextActionVehicle = new HashMap<Vehicle, Action>();
	}

	/**
	 * Initiate all attributes of the solution by putting all the tasks in the
	 * largest vehicle
	 */
	public void initSolutionSingle(TaskSet tasks) {
		int indexBiggestVehicle = -1;
		int maxCapacity = 0;

		for (int i = 0; i < vehicles.size(); i++) {
			if (vehicles.get(i).capacity() > maxCapacity) {
				indexBiggestVehicle = i;
				maxCapacity = vehicles.get(i).capacity();
			}
		}

		// Assign all tasks to biggest vehicles
		Plan planBiggestVehicle = naivePlan(vehicles.get(indexBiggestVehicle), tasks);

		// Initialize plan and nextTask for vehicles
		for (int i = 0; i < vehicles.size(); i++) {
			if (i == indexBiggestVehicle) {
				plans.add(planBiggestVehicle);
			} else {
				plans.add(Plan.EMPTY);
				nextActionVehicle.put(vehicles.get(i), null);
			}
		}
	}

	/**
	 * Initiate tasks distributed sequentially along all possible vehicles
	 */
	public void initSolutionMultiple(TaskSet tasks) {
		// assign tasks to all vehicles with naive plan (pickup_i --> delivery_i)
		List<Integer> capacities = new ArrayList<Integer>();
		Task task;
		Plan plan;

		int numVehicles = vehicles.size();
		List<TaskSet> taskVehicle = new ArrayList<TaskSet>(numVehicles);

		for (int i = 0; i < numVehicles; i++) {
			taskVehicle.add(TaskSet.noneOf(tasks));
			capacities.add(vehicles.get(i).capacity());
		}

		Iterator<Task> it_task = tasks.iterator();
		int j = 0;
		int check = 0;
		while (it_task.hasNext()) {
			task = it_task.next();
			while (capacities.get(j % numVehicles) < task.weight && check < numVehicles) {
				j++;
				check++;
			}
			if (check == numVehicles) {
				throw new LogistException("Non feasible solution");
			}
			check = 0;
			taskVehicle.get(j % numVehicles).add(task);
			j++;
		}

		for (int i = 0; i < numVehicles; i++) {
			if (taskVehicle.get(i).size() > 0) {
				plan = naivePlan(vehicles.get(i), taskVehicle.get(i));
			} else {
				plan = Plan.EMPTY;
			}
			plans.add(plan);
		}
	}

	/**
	 * Put all the tasks in the given vehicle and generate its plan
	 * 
	 * @param vehicle
	 * @param tasks
	 * @return Plan
	 */
	private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);
		Action aux;

		Action lastAction = null;
		for (Task task : tasks) {
			if (lastAction != null) {
				// Next pickups and deliveries
				aux = new Action.Pickup(task);
				nextAction.put(lastAction, aux);
				lastAction = new Action.Delivery(task);
				nextAction.put(aux, lastAction);
			} else {
				// First Action is pickup of first task
				lastAction = new Action.Delivery(task);
				aux = new Action.Pickup(task);
				nextActionVehicle.put(vehicle, aux);
				nextAction.put(aux, lastAction);
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
		}

		// Last Action is followed by null
		nextAction.put(lastAction, null);

		return plan;
	}

	
	/**
	 * Clone current solution
	 */
	public Solution clone() {
		Iterator<Plan> it_plans;

		Solution A = new Solution(vehicles);

		// copy plans
		A.plans = new ArrayList<Plan>();
		it_plans = this.plans.iterator();
		while (it_plans.hasNext()) {
			A.plans.add(it_plans.next());
		}

		// copy time, taksVehicle, nextTask, nextTaskVehicle, vehicleTaskSet
		A.actionVehicle = new HashMap<Action, Vehicle>(this.actionVehicle);
		A.nextAction = new HashMap<Action, Action>(this.nextAction);
		A.nextActionVehicle = new HashMap<Vehicle, Action>(this.nextActionVehicle);

		return A;
	}

	/**
	 * Update plan for vehicle v
	 * 
	 * @param v
	 */
	public void updatePlan(Vehicle v) {
		Action a = nextActionVehicle.get(v);
		Plan plan = new Plan(v.getCurrentCity());

		City currentCity = v.getCurrentCity();

		while (a != null) {

			// move: current city => next location
			for (City city : currentCity.pathTo(a.city)) {
				plan.appendMove(city);
				currentCity = city;
			}

			// Perform action
			if (a.pickup) {
				plan.appendPickup(a.task);
			} else {
				plan.appendDelivery(a.task);
			}

			// Check next action
			a = nextAction.get(a);
		}

		// Set the previously computed plan for current vehicle
		plans.set(v.id(), plan);
	}

	
	public double totalCost(List<Vehicle> vehicles) {
		// cost for the agent
		double cost = 0;

		Iterator<Vehicle> it_vehicle = vehicles.iterator();
		Iterator<Plan> it_plan = this.plans.iterator();
		while (it_vehicle.hasNext() && it_plan.hasNext()) {
			cost += it_plan.next().totalDistance() * it_vehicle.next().costPerKm();
		}
		return cost;
	}

	public int printNumberOfTasks() {
		int n = 0;
		for (Plan p : plans) {
			Iterator<logist.plan.Action> it = p.iterator();
			while (it.hasNext()) {
				if (it.next().getClass().getName().equals("logist.plan.Action$Delivery"))
					n++;
			}
		}
		System.out.println(n);
		return n;
	}

}
