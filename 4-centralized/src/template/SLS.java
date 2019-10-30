package template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import logist.plan.Action;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

public class SLS {
	List<Vehicle> vehicles;
	TaskSet tasks;
	
	public SLS(List<Vehicle> vehicles, TaskSet tasks) {
		this.vehicles = vehicles;
		this.tasks = tasks;
	}
	
	public List<Plan> build(){
    	// Select Initial Solution
		Solution A = SelectInitialSolution();
    	
    	boolean end_condition = false;
    	while(!end_condition) {
    		localChoice(chooseNeighbors());
    	}
    	return A.plans;
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
	
	private Solution SelectInitialSolution() {
		long time_start = System.currentTimeMillis();
		
		Solution A = new Solution(); 
		
		int indexBiggestVehicle = -1;
		int maxCapacity = 0;
		
		for(int i = 0; i < vehicles.size(); i++) {
			if(vehicles.get(i).capacity() > maxCapacity) {
				indexBiggestVehicle = i;
				maxCapacity = vehicles.get(i).capacity();
			}
		}
		
		// Assign all tasks to biggest vehicles
        Plan planBiggestVehicle = this.naivePlan(A, vehicles.get(indexBiggestVehicle));

        // Initialize plan and nextTask for vehicles
        for(int i = 0; i < vehicles.size(); i++) {
			if(i == indexBiggestVehicle) {
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

	
	
	
	private Solution ChangingVehicles(Solution A, Vehicle v1, Vehicle v2) {
		Solution newSolution = A.clone(); 
		Task t1 = A.nextTask.get(v1);
		Task t2 = A.nextTask.get(v2);
		
		newSolution.nextTaskVehicle.put(v1, A.nextTask.get(t1));
		newSolution.nextTask.put(t1, t2);
		newSolution.nextTaskVehicle.put(v2, t1);
		
		newSolution.updateTime(v1);
		newSolution.updateTime(v2);
		
		newSolution.taskVehicle.put(t1, v2);
		
    	return newSolution; 
    }
	
	
	
	
	private double costVehicle(Plan plan, Vehicle vehicle) {
    	// cost for a single vehicle's plan
    	double cost = plan.totalDistance() * vehicle.costPerKm();
    	return cost; 
    }
    
    private double totalCost(Solution A) {
    	// cost for the agent
    	double cost = 0; 
    	
    	Iterator<Vehicle> it_vehicle = vehicles.iterator(); 
    	Iterator<Plan>    it_plan = A.plans.iterator();
    	while(it_vehicle.hasNext() && it_plan.hasNext()) {
    		cost += costVehicle(it_plan.next(), it_vehicle.next());
    	}
    	
    	return cost; 
    }
		
}
