package main;
/**
 * Class that implements the simulation space of the rabbits grass simulation.
 * @author Oriol Barbany & Natalie Bolon
 */

// import uchicago.src.sim.space.Discrete2DSpace;
import uchicago.src.sim.space.Object2DGrid;

public class RabbitsGrassSimulationSpace {
	private Object2DGrid grassSpace;
	private Object2DGrid rabbitSpace;

	/**
	 * Constructor of the class RabbitsGrassSimulationSpace
	 * @param GridSize Length of any axis of the square grid
	 */
	public RabbitsGrassSimulationSpace(int GridSize) {
		grassSpace = new Object2DGrid(GridSize, GridSize);
		rabbitSpace = new Object2DGrid(GridSize, GridSize);
		

		for (int i = 0; i < GridSize; i++) {
			for (int j = 0; j < GridSize; j++) {
				grassSpace.putObjectAt(i, j, new Integer(0));
			}
		}
	}

	/** Spread grass in space in a random cell and with a random quantity
	 * of energy in the integers [1, MaxGrassEnergy]. If there is already grass
	 * in this cell, it will be added and clipped to MaxGrassEnergy.
	 * @param numGrass This is the number of grass units that will be allocated
	 * @param MaxGrassEnergy This is the maximum energy of each unit of grass
	 */
	public void spreadGrass(int numGrass, int MaxGrassEnergy) {

		int i = 0;

		while (i < numGrass) {

			// Choose coordinates
			int x = (int) (Math.random() * (grassSpace.getSizeX()));
			int y = (int) (Math.random() * (grassSpace.getSizeY()));

			// Get value
			int currentValue = getGrassAt(x, y);

			int nextVal = new Integer((int)(1 + Math.random() * (MaxGrassEnergy)));
			// Assign new value - between 1 and maximum grass energy level 
			grassSpace.putObjectAt(x, y, Math.min(currentValue + nextVal, MaxGrassEnergy) );
			
			i++;
		
		}
	}

	/**
	 * Return energy of grass in the current space
	 * (0 if there is no grass there)
	 * @param x This is the x coordinate
	 * @param y This is the y coordinate
	 * @return int Amount of energy in (x, y)
	 */
	public int getGrassAt(int x, int y) {
		int i;
		if (grassSpace.getObjectAt(x, y) != null) {
			i = ((Integer) grassSpace.getObjectAt(x, y)).intValue();
		} else {
			i = 0;
		}
		return i;
	}
	
	/**
	 * Return rabbits in the current space
	 * (0 if there is no rabbit, 1 if there is one)
	 * @param x This is the x coordinate
	 * @param y This is the y coordinate
	 * @return int Amount of rabbits in (x, y)
	 */
	public int getRabbitEnergyAt(int x, int y) {
		int i;
		if (rabbitSpace.getObjectAt(x, y) != null) {
			RabbitsGrassSimulationAgent agent = (RabbitsGrassSimulationAgent) rabbitSpace.getObjectAt(x, y);
			i = agent.getEnergy();
		}else {
			i = 0;
		}
			
		return i;
	}
		
	

	/**
	 * Check if there is a rabbit in the specified cell
	 * @param x This is the x coordinate
	 * @param y This is the y coordinate
	 * @return boolean Return true if cell is occupied
	 */
	public boolean isCellOccupied(int x, int y) {
		boolean value = false;
		if (rabbitSpace.getObjectAt(x, y) != null)
			value = true;
		return value;
	}

	/**
	 * Add a new rabbit located in the specified cell. The initial
	 * location is random and we try a maximum of 10 times the number
	 * of cells in the space. This is an educated guess that makes
	 * the probability of no allocation when there is actually space
	 * very low and avoids an infinite loop when there is no space.
	 * @param RabbitsGrassSimulationAgent Instance of the agent to be allocated
	 * @return True if location was possible
	 */
	public boolean addRabbit(RabbitsGrassSimulationAgent agent) {
		boolean value = false;
		int count = 0;
		int countLimit = 10 * rabbitSpace.getSizeX() * rabbitSpace.getSizeY();

		while ((value == false) && (count < countLimit)) {
			int x = (int) (Math.random() * (rabbitSpace.getSizeX()));
			int y = (int) (Math.random() * (rabbitSpace.getSizeY()));
			if (isCellOccupied(x, y) == false) {
				rabbitSpace.putObjectAt(x, y, agent);
				agent.setXY(x, y);
				agent.setRabbitSpace(this);
				value = true;
			}
			count++;
		}

		return value;
	}

	/**
	 * Kill a rabbit located in the specified cell
	 * @param x This is the x coordinate
	 * @param y This is the y coordinate
	 */
	public void removeRabbitAt(int x, int y) {
		rabbitSpace.putObjectAt(x, y, null);
	}

	/**
	 * This method tries to eat grass at one position
	 * (either if there is grass or not). In both cases, we 
	 * get the energy level (that will be 0 in the case that
	 * there is no grass) and set it to 0.
	 * @param x This is the x coordinate
	 * @param y This is the y coordinate
	 * @return int energy at cell (x, y)
	 */
	public int eatGrassAt(int x, int y) {
		int energy = getGrassAt(x, y);
		grassSpace.putObjectAt(x, y, new Integer(0));
		return energy;
	}

	/**
	 * This method tries a movement of a rabbit from one cell to another.
	 * Note that in the case that only NSEW are allowed,
	 * either x = newX or y = newY but not both or any of them.
	 * @param x This is the current x coordinate
	 * @param y This is the current y coordinate
	 * @param newX This is the possibly future x coordinate
	 * @param newY This is the possibly future y coordinate
	 * @return boolean True if movement was legal (new cell not occupied)
	 */
	public boolean moveRabbitAt(int x, int y, int newX, int newY) {
		boolean retVal = false;
		if (!isCellOccupied(newX, newY)) {
			RabbitsGrassSimulationAgent cda = (RabbitsGrassSimulationAgent) rabbitSpace.getObjectAt(x, y);
			removeRabbitAt(x, y);
			cda.setXY(newX, newY);
			rabbitSpace.putObjectAt(newX, newY, cda);
			retVal = true;
		}
		return retVal;
	}

	/**
	 * This method returns the total energy stored in form of grass
	 * in the space. It iterates all the space and collect the energy
	 * of each cell (without considering rabbits on it).
	 * @return int Total energy at current step
	 */
	public int getTotalGrassEnergy() {
		int totalEnergy = 0;
		for (int i = 0; i < grassSpace.getSizeX(); i++) {
			for (int j = 0; j < grassSpace.getSizeY(); j++) {
				totalEnergy += getGrassAt(i, j);
			}
		}
		return totalEnergy;
	}
	
	
	/**
	 * This method returns the total number of alive rabbits
	 * in the space. It iterates all the space and collect the rabbits
	 * of each cell.
	 * @return int Total number of alive rabbits at current step
	 */
	public int getTotalRabbitsEnergy() {
		int totalRabbits = 0;
		for (int i = 0; i < rabbitSpace.getSizeX(); i++) {
			for (int j = 0; j < rabbitSpace.getSizeY(); j++) {
				totalRabbits += getRabbitEnergyAt(i, j);
			}
		}
		return totalRabbits;
	}
	
	/**
	 * @return Object2DGrid grassSpace
	 */
	public Object2DGrid getCurrentGrassSpace() {
		return grassSpace;
	}

	/**
	 * @return Object2DGrid rabbitSpace
	 */
	public Object2DGrid getCurrentRabbitSpace() {
		return rabbitSpace;
	}

}
