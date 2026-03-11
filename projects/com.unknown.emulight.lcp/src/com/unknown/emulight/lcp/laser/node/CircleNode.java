package com.unknown.emulight.lcp.laser.node;

import java.util.ArrayList;
import java.util.List;

import com.unknown.emulight.lcp.laser.Point3D;
import com.unknown.math.g3d.Mtx44;
import com.unknown.math.g3d.Vec3;

public class CircleNode extends Node {
	public static final String TYPE = "circle";

	private final Property<Vec3> position = new Property<>(StandardPropertyNames.POSITION, ZERO, MIN3D, MAX3D);
	private final Property<Color3> color = new Property<>(StandardPropertyNames.COLOR, WHITE);
	private final Property<Double> radius = new Property<>(StandardPropertyNames.RADIUS, 0.1, 0.0, 1.0);
	private final Property<Integer> pointCount = new Property<>(StandardPropertyNames.POINTS, 10, 3, 1000, false);
	private final Property<Integer> repetition = new Property<>(StandardPropertyNames.REPETITION, 1, 1, 1000,
			false);
	private final Property<Boolean> connected = new Property<>(StandardPropertyNames.CONNECTED, true, false);

	public CircleNode() {
		super(TYPE, true);

		addProperty(position);
		addProperty(color);
		addProperty(radius);
		addProperty(pointCount);
		addProperty(repetition);
		addProperty(connected);
	}

	public int getPointCount(int time) {
		return pointCount.getValue(time);
	}

	public void setPointCount(int time, int pointCount) {
		this.pointCount.setValue(time, pointCount);
	}

	public Vec3 getPosition(int time) {
		return position.getValue(time);
	}

	public void setPosition(int time, Vec3 position) {
		this.position.setValue(time, position);
	}

	public double getRadius(int time) {
		return radius.getValue(time);
	}

	public void setRadius(int time, double radius) {
		this.radius.setValue(time, radius);
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

	public boolean isConnected(int time) {
		return connected.getValue(time);
	}

	public void setConnected(int time, boolean connected) {
		this.connected.setValue(time, connected);
	}

	@Override
	protected List<Shape> render(List<Shape> result, int time, Mtx44 positionTransform, Mtx44 colorTransform) {
		Mtx44 positionMtx = positionTransform.concat(getTransformation(time));
		Mtx44 colorMtx = colorTransform.concat(getColorTransformation(time));

		int cnt = getPointCount(time) + 1;
		int max = cnt - 1;
		if(cnt < 2) {
			return result;
		}

		Vec3 col = colorMtx.mult(getColor(time));

		double r = getRadius(time);
		Vec3 pos = getPosition(time);

		int rep = getRepetition(time);
		boolean con = isConnected(time);

		List<Point3D> points = new ArrayList<>();
		for(int i = 0; i < cnt; i++) {
			double phi = i / (double) max;
			double x = Math.cos(phi * 2.0 * Math.PI) * r;
			double y = Math.sin(phi * 2.0 * Math.PI) * r;

			Vec3 p = positionMtx.mult(new Vec3(x + pos.x, y + pos.y, 0));
			if(i == 0) {
				// TODO: not sure if this is necessary
				points.add(new Point3D(p, new Vec3(0, 0, 0)));
			} else {
				if(!con) {
					points.add(new Point3D(p, new Vec3(0, 0, 0)));
				}
				for(int j = 0; j < rep; j++) {
					points.add(new Point3D(p, col));
				}
				if(!con) {
					points.add(new Point3D(p, new Vec3(0, 0, 0)));
				}
			}
		}

		result.add(new Shape(this, points));

		return result;
	}

	@Override
	public CircleNode clone() {
		CircleNode node = new CircleNode();
		node.copyFrom(this);
		return node;
	}
}
