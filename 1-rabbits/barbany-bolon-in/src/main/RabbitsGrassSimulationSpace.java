package main;
/**
 * Class that implements the simulation space of the rabbits grass simulation.
 * @author 
 */

import uchicago.src.sim.space.Object2DGrid;


public class RabbitsGrassSimulationSpace {
	private Object2DGrid grassSpace;
	private Object2DGrid rabbitSpace; 
	
	public RabbitsGrassSimulationSpace(int GridSize) {
		grassSpace = new Object2DGrid(GridSize, GridSize);
		rabbitSpace = new Object2DGrid(GridSize, GridSize);
		
		for(int i = 0; i < GridSize; i++) {
			for(int j = 0; j < GridSize; j++) {
				grassSpace.putObjectAt(i,j, new Integer(0));
			}
		}
	}
	
	// Initialize grass in grass space
	public void spreadGrass(int numInitGrass) {
		
		int i = 0;
		
		while(i < numInitGrass) {
			
			// Choose coordinates
		      int x = (int)(Math.random()*(grassSpace.getSizeX()));
		      int y = (int)(Math.random()*(grassSpace.getSizeY()));

		   // Get value 
		      int currentValue = getGrassAt(x,y);
		   
		   // Assign new value - allows to have more than 1 grass
		      if(currentValue == 0) {
		    	 grassSpace.putObjectAt(x,y,new Integer(1));
		    	 i++;
		      }    
		}
	}
	
	// Return actual grass in the current space
	public int getGrassAt(int x, int y){
	    int i;
	    if(grassSpace.getObjectAt(x,y)!= null){
	      i = ((Integer)grassSpace.getObjectAt(x,y)).intValue();
	    }
	    else{
	      i = 0;
	    }
	    return i;
	  }
	
	public Object2DGrid getCurrentGrassSpace() {
		return grassSpace;
	}
	
	public Object2DGrid getCurrentAgentSpace(){
	    return rabbitSpace;
	}
	
	public boolean isCellOccupied(int x, int y){
	    boolean value = false;
	    if(rabbitSpace.getObjectAt(x, y)!=null) value = true;
	    return value;
	  }

	  public boolean addRabbit(RabbitsGrassSimulationAgent agent){
	    boolean value = false;
	    int count = 0;
	    int countLimit = 10 * rabbitSpace.getSizeX() * rabbitSpace.getSizeY();

	    while((value==false) && (count < countLimit)){
	      int x = (int)(Math.random()*(rabbitSpace.getSizeX()));
	      int y = (int)(Math.random()*(rabbitSpace.getSizeY()));
	      if(isCellOccupied(x,y) == false){
	    	  rabbitSpace.putObjectAt(x,y,agent);
	        agent.setXY(x,y);
	        value = true;
	      }
	      count++;
	    }

	    return value;
	  }
	

}
