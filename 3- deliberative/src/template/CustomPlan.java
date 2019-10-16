package template;
import logist.plan.Action;
import logist.plan.Plan;
import logist.topology.Topology.City;

import java.util.Iterator;
import java.util.List;

public class CustomPlan extends Plan{

	public CustomPlan(City initialCity, Action[] actions) {
		super(initialCity, actions);
		// TODO Auto-generated constructor stub
	}
	
	public CustomPlan(City initialCity, List<Action> actions) {
		super(initialCity, actions);
		// TODO Auto-generated constructor stub
	}
	
	private CustomPlan clone() {
		CustomPlan clonePlan;
		Iterator<Action> it = this.iterator();
		Action firstAction = it.next();
		
	}

}
