package com.unknown.emulight.lcp.laser.node;

import java.util.ArrayList;
import java.util.List;

import com.unknown.emulight.lcp.laser.Point3D;
import com.unknown.math.g3d.Mtx44;
import com.unknown.math.g3d.Vec3;

public class LineNode extends Node {
	private Property<Vec3> start = new Property<>(StandardPropertyNames.START, new Vec3(0, 0, 0));
	private Property<Vec3> end = new Property<>(StandardPropertyNames.END, new Vec3(0, 0, 0));
	private Property<Color3> color = new Property<>(StandardPropertyNames.COLOR, new Color3(1, 1, 1));
	private final Property<Integer> pointCount = new Property<>(StandardPropertyNames.POINTS, 2);
	private final Property<Integer> repetition = new Property<>(StandardPropertyNames.REPETITION, 1);
	private final Property<Boolean> connected = new Property<>(StandardPropertyNames.CONNECTED, true);

	public LineNode() {
		super(true);

		addProperty(start);
		addProperty(end);
		addProperty(color);
		addProperty(pointCount);
		addProperty(repetition);
		addProperty(connected);
	}

	public Vec3 getStart() {
		return start.getValue();
	}

	public void setStart(Vec3 start) {
		this.start.setValue(start);
	}

	public Vec3 getEnd() {
		return end.getValue();
	}

	public void setEnd(Vec3 end) {
		this.end.setValue(end);
	}

	public Color3 getColor() {
		return color.getValue();
	}

	public void setColor(Color3 color) {
		this.color.setValue(color);
	}

	public int getPointCount() {
		return pointCount.getValue();
	}

	public void setPointCount(int pointCount) {
		this.pointCount.setValue(pointCount);
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

		Vec3 pos1 = positionMtx.mult(getStart());
		Vec3 pos2 = positionMtx.mult(getEnd());
		Vec3 col = colorMtx.mult(getColor());
		Vec3 delta = pos2.sub(pos1);

		int count = getPointCount();

		if(count <= 2) {
			result.add(new Shape(this, List.of(new Point3D(pos1, col), new Point3D(pos2, col))));
		} else {
			Vec3 step = delta.scale(1.0 / (count - 1.0));

			int rep = getRepetition();
			boolean con = isConnected();

			List<Point3D> points = new ArrayList<>();
			for(int i = 0; i < count; i++) {
				Vec3 pos = pos1.add(step.scale(i));
				if(!con) {
					points.add(new Point3D(pos, new Vec3(0, 0, 0)));
				}
				for(int j = 0; j < rep; j++) {
					points.add(new Point3D(pos, col));
				}
				if(!con) {
					points.add(new Point3D(pos, new Vec3(0, 0, 0)));
				}
			}

			result.add(new Shape(this, points));
		}

		return result;
	}

	@Override
	public LineNode clone() {
		LineNode node = new LineNode();
		node.copyFrom(this);
		return node;
	}
}
