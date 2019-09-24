package main;

import java.awt.Color;
import java.util.ArrayList;

import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.gui.Value2DDisplay;
import uchicago.src.sim.util.SimUtilities;

/**
 * Class that implements the simulation model for the rabbits grass
 * simulation.  This is the first class which needs to be setup in
 * order to run Repast simulation. It manages the entire RePast
 * environment and the simulation.
 *
 * @author 
 */


public class RabbitsGrassSimulationModel extends SimModelImpl {		

	private static final int GRIDSIZE = 20;
	private static final int NUMINITRABBITS = 5;
	private static final int NUMINITGRASS = 10;
	private static final int GRASSGROWTHRATE = 50;
	private static final int BIRTHTHRESHOLD = 15;
	
	
	
	private int GridSize = GRIDSIZE;
	private int NumInitRabbits = NUMINITRABBITS; 
	private int NumInitGrass = NUMINITGRASS; 
	private int GrassGrowthRate = GRASSGROWTHRATE;
	private int BirthThreshold = BIRTHTHRESHOLD; 
	
	private Schedule schedule;
	
	private RabbitsGrassSimulationSpace space; 
	
	 private ArrayList<RabbitsGrassSimulationAgent> rabbitList;
	
	private DisplaySurface displaySurf;
	
	
	
	public static void main(String[] args) {
			
			System.out.println("Rabbit skeleton");

			SimInit init = new SimInit();
			RabbitsGrassSimulationModel model = new RabbitsGrassSimulationModel();
			// Do "not" modify the following lines of parsing arguments
			if (args.length == 0) // by default, you don't use parameter file nor batch mode 
				init.loadModel(model, "", false);
			else
				init.loadModel(model, args[0], Boolean.parseBoolean(args[1]));
			
		}

	public void begin() {
		buildModel();
	    buildSchedule();
	    buildDisplay();
	    
	    displaySurf.display();
		
	}
	
	public void buildModel(){
		System.out.println("Running BuildModel");
		space = new RabbitsGrassSimulationSpace(GridSize);
		space.spreadGrass(NumInitGrass);
		
		for(int i = 0; i < NumInitRabbits; i++){
		      addNewRabbit();
		    }
	
		for(int i = 0; i < rabbitList.size(); i++){
		      RabbitsGrassSimulationAgent cda = rabbitList.get(i);
		      cda.report();
		    }
	}
	
	private void addNewRabbit(){
		RabbitsGrassSimulationAgent rabbit = new RabbitsGrassSimulationAgent(BirthThreshold);
	    rabbitList.add(rabbit);
	    space.addRabbit(rabbit);
	  }
	

	public void buildSchedule(){
		System.out.println("Running BuildSchedule");
	}

	public void buildDisplay(){
		System.out.println("Running BuildDisplay");
		
		ColorMap map = new ColorMap();
		
		map.mapColor(1, Color.green);
	    
	    map.mapColor(0, Color.black);

	    Value2DDisplay displayEnergy = 
	        new Value2DDisplay(space.getCurrentGrassSpace(), map);
	    
	    Object2DDisplay displayRabbits = new Object2DDisplay(space.getCurrentRabbitSpace());
	    displayRabbits.setObjectList(rabbitList);

	    displaySurf.addDisplayable(displayEnergy, "Grass");
	    displaySurf.addDisplayable(displayRabbits, "Rabbits");
		
	}

	public String[] getInitParam() {
		// TODO Auto-generated method stub
		// Parameters to be set by users via the Repast UI slider bar
		// Do "not" modify the parameters names provided in the skeleton code, you can add more if you want 
		String[] params = { "GridSize", "NumInitRabbits", "NumInitGrass", "GrassGrowthRate", "BirthThreshold"};
		return params;
	}

	public String getName() {
		return "Rabbits Grass Model";
	}

	public Schedule getSchedule() {
		// TODO Auto-generated method stub
		return schedule;
	}

	public void setup() {
		System.out.println("Running setup");
	    space = null;
	    ArrayList<RabbitsGrassSimulationAgent> agentList = new ArrayList<RabbitsGrassSimulationAgent> ();
	    schedule = new Schedule(1);
	    
	    if (displaySurf != null){
	        displaySurf.dispose();
	      }
	      displaySurf = null;

	      displaySurf = new DisplaySurface(this, "Rabbits Grass model W1");

	      registerDisplaySurface("Rabbits Grass model W1", displaySurf);
	      

		
	}

	public int getGridSize() {
		return GridSize;
	}

	public void setGridSize(int gridSize) {
		GridSize = gridSize;
	}

	public int getNumInitRabbits() {
		return NumInitRabbits;
	}

	public void setNumInitRabbits(int numInitRabbits) {
		NumInitRabbits = numInitRabbits;
	}

	public int getNumInitGrass() {
		return NumInitGrass;
	}

	public void setNumInitGrass(int numInitGrass) {
		NumInitGrass = numInitGrass;
	}

	public int getGrassGrowthRate() {
		return GrassGrowthRate;
	}

	public void setGrassGrowthRate(int grassGrowthRate) {
		GrassGrowthRate = grassGrowthRate;
	}

	public int getBirthThreshold() {
		return BirthThreshold;
	}

	public void setBirthThreshold(int birthThreshold) {
		BirthThreshold = birthThreshold;
	}

	public void setSchedule(Schedule schedule) {
		this.schedule = schedule;
	}
}
