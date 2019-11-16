package template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology.City;

/**
 * Solution Class Store sequences of actions and its correspondent plans
 * 
 * @author Oriol Barbany & Natalie Bolon
 */
public class Solution implements Cloneable {
	public List<Plan> plans;
	List<Vehicle> vehicles;
	public HashMap<Action, Action> nextAction;
	public HashMap<Vehicle, Action> nextActionVehicle;

	/**
	 * Create empty Solution with the given fleet
	 * 
	 * @param vehicles
	 */
	public Solution(List<Vehicle> vehicles) {
		this.plans = new ArrayList<Plan>();
		this.vehicles = vehicles;
		this.nextAction = new HashMap<Action, Action>();
		this.nextActionVehicle = new HashMap<Vehicle, Action>();
	}

	/**
	 * Initiate all attributes of the solution by putting all the tasks in the
	 * largest vehicle
	 * 
	 * @param tasks
	 */
	public void initSolutionSingle(Vehicle v, Task task) {
		// Initialize plan and nextTask for vehicles
		for (Vehicle u : vehicles) {
			if (u.equals(v)) {
				plans.add(naivePlan(v, task));
			} else {
				plans.add(Plan.EMPTY);
				nextActionVehicle.put(u, null);
			}
		}
	}

	/**
	 * Put the task in the given vehicle and return its plan
	 * 
	 * @param vehicle
	 * @param task
	 * @return Plan
	 */
	private Plan naivePlan(Vehicle vehicle, Task task) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		// First Action is pickup of first task
		Action lastAction = new Action.Delivery(task);
		Action aux = new Action.Pickup(task);
		;
		nextActionVehicle.put(vehicle, aux);
		nextAction.put(aux, lastAction);
		nextAction.put(lastAction, null);

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

		return plan;
	}

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

	/**
	 * Create final plan with new Task objects
	 * 
	 * @param v
	 */
	public void finalPlan(Vehicle v, HashMap<Integer, Task> int_task) {
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
				plan.appendPickup(int_task.get(a.task.id));
			} else {
				plan.appendDelivery(int_task.get(a.task.id));
			}

			// Check next action
			a = nextAction.get(a);
		}

		// Set the previously computed plan for current vehicle
		plans.set(v.id(), plan);
	}

	/**
	 * Get the total cost, computed from the plan of this Solution Vehicles are
	 * given to weight the cost per kilometer of each one
	 * 
	 * @param vehicles
	 * @return
	 */
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

}
