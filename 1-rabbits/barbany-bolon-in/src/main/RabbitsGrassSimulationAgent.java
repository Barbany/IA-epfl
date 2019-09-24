package main;

import java.awt.Color;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;


/**
 * Class that implements the simulation agent for the rabbits grass simulation.

 * @author
 */

public class RabbitsGrassSimulationAgent implements Drawable {
	
	private static int IDNum = 0;
	
	private int x;
	private int y;
	private int energy;
	private int ID; 
	
	public RabbitsGrassSimulationAgent(int maxEnergy){
	    x = -1;
	    y = -1;
	    energy = (int)((Math.random() * (maxEnergy)));
	    IDNum++; // Set ID of the agent
	    ID = IDNum;
	  }
	
	public void setXY(int newX, int newY){
	    x = newX;
	    y = newY;
	  }
	
	public String getID(){
	    return "Rabbit-" + ID;
	  }

	  public int getEnergy(){
	    return energy;
	  }


	  public void report(){
	    System.out.println(getID() + 
	                       " at " + 
	                       x + ", " + y + 
	                       " has " + 
	                       getEnergy() + " energy");
	  }


	public void draw(SimGraphics G) {
		G.drawFastRoundRect(Color.white);
		
	}
	
	

	public int getX() {
		// TODO Auto-generated method stub
		return x;
	}

	public int getY() {
		// TODO Auto-generated method stub
		return y;
	}
	
	

}
