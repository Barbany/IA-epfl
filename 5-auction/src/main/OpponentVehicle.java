package main;

import java.awt.Color;

import logist.simulation.Vehicle;
import logist.task.TaskSet;
import logist.topology.Topology.City;

public class OpponentVehicle implements Vehicle {
	int id, capacity, costPerKm;
	double speed, distance;
	long reward, distanceUnits;
	String name;
	City homeCity, currentCity;
	Color color;
	TaskSet currentTasks;
	
	public OpponentVehicle (Vehicle v, City currentCity) {
		this.id = v.id();
		this.capacity = v.capacity();
		this.costPerKm = v.costPerKm();
		this.speed = v.speed();
		this.distance = v.getDistance();
		this.reward = v.getReward();
		this.distanceUnits = v.getDistanceUnits();
		this.name = v.name();
		this.homeCity = v.homeCity();
		this.currentCity = currentCity;
		this.color = v.color();
	}

	@Override
	public int id() {
		return id;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public int capacity() {
		return capacity;
	}

	@Override
	public City homeCity() {
		return homeCity;
	}

	@Override
	public double speed() {
		return speed;
	}

	@Override
	public int costPerKm() {
		return costPerKm;
	}

	@Override
	public City getCurrentCity() {
		return currentCity;
	}

	@Override
	public TaskSet getCurrentTasks() {
		return null;
	}

	@Override
	public long getReward() {
		return reward;
	}

	@Override
	public long getDistanceUnits() {
		return distanceUnits;
	}

	@Override
	public double getDistance() {
		return distance;
	}

	@Override
	public Color color() {
		return color;
	}
}
