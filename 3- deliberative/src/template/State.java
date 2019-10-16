package template;

import logist.plan.Plan;
import logist.task.TaskSet;
import logist.topology.Topology.City;

public class State {
	public Plan plan;
	public City currentCity;
	public TaskSet openTasks, newTasks;
	public int freeSpace;
	public boolean visited[];
	
	public State(Plan plan, City current, TaskSet openTasks, TaskSet newTasks, int freeSpace, boolean visited[]) {
		this.plan = plan;
		this.openTasks = openTasks;
		this.newTasks = newTasks;
		this.freeSpace = freeSpace;
		this.visited = visited;
	}
}
