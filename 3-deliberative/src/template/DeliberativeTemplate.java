package template;

import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * An optimal planner for one vehicle.
 */
@SuppressWarnings("unused")
public class DeliberativeTemplate implements DeliberativeBehavior {

	enum Algorithm {
		BFS, ASTAR
	}

	/* Environment */
	Topology topology;
	TaskDistribution td;

	/* the properties of the agent */
	Agent agent;
	int capacity;

	/* the planning class */
	Algorithm algorithm;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		this.topology = topology;
		this.td = td;
		this.agent = agent;

		// initialize the planner
		int capacity = agent.vehicles().get(0).capacity();
		String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");

		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());
	}

	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		Plan plan;
		long startTime; 
		long endTime; 

		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			startTime = System.currentTimeMillis();
			plan = new PlanBuilder(vehicle, tasks).ASTARPlan(); 
			endTime = System.currentTimeMillis();
			System.out.println("Plan A*: "+ plan);
			break;
			
		case BFS:
			startTime = System.currentTimeMillis();
			plan = new PlanBuilderBFS(vehicle, tasks).BFSPlan();
			endTime = System.currentTimeMillis();
			System.out.println("Plan BFS: "+ plan);
			break;
		default:
			throw new AssertionError("Should not happen.");
		}
		System.out.println("Time required " + (endTime-startTime)/1000 + " s with " + algorithm);
		return plan;
	}

	@Override
	public void planCancelled(TaskSet carriedTasks) {
		if (!carriedTasks.isEmpty()) {
			System.out.println("Recomputing new plan with pending tasks");
		} else {
			System.out.println("Recomputing new plan");
		}
	}
}
