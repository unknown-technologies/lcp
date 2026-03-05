package com.unknown.emulight.lcp.laser.node;

import java.util.ArrayList;
import java.util.List;

import com.unknown.emulight.lcp.laser.Point3D;
import com.unknown.math.g3d.Mtx44;
import com.unknown.math.g3d.Vec3;

public class CircleNode extends Node {
	private final Property<Vec3> position = new Property<>(StandardPropertyNames.POSITION, new Vec3(0, 0, 0));
	private final Property<Color3> color = new Property<>(StandardPropertyNames.COLOR, new Color3(1, 1, 1));
	private final Property<Double> radius = new Property<>(StandardPropertyNames.RADIUS, 0.1);
	private final Property<Integer> pointCount = new Property<>(StandardPropertyNames.POINTS, 10);
	private final Property<Integer> repetition = new Property<>(StandardPropertyNames.REPETITION, 1);
	private final Property<Boolean> connected = new Property<>(StandardPropertyNames.CONNECTED, true);

	public CircleNode() {
		super(true);

		addProperty(position);
		addProperty(color);
		addProperty(radius);
		addProperty(pointCount);
		addProperty(repetition);
		addProperty(connected);
	}

	public int getPointCount() {
		return pointCount.getValue();
	}

	public void setPointCount(int pointCount) {
		this.pointCount.setValue(pointCount);
	}

	public Vec3 getPosition() {
		return position.getValue();
	}

	public void setPosition(Vec3 position) {
		this.position.setValue(position);
	}

	public double getRadius() {
		return radius.getValue();
	}

	public void setRadius(double radius) {
		this.radius.setValue(radius);
	}

	public Color3 getColor() {
		return color.getValue();
	}

	public void setColor(Color3 color) {
		this.color.setValue(color);
	}

	public int getRepetition() {
		return repetition.getValue();
	}

	public void setRepetition(int repetition) {
		if(repetition < 1) {
			throw new IllegalArgumentException("Invalid repetition count");
		}
		this.repetition.setValue(repetition);
	}

	public boolean isConnected() {
		return connected.getValue();
	}

	public void setConnected(boolean connected) {
		this.connected.setValue(connected);
	}

	@Override
	protected List<Shape> render(List<Shape> result, Mtx44 positionTransform, Mtx44 colorTransform) {
		Mtx44 positionMtx = positionTransform.concat(getTransformation());
		Mtx44 colorMtx = colorTransform.concat(getColorTransformation());

		int cnt = pointCount.getValue() + 1;
		int max = cnt - 1;
		if(cnt < 2) {
			return result;
		}

		Vec3 col = colorMtx.mult(color.getValue());

		double r = radius.getValue();
		Vec3 pos = position.getValue();

		int rep = getRepetition();
		boolean con = isConnected();

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
