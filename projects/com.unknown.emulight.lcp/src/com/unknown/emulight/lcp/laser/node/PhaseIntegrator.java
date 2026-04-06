package com.unknown.emulight.lcp.laser.node;

import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

public class PhaseIntegrator {
	private final Property<Double> property;

	private NavigableMap<Integer, Double> phaseAccumulator = new TreeMap<>();

	public PhaseIntegrator(Property<Double> property) {
		if(!property.getType().equals(Double.class)) {
			throw new IllegalStateException("cannot use phase integrator for non-double property");
		}

		this.property = property;
	}

	public void recompute() {
		phaseAccumulator.clear();

		double phi = 0;

		phaseAccumulator.put(0, phi);

		if(property.getCount() < 2) {
			return;
		}

		int lastTime = 0;
		double lastSpeed = 0;
		for(Entry<Integer, Double> entry : property.getValues().sequencedEntrySet()) {
			int time = entry.getKey();
			double speed = entry.getValue();

			if(time != 0) {
				double t = time - lastTime;
				double dT = speed - lastSpeed;
				double dphi = lastSpeed * t + dT * t / 2.0;

				phi += dphi;

				phaseAccumulator.put(time, phi);
			}

			lastTime = time;
			lastSpeed = speed;
		}
	}

	public double getPhase(int time, int ppq) {
		Entry<Integer, Double> entry = phaseAccumulator.floorEntry(time);
		int lastTime = entry.getKey();
		double phi = entry.getValue();
		if(time == lastTime) {
			return (phi / ppq) % 1.0;
		}

		double speed0 = property.getValue(entry.getKey());

		NavigableMap<Integer, Double> values = property.getValues();
		Integer next = values.ceilingKey(time);
		if(next == null) {
			int dt = time - lastTime;
			double dphi = dt * speed0;
			phi += dphi;
			return (phi / ppq) % 1.0;
		}

		double speed1 = values.get(next);

		double t = time - lastTime;
		double dt = t / (next - lastTime);
		double dT = (speed1 - speed0) * dt;
		double dphi = speed0 * t + dT * t / 2.0;

		phi += dphi;

		return (phi / ppq) % 1.0;
	}
}
