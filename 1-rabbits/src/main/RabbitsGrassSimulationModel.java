package main;

import java.awt.Color;
import java.util.ArrayList;

import uchicago.src.sim.analysis.BinDataSource;
import uchicago.src.sim.analysis.DataSource;
import uchicago.src.sim.analysis.OpenHistogram;
import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.analysis.Sequence;
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
 * Class that implements the simulation model for the rabbits grass simulation.
 * This is the first class which needs to be setup in order to run Repast
 * simulation. It manages the entire RePast environment and the simulation.
 *
 * @author Oriol Barbany & Natalie Bolon
 */

public class RabbitsGrassSimulationModel extends SimModelImpl {

	private static final int GRIDSIZE = 20;
	private static final int NUMINITRABBITS = 10;
	private static final int NUMINITGRASS = 10;
	private static final int GRASSGROWTHRATE = 10;
	private static final int BIRTHTHRESHOLD = 15;
	private static final int MAXENERGY = 20;
	private static final int MAXGRASSENERGY = 10;

	private int GridSize = GRIDSIZE;
	private int NumInitRabbits = NUMINITRABBITS;
	private int NumInitGrass = NUMINITGRASS;
	private int GrassGrowthRate = GRASSGROWTHRATE;
	private int BirthThreshold = BIRTHTHRESHOLD;
	private int MaxEnergy = MAXENERGY;
	private int MaxGrassEnergy = MAXGRASSENERGY; 

	private Schedule schedule;

	private RabbitsGrassSimulationSpace space;

	private ArrayList<RabbitsGrassSimulationAgent> rabbitList;

	private DisplaySurface displaySurf;

	private OpenSequenceGraph amountOfEnergyInSpace;
	private OpenHistogram rabbitEnergyDistribution;

	class energyInSpace implements DataSource, Sequence {

		public Object execute() {
			return new Double(getSValue());
		}

		public double getSValue() {
			return (double) space.getTotalGrassEnergy();
		}
	}
	
	class rabbitsInSpace implements DataSource, Sequence {

		public Object execute() {
			return new Double(getSValue());
		}

		public double getSValue() {
			return (double) space.getTotalRabbitsEnergy();
		}
	}

	class rabbitEnergy implements BinDataSource {
		public double getBinValue(Object o) {
			RabbitsGrassSimulationAgent cda = (RabbitsGrassSimulationAgent) o;
			return (double) cda.getEnergy();
		}
	}

	public static void main(String[] args) {

		System.out.println("Rabbit Grass Simulation Model");

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
		amountOfEnergyInSpace.display();
		rabbitEnergyDistribution.display();
	}

	public void buildModel() {
		System.out.println("Running BuildModel");
		space = new RabbitsGrassSimulationSpace(GridSize);
		space.spreadGrass(NumInitGrass, MaxGrassEnergy);

		for (int i = 0; i < NumInitRabbits; i++) {
			addNewRabbit();
		}

		for (int i = 0; i < rabbitList.size(); i++) {
			RabbitsGrassSimulationAgent cda = rabbitList.get(i);
			cda.report();
		}
	}

	private void addNewRabbit() {
		RabbitsGrassSimulationAgent rabbit = new RabbitsGrassSimulationAgent(MaxEnergy);
		rabbitList.add(rabbit);
		space.addRabbit(rabbit);
	}

	private int updateSpace() {
		int count = 0;
		for (int i = (rabbitList.size() - 1); i >= 0; i--) {
			RabbitsGrassSimulationAgent cda = rabbitList.get(i);
			if (cda.getEnergy() >= BirthThreshold) {
				cda.setEnergy(cda.getEnergy() / 3);
				count++;
			} else if (cda.getEnergy() < 1) {
				space.removeRabbitAt(cda.getX(), cda.getY());
				rabbitList.remove(i);
			}
		}

		space.spreadGrass(GrassGrowthRate, MaxGrassEnergy);
		return count;
	}

	private int countLivingAgents() {
		int livingAgents = 0;
		for (int i = 0; i < rabbitList.size(); i++) {
			RabbitsGrassSimulationAgent cda = rabbitList.get(i);
			if (cda.getEnergy() > 0)
				livingAgents++;
		}
		System.out.println("Number of living rabbits is: " + livingAgents);

		return livingAgents;
	}

