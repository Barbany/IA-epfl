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

	public RabbitsGrassSimulationSpace(int GridSize) {
		grassSpace = new Object2DGrid(GridSize, GridSize);
		rabbitSpace = new Object2DGrid(GridSize, GridSize);
		

		for (int i = 0; i < GridSize; i++) {
			for (int j = 0; j < GridSize; j++) {
				grassSpace.putObjectAt(i, j, new Integer(0));
			}
		}
	}

	// Initialize grass in grass space
	public void spreadGrass(int numInitGrass, int MaxGrassEnergy) {

		int i = 0;

		while (i < numInitGrass) {

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

	// Return actual grass in the current space
	public int getGrassAt(int x, int y) {
		int i;
		if (grassSpace.getObjectAt(x, y) != null) {
			i = ((Integer) grassSpace.getObjectAt(x, y)).intValue();
		} else {
			i = 0;
		}
		return i;
	}

	public Object2DGrid getCurrentGrassSpace() {
		return grassSpace;
	}

	public Object2DGrid getCurrentRabbitSpace() {
		return rabbitSpace;
	}

	public boolean isCellOccupied(int x, int y) {
		boolean value = false;
		if (rabbitSpace.getObjectAt(x, y) != null)
			value = true;
		return value;
	}

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

	public void removeRabbitAt(int x, int y) {
		rabbitSpace.putObjectAt(x, y, null);
	}

	public int eatGrassAt(int x, int y) {
		int energy = getGrassAt(x, y);
		grassSpace.putObjectAt(x, y, new Integer(0));
		return energy;
	}

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

	public int getTotalEnergy() {
		int totalEnergy = 0;
		for (int i = 0; i < grassSpace.getSizeX(); i++) {
			for (int j = 0; j < grassSpace.getSizeY(); j++) {
				totalEnergy += getGrassAt(i, j);
			}
		}
		return totalEnergy;
	}

}
