package main;

import java.awt.Color;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.space.Object2DGrid;

/**
 * Class that implements the simulation agent for the rabbits grass simulation.
 * 
 * @author Oriol Barbany & Natalie Bolon
 */

public class RabbitsGrassSimulationAgent implements Drawable {

	private static int IDNum = 0;

	private int x;
	private int y;
	private int energy;
	private int ID;
	private RabbitsGrassSimulationSpace rabbitSpace;

	/** 
	 * Constructor of the class RabbitsGrassSimulationAgent
	 * Assign a unique identifier and a random integer quantity of energy in [1, maxEnergy]
	 * @param maxEnergy This is the maximum energy that the new rabbit can have
	 */
	public RabbitsGrassSimulationAgent(int maxEnergy) {
		x = -1;
		y = -1;
		energy = (int) ((Math.random() * (maxEnergy)) + 1);
		IDNum++; // Set ID of the agent
		ID = IDNum;
	}

	/**
	 * Print the unique ID of the Rabbit along with its position and energy
	 */
	public void report() {
		System.out.println(getID() + " at " + x + ", " + y + " has " + getEnergy() + " energy");
	}
	
	/**
	 * Draw the rabbit as a white ring with transparent background.
	 * This is aimed to see if the rabbit is stepping on grass.
	 */
	public void draw(SimGraphics G) {
		G.drawHollowFastOval(Color.white);
	}

	/**
	 * Perform next movement considering the torus structure of the grid
	 * Eat grass at new move if this was performed.
	 * Otherwise check if grass has grown at current point.
	 * Note each step, rabbit loses energy even if it doesn't move
	 */
	public void step() {
		int newX = x;
		int newY = y;
		Object2DGrid grid = rabbitSpace.getCurrentRabbitSpace();

		switch ((int) Math.floor(Math.random() * 4)) {
		case 0:
			newX = (x + 1) % grid.getSizeX();
			break;
		case 1:
			newX = ((x - 1) < 0) ? grid.getSizeX() - 1 : x - 1;
			break;
		case 2:
			newY = (y + 1) % grid.getSizeY();
			break;
		default:
			newY = ((y - 1) < 0) ? grid.getSizeY() - 1 : y - 1;
			break;
		}

		if (tryMove(newX, newY)) {
			energy += rabbitSpace.eatGrassAt(newX, newY);
		} else {
			energy += rabbitSpace.eatGrassAt(x, y);
		}
		energy--;
	}

	/**
	 * Try a move
	 * @param newX This is the possibly new x coordinate
	 * @param newY This is the possibly new y coordinate
	 * @return boolean True if movement was performed
	 */
	private boolean tryMove(int newX, int newY) {
		return rabbitSpace.moveRabbitAt(x, y, newX, newY);
	}

	/**
	 * @param rabbitSpace Is the RabbitsGrassSimulationSpace where rabbits are
	 */
	public void setRabbitSpace(RabbitsGrassSimulationSpace rabbitSpace) {
		this.rabbitSpace = rabbitSpace;
	}

	/**
	 * @param newX
	 * @param newY
	 */
	public void setXY(int newX, int newY) {
		x = newX;
		y = newY;
	}

	/**
	 * @return String Agent name: "Rabbit-ID", where ID is the unique identifier
	 */
	public String getID() {
		return "Rabbit-" + ID;
	}

	/**
	 * @return Amount of energy that the rabbit has
	 */
	public int getEnergy() {
		return energy;
	}

	/**
	 * @param energy Set the energy that the agent will have
	 */
	public void setEnergy(int energy) {
		this.energy = energy;
	}
	
	/**
	 * @return int Get the x coordinate where agent is located
	 */
	public int getX() {
		return x;
	}

	/**
	 * @return int Get the y coordinate where agent is located
	 */
	public int getY() {
		return y;
	}

}
