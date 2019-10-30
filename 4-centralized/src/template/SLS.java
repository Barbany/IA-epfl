package template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

public class SLS {
	List<Vehicle> vehicles;
	TaskSet tasks;
	HashMap<Task, Integer> time;
	HashMap<Task, Vehicle> taskVehicle;
	HashMap<Task, Task> nextTask;
	HashMap<Vehicle, Task> nextTaskVehicle;
	
	public SLS(List<Vehicle> vehicles, TaskSet tasks) {
		this.vehicles = vehicles;
		this.tasks = tasks;
		// Initialize time and vehicle arrays
    	this.time = new HashMap<Task, Integer>();
    	this.taskVehicle = new HashMap<Task, Vehicle>();
	}
	
	public List<Plan> build(){
    	// Select Initial Solution
    	List<Plan> a = SelectInitialSolution();
    	
    	
    	boolean end_condition = false;
    	while(!end_condition) {
    		a = localChoice(chooseNeighbors(a));
    	}
    	return a;
    }
	
	/*private boolean constraints() {
    	for (Task task : tasks) {
    		// Constraint 1
    		if(task.equals(nextTask.get(task))) {
    			return false;
    		}
    		// Constraint 3
    		if(time.get(nextTask.get(task)) != time.get(task) + 1) {
    			return false;
    		}
    		// Constraint 5
    		if(!taskVehicle.get(task).equals(taskVehicle.get(nextTask.get(task)))) {
    			return false;
    		}
    		// TODO: Constraint 6
    		if(!nextTask.containsValue(task)) {
    			return false;
    		}
    	}
    	for (Vehicle v: vehicles) {
    		// Constraint 2
    		if(time.get(nextTaskVehicle.get(v)) != 1) {
    			return false;
    		}
    		// Constraint 4
    		if(!v.equals(taskVehicle.get(nextTaskVehicle.get(v)))) {
    			return false;
    		}
    	}
    	
    	return true;
    }*/
	
	private List<Plan> SelectInitialSolution() {
		long time_start = System.currentTimeMillis();
		
		int indexBiggestVehicle = -1;
		int maxCapacity = 0;
		
		for(int i = 0; i < vehicles.size(); i++) {
			if(vehicles.get(i).capacity() > maxCapacity) {
				indexBiggestVehicle = i;
				maxCapacity = vehicles.get(i).capacity();
			}
		}
		
		// Assign all tasks to biggest vehicles
        Plan planBiggestVehicle = this.naivePlan(vehicles.get(indexBiggestVehicle));

        // Initialize plan and nextTask for vehicles
        List<Plan> plans = new ArrayList<Plan>();
        for(int i = 0; i < vehicles.size(); i++) {
			if(i == indexBiggestVehicle) {
				plans.add(planBiggestVehicle);
			} else {
				plans.add(Plan.EMPTY);
				nextTaskVehicle.put(vehicles.get(i), null);
			}
		}
        
        long time_end = System.currentTimeMillis();
        long duration = time_end - time_start;
        System.out.println("The plan was generated in " + duration + " milliseconds.");
        
        return plans;
	}
	
	private Plan naivePlan(Vehicle vehicle) {
        City current = vehicle.getCurrentCity();
        Plan plan = new Plan(current);

        Task previousTask = null;
        for (Task task : tasks) {
        	if (previousTask != null) {
        		// Next task to be delivered
        		nextTask.put(previousTask, task);
        	} else {
        		// First task to be delivered
        		nextTaskVehicle.put(vehicle, task);
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
        nextTask.put(previousTask, null);
        
        return plan;
    }
	
}
