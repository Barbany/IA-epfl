package template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;

public class Solution {
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
	
	// TODO: copy 
	
	
	
	

}
