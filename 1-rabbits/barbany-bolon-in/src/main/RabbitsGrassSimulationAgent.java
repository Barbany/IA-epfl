package main;

import java.awt.Color;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.space.Object2DGrid;

/**
 * Class that implements the simulation agent for the rabbits grass simulation.
 * 
 * @author
 */

public class RabbitsGrassSimulationAgent implements Drawable {

	private static int IDNum = 0;

	private int x;
	private int y;
	private int energy;
	private int ID;
	private RabbitsGrassSimulationSpace rabbitSpace;

	public RabbitsGrassSimulationAgent(int maxEnergy) {
		x = -1;
		y = -1;
		energy = (int) ((Math.random() * (maxEnergy)));
		IDNum++; // Set ID of the agent
		ID = IDNum;
	}

	public void setRabbitSpace(RabbitsGrassSimulationSpace rabbitSpace) {
		this.rabbitSpace = rabbitSpace;
	}

	public void setXY(int newX, int newY) {
		x = newX;
		y = newY;
	}

	public String getID() {
		return "Rabbit-" + ID;
	}

	public int getEnergy() {
		return energy;
	}

	public void setEnergy(int energy) {
		this.energy = energy;
	}

	public void report() {
		System.out.println(getID() + " at " + x + ", " + y + " has " + getEnergy() + " energy");
	}

	public int getX() {
		// TODO Auto-generated method stub
		return x;
	}

	public int getY() {
		// TODO Auto-generated method stub
		return y;
	}

	public void draw(SimGraphics G) {
		G.drawFastRoundRect(Color.white);

	}

	public void step() {
		int newX = x;
		int newY = y;
		Object2DGrid grid = rabbitSpace.getCurrentRabbitSpace();

		// Perform next movement considering the torus structure of the grid
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
		
		System.out.printf("New coordinates: %d, %d \n", newX, newY);

		if (tryMove(newX, newY)) {
			energy += rabbitSpace.eatGrassAt(newX, newY);
			energy--;
		}
	}

	private boolean tryMove(int newX, int newY) {
		return rabbitSpace.moveRabbitAt(x, y, newX, newY);
	}

}
