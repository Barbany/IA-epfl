package template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;

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
		A.time = (HashMap<Task, Integer>) this.time.clone();
		A.taskVehicle = (HashMap<Task, Vehicle>) this.taskVehicle.clone(); 
		A.nextTask = (HashMap<Task, Task>) this.nextTask.clone();
		A.nextTaskVehicle = (HashMap<Vehicle, Task>) this.nextTaskVehicle.clone(); 
		A.vehicleTaskSet = (HashMap<Vehicle, TaskSet>) this.vehicleTaskSet.clone();
		
		
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
	
	
	
	
	
	

}
