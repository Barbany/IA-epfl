package template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import logist.plan.Action;
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
	
	public List<Plan> build(){
    	// Select Initial Solution
		Solution A = SelectInitialSolution();
    	
    	boolean end_condition = false;
    	while(!end_condition) {
    		localChoice(chooseNeighbors(A));
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

	private List<Solution> chooseNeighbors(Solution A_old) {
		List<Solution> N = new ArrayList<Solution>();
		
		// Choose random vehicles from the ones that have a task
		List<Vehicle> usedVehicles = new ArrayList<Vehicle>();
		for(Vehicle v: vehicles) {
			if(A_old.nextTaskVehicle.get(v) != null) {
				usedVehicles.add(v);
			}
		}
		Vehicle v_i = usedVehicles.get(rn.nextInt(usedVehicles.size()));
		
		// Applying the changing vehicle operator
		for(Vehicle v_j: vehicles) {
			if(!v_j.equals(v_i)) {
				Task t = A_old.nextTaskVehicle.get(v_j);
				if(t.weight <= v_j.capacity()) {
					Solution A = ChangingVehicles(A_old, v_i, v_j);
					N.add(A);
				}
			}
		}
		
		// Applying changing task order operator
		int length = A_old.vehicleTaskSet.get(v_i).size();
		if(length >= 2) {
			for(int tIdx1 = 1; tIdx1 < length; tIdx1++) {
				for(int tIdx2 = tIdx1 + 1; tIdx2 <= length; tIdx2++) {
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
		
    	return newSolution; 
    }
	
	private Solution changingTaskOrder(Solution A, Vehicle v_i, int tIdx1, int tIdx2) {
		Solution A1 = A.clone();
		
		Task tPre1 = A1.nextTaskVehicle.get(v_i);
		Task t1 = A1.nextTask.get(tPre1);
		int count = 1;
		while(count < tIdx1) {
			tPre1 = t1;
			t1 = A1.nextTask.get(t1);
			count++;
		}
		Task tPost1 = A1.nextTask.get(t1);
		
		Task tPre2 = t1;
		Task t2 = A1.nextTask.get(t1);
		while(count < tIdx2) {
			tPre2 = t2;
			t2 = A1.nextTask.get(t2);
			count++;
		}
		Task tPost2 = A1.nextTask.get(t2);
		
		// Exchanging two tasks
		if(tPost1.equals(t2)) {
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
		return A1;
	}		
}
