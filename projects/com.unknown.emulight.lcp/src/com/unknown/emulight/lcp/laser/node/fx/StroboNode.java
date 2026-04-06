package com.unknown.emulight.lcp.laser.node.fx;

import com.unknown.emulight.lcp.laser.node.GroupNode;
import com.unknown.emulight.lcp.laser.node.Node;
import com.unknown.emulight.lcp.laser.node.Property;
import com.unknown.emulight.lcp.laser.node.StandardPropertyNames;
import com.unknown.math.g3d.Mtx44;

public class StroboNode extends GroupNode {
	public static final String TYPE = "strobo";

	private final Property<Boolean> stroboEnable = new Property<>("stroboEnable", true);
	private final Property<Double> stroboSpeed = new Property<>("stroboSpeed", 1.0, 0.0, 10000.0);
	private final Property<Double> dutyCycle = new Property<>(StandardPropertyNames.DUTY_CYCLE, 0.5, 0.0, 1.0);

	public StroboNode() {
		super(TYPE);
		addProperty(stroboEnable);
		addProperty(stroboSpeed);
		addProperty(dutyCycle);
	}

	private boolean isStroboEnabled(int time) {
		return stroboEnable.getValue(time);
	}

	public void setStroboEnabled(int time, boolean enabled) {
		stroboEnable.setValue(time, enabled);
	}

	public double getStroboSpeed(int time) {
		return stroboSpeed.getValue(time);
	}

	public void setStroboSpeed(int time, double speed) {
		stroboSpeed.setValue(time, speed);
	}

	public double getDutyCycle(int time) {
		return dutyCycle.getValue(time);
	}

	public void setDutyCycle(int time, double value) {
		dutyCycle.setValue(time, value);
	}

	@Override
	public Mtx44 getColorTransformation(int time) {
		double intensity = getBrightness(time);
		boolean enabled = isStroboEnabled(time);
		if(enabled) {
			// TODO: make this smooth and make it work with animated speeds
			int ppq = getClip().getProject().getPPQ();
			double speed = getStroboSpeed(time);
			double T = ppq / speed;
			double phi = (time % T) / T;
			if(phi >= getDutyCycle(time)) {
				intensity = 0;
			}
		}
		return Mtx44.scale(getColorScale(time)).applyScale(intensity, intensity, intensity);
	}

	@Override
	public StroboNode clone() {
		StroboNode node = new StroboNode();
		copy(node);
		for(Node n : getChildren()) {
			node.addChild(n.clone());
		}
		return node;
	}
}
