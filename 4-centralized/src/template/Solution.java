package template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

public class Solution implements Cloneable{
	public List<Plan> plans; 
	public HashMap<Task, Integer> time;
	public HashMap<Task, Vehicle> taskVehicle;
	public HashMap<Task, Task> nextTask;
	public HashMap<Vehicle, Task> nextTaskVehicle;
	public HashMap<Vehicle, TaskSet> vehicleTaskSet;
	
	
	public Solution() {
		this.plans = new ArrayList<Plan>();
		this.time = new HashMap<Task, Integer>();
		this.taskVehicle = new HashMap<Task, Vehicle>();
		this.nextTask = new HashMap<Task, Task>();
		this.nextTaskVehicle = new HashMap<Vehicle, Task>();
		this.vehicleTaskSet = new HashMap<Vehicle, TaskSet>();
	}
	
	public Solution clone() {
		Iterator<Plan> it_plans; 
		
		Solution A = new Solution(); 
		
		// copy plans
		A.plans = new ArrayList<Plan>(); 
		it_plans = this.plans.iterator();
		while(it_plans.hasNext()) {
			A.plans.add(it_plans.next());
		}
		
		// copy time, taksVehicle, nextTask, nextTaskVehicle, vehicleTaskSet
		A.time = new HashMap<Task, Integer>(this.time);
		A.taskVehicle = new HashMap<Task, Vehicle>(this.taskVehicle); 
		A.nextTask = new HashMap<Task, Task>(this.nextTask);
		A.nextTaskVehicle = new HashMap<Vehicle, Task>(this.nextTaskVehicle); 
		A.vehicleTaskSet = new HashMap<Vehicle, TaskSet>(this.vehicleTaskSet);
		
		
		return A;
		
	}
	
	public void updateTime(Vehicle v1) {
		// Update time and vehicleTasks
		TaskSet vehicleTasks = TaskSet.noneOf(this.vehicleTaskSet.get(v1));
		
		int time = 1; 
		Task t; 
		t = this.nextTaskVehicle.get(v1);
		if (t!= null) {
			vehicleTasks.add(t);
			this.time.put(t, time);
			time += 1; 
			while(this.nextTask.get(t) != null) {
				t = this.nextTask.get(t); 
				
				vehicleTasks.add(t);
				this.time.put(t, time);
				time += 1;
			}
			
		}
		this.vehicleTaskSet.put(v1, vehicleTasks);		
	}
	
	public void updatePlan(Vehicle v) {
		Task task = nextTaskVehicle.get(v);
		Plan plan = new Plan(v.getCurrentCity());
		
		while (task != null) {

			// move: current city => pickup location
			for (City city : v.getCurrentCity().pathTo(task.pickupCity)) {
				plan.appendMove(city);
			}

			plan.appendPickup(task);

			// move: pickup location => delivery location
			for (City city : task.path()) {
				plan.appendMove(city);
			}

			plan.appendDelivery(task);

			// Update previous task
			task = nextTask.get(task);
		}
		if (v.getCurrentCity().name == "London") {
			System.out.println("here");
		}
		plans.set(v.id(), plan);
	}
	
	public double totalCost(List<Vehicle> vehicles) {
    	// cost for the agent
    	double cost = 0;
    	
    	Iterator<Vehicle> it_vehicle = vehicles.iterator(); 
    	Iterator<Plan>    it_plan = this.plans.iterator();
    	while(it_vehicle.hasNext() && it_plan.hasNext()) {
    		cost += it_plan.next().totalDistance() * it_vehicle.next().costPerKm();
    	}
    	return cost; 
    }

}
