package com.unknown.emulight.lcp.laser.node;

import java.util.ArrayList;
import java.util.List;

import com.unknown.emulight.lcp.laser.Point3D;
import com.unknown.math.g3d.Mtx44;
import com.unknown.math.g3d.Vec3;

public class PointNode extends Node {
	public static final String TYPE = "point";

	private final Property<Vec3> position = new Property<>(StandardPropertyNames.POSITION, ZERO, MIN3D, MAX3D);
	private final Property<Color3> color = new Property<>(StandardPropertyNames.COLOR, WHITE);
	private final Property<Integer> repetition = new Property<>(StandardPropertyNames.REPETITION, 1, 1, 1000,
			false);

	public PointNode() {
		super(TYPE, true);

		addProperty(position);
		addProperty(color);
		addProperty(repetition);
	}

	public Vec3 getPosition(int time) {
		return position.getValue(time);
	}

	public void setPosition(int time, Vec3 position) {
		this.position.setValue(time, position);
	}

	public Color3 getColor(int time) {
		return color.getValue(time);
	}

	public void setColor(int time, Color3 color) {
		this.color.setValue(time, color);
	}

	public int getRepetition(int time) {
		return repetition.getValue(time);
	}

	public void setRepetition(int time, int repetition) {
		if(repetition < 1) {
			throw new IllegalArgumentException("Invalid repetition count");
		}
		this.repetition.setValue(time, repetition);
	}

	@Override
	protected List<Shape> render(List<Shape> result, int time, Mtx44 positionTransform, Mtx44 colorTransform) {
		Mtx44 positionMtx = positionTransform.concat(getTransformation(time));
		Mtx44 colorMtx = colorTransform.concat(getColorTransformation(time));

		Vec3 pos = positionMtx.mult(position.getValue(time));
		Vec3 col = colorMtx.mult(color.getValue(time));

		int rep = getRepetition(time);
		List<Point3D> points = new ArrayList<>();
		for(int i = 0; i < rep; i++) {
			points.add(new Point3D(pos, col));
		}
		result.add(new Shape(this, points));

		return result;
	}

	@Override
	public PointNode clone() {
		PointNode node = new PointNode();
		node.copyFrom(this);
		return node;
	}
}
