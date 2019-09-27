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
	private static final int MAXGRASSENERGY = 3;

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
			return (double) space.getTotalEnergy();
		}
	}

	class rabbitEnergy implements BinDataSource {
		public double getBinValue(Object o) {
			RabbitsGrassSimulationAgent cda = (RabbitsGrassSimulationAgent) o;
			return (double) cda.getEnergy();
		}
	}

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

	/**
	 * Build model, schedule and display and initialize plots
	 */
	public void begin() {
		buildModel();
		buildSchedule();
		buildDisplay();

		displaySurf.display();
		amountOfEnergyInSpace.display();
		rabbitEnergyDistribution.display();
	}

	/**
	 * Build he model and add the initial grass and number of rabbits
	 * Report each of the rabbits
	 */
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

	/**
	 * Add a new rabbit to the space and the list of current rabbits
	 */
	private void addNewRabbit() {
		RabbitsGrassSimulationAgent rabbit = new RabbitsGrassSimulationAgent(MaxEnergy);
		rabbitList.add(rabbit);
		space.addRabbit(rabbit);
	}

	/**
	 * For all the rabbits in the map, check if
	 * a certain rabbit can give birth to new ones and reduce its energy
	 * to one third of the current one if that is the case. Also check if
	 * their energy level is 0 and remove them if this happens.
	 * 
	 * This function also spreads several units of grass determined by
	 * the variable GrassGrowthRate.
	 * @return int Number of rabbits that have to be added
	 */
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

	/**
	 * Count and print the number of living rabbits.
	 * @return int Number of living rabbits
	 */
	private int countLivingAgents() {
		int livingRabbits = 0;
		for (int i = 0; i < rabbitList.size(); i++) {
			RabbitsGrassSimulationAgent cda = rabbitList.get(i);
			if (cda.getEnergy() > 0)
				livingRabbits++;
		}
		System.out.println("Number of living rabbits is: " + livingRabbits);

		return livingRabbits;
	}

	/**
	 * Create schedule with each action to be performed and its period of repetition
	 */
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

		schedule.scheduleActionAtInterval(10, new RabbitsGrassUpdateEnergyInSpace());

		class RabbitsGrassUpdateEnergy extends BasicAction {
			public void execute() {
				rabbitEnergyDistribution.step();
			}
		}

		schedule.scheduleActionAtInterval(10, new RabbitsGrassUpdateEnergy());
	}

	/**
	 * Map values of the map to colors and create displays and plots
	 */
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

		displaySurf.addDisplayableProbeable(displayEnergy, "Grass");
		displaySurf.addDisplayableProbeable(displayRabbits, "Rabbits");

		amountOfEnergyInSpace.addSequence("Energy in Space", new energyInSpace());
		rabbitEnergyDistribution.createHistogramItem("Rabbit Energy", rabbitList, new rabbitEnergy());

	}

	/**
	 * Parameters to be set by users via the Repast UI slider bar
	 * Do "not" modify the parameters names provided here
	 * @return String[] Array of the names of the initial parameters
	 */
	public String[] getInitParam() {
		String[] params = { "GridSize", "NumInitRabbits", "NumInitGrass", "GrassGrowthRate", "BirthThreshold",
				"MaxEnergy", "MaxGrassEnergy" };
		return params;
	}

	public void setup() {
		System.out.println("Running setup");
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
		// We set lower bound to -1 to allow value 0
		rabbitEnergyDistribution = new OpenHistogram("Rabbit Energy", 8, -1);

		// Register Displays
		registerDisplaySurface("Rabbits Grass model W1", displaySurf);
		this.registerMediaProducer("Plot", amountOfEnergyInSpace);

	}
	
	// Getters and Setters
	public String getName() {
		return "Rabbits Grass Model";
	}

	public Schedule getSchedule() {
		return schedule;
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