	public void buildSchedule() {
		System.out.println("Running BuildSchedule");

		class RabbitGrassStep extends BasicAction {
			public void execute() {
				SimUtilities.shuffle(rabbitList);
				for (int i = 0; i < rabbitList.size(); i++) {
					RabbitsGrassSimulationAgent cda = rabbitList.get(i);
					cda.step();
				}

				// Create one new rabbit for each existing one with high energy
				int newRabbits = updateSpace();
				for (int i = 0; i < newRabbits; i++) {
					if (rabbitList.size() < GridSize * GridSize) {
						addNewRabbit();
					} else {
						System.out.println("Grid is full of Rabbits: Unable to allocate more");
					}
				}

				displaySurf.updateDisplay();
			}
		}

		schedule.scheduleActionBeginning(0, new RabbitGrassStep());

		class RabbitGrassCountLiving extends BasicAction {
			public void execute() {
				countLivingAgents();
			}
		}

		schedule.scheduleActionAtInterval(10, new RabbitGrassCountLiving());

		class RabbitsGrassUpdateEnergyInSpace extends BasicAction {
			public void execute() {
				amountOfEnergyInSpace.step();
			}
		}

		schedule.scheduleActionAtInterval(5, new RabbitsGrassUpdateEnergyInSpace());

		class RabbitsGrassUpdateEnergy extends BasicAction {
			public void execute() {
				rabbitEnergyDistribution.step();
			}
		}

		schedule.scheduleActionAtInterval(10, new RabbitsGrassUpdateEnergy());
	}

	public void buildDisplay() {
		System.out.println("Running BuildDisplay");

		ColorMap map = new ColorMap();

		for(int i=1; i< (MaxGrassEnergy + 1); i++) {
			map.mapColor(i, 0, i*(1.0 / MaxGrassEnergy), 0);
		}

		map.mapColor(0, Color.black);

		Value2DDisplay displayEnergy = new Value2DDisplay(space.getCurrentGrassSpace(), map);

		Object2DDisplay displayRabbits = new Object2DDisplay(space.getCurrentRabbitSpace());
		displayRabbits.setObjectList(rabbitList);

		// Main display of grid with grass and rabbits
		displaySurf.addDisplayable(displayEnergy, "Grass");
		displaySurf.addDisplayable(displayRabbits, "Rabbits");

		// Plot grass and rabbit's energy
		amountOfEnergyInSpace.addSequence("Grass' energy", new energyInSpace());
		amountOfEnergyInSpace.addSequence("Rabbits' energy", new rabbitsInSpace());
		
		// Histogram of energy distribution among rabbits
		rabbitEnergyDistribution.createHistogramItem("Rabbit Energy", rabbitList, new rabbitEnergy());

	}

	public String[] getInitParam() {
		// TODO Auto-generated method stub
		// Parameters to be set by users via the Repast UI slider bar
		// Do "not" modify the parameters names provided in the skeleton code, you can
		// add more if you want
		String[] params = { "GridSize", "NumInitRabbits", "NumInitGrass", "GrassGrowthRate", "BirthThreshold",
				"MaxEnergy", "MaxGrassEnergy" };
		return params;
	}

	public String getName() {
		return "Rabbits Grass Model";
	}

	public Schedule getSchedule() {
		return schedule;
	}

	public void setup() {
		space = null;
		rabbitList = new ArrayList<RabbitsGrassSimulationAgent>();
		schedule = new Schedule(1);

		// Tear down Displays
		if (displaySurf != null) {
			displaySurf.dispose();
		}
		displaySurf = null;

		if (amountOfEnergyInSpace != null) {
			amountOfEnergyInSpace.dispose();
		}
		amountOfEnergyInSpace = null;

		if (rabbitEnergyDistribution != null) {
			rabbitEnergyDistribution.dispose();
		}
		rabbitEnergyDistribution = null;

		// Create Displays
		displaySurf = new DisplaySurface(this, "Rabbits Grass model W1");
		amountOfEnergyInSpace = new OpenSequenceGraph("Amount of Energy In Space", this);
		// OpenHistogram(title, # of bins, lower bound)
		rabbitEnergyDistribution = new OpenHistogram("Rabbit Energy", 8, -1);

		// Register Displays
		registerDisplaySurface("Rabbits Grass model W1", displaySurf);
		this.registerMediaProducer("Plot", amountOfEnergyInSpace);

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

	public int getMaxEnergy() {
		return MaxEnergy;
	}

	public void setMaxEnergy(int maxEnergy) {
		MaxEnergy = maxEnergy;
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

	public int getMaxGrassEnergy() {
		return MaxGrassEnergy;
	}

	public void setMaxGrassEnergy(int maxGrassEnergy) {
		MaxGrassEnergy = maxGrassEnergy;
	}
}
