package com.unknown.emulight.lcp.laser.node;

import java.util.ArrayList;
import java.util.List;

import com.unknown.emulight.lcp.laser.Point3D;
import com.unknown.math.g3d.Mtx44;
import com.unknown.math.g3d.Vec3;

public class LineNode extends Node {
	public static final String TYPE = "line";

	private Property<Vec3> start = new Property<>(StandardPropertyNames.START, new Vec3(-0.1, 0, 0), MIN3D, MAX3D);
	private Property<Vec3> end = new Property<>(StandardPropertyNames.END, new Vec3(0.1, 0, 0), MIN3D, MAX3D);
	private Property<Color3> color = new Property<>(StandardPropertyNames.COLOR, WHITE);
	private Property<Color3> colorFade = new Property<>(StandardPropertyNames.COLOR_FADE, WHITE);
	private Property<Boolean> gradient = new Property<>(StandardPropertyNames.GRADIENT, false);
	private final Property<Integer> pointCount = new Property<>(StandardPropertyNames.POINTS, 2, 2, 1000, false);
	private final Property<Integer> repetition = new Property<>(StandardPropertyNames.REPETITION, 1, 1, 1000,
			false);
	private final Property<Boolean> connected = new Property<>(StandardPropertyNames.CONNECTED, true, false);

	public LineNode() {
		super(TYPE, true);

		addProperty(start);
		addProperty(end);
		addProperty(color);
		addProperty(colorFade);
		addProperty(gradient);
		addProperty(pointCount);
		addProperty(repetition);
		addProperty(connected);
	}

	public Vec3 getStart(int time) {
		return start.getValue(time);
	}

	public void setStart(int time, Vec3 start) {
		this.start.setValue(time, start);
	}

	public Vec3 getEnd(int time) {
		return end.getValue(time);
	}

	public void setEnd(int time, Vec3 end) {
		this.end.setValue(time, end);
	}

	public Color3 getColor(int time) {
		return color.getValue(time);
	}

	public void setColor(int time, Color3 color) {
		this.color.setValue(time, color);
	}

	public Color3 getColorFade(int time) {
		return colorFade.getValue(time);
	}

	public void setColorFade(int time, Color3 colorFade) {
		this.colorFade.setValue(time, colorFade);
	}

	public boolean isGradient(int time) {
		return gradient.getValue(time);
	}

	public void setGradient(int time, boolean gradient) {
		this.gradient.setValue(time, gradient);
	}

	public int getPointCount(int time) {
		return pointCount.getValue(time);
	}

	public void setPointCount(int time, int pointCount) {
		this.pointCount.setValue(time, pointCount);
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

		Vec3 pos1 = positionMtx.mult(getStart(time));
		Vec3 pos2 = positionMtx.mult(getEnd(time));
		Vec3 col = colorMtx.mult(getColor(time));
		Vec3 delta = pos2.sub(pos1);

		int count = getPointCount(time);

		if(count <= 2) {
			int rep = getRepetition(time);
			boolean con = isConnected(time);

			List<Point3D> points = new ArrayList<>();
			Vec3[] positions = new Vec3[] { pos1, pos2 };
			for(Vec3 pos : positions) {
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
		} else {
			Vec3 step = delta.scale(1.0 / (count - 1.0));
			Vec3 endColor = colorMtx.mult(getColorFade(time));
			Vec3 colorDelta = endColor.sub(col);
			Vec3 colorStep = colorDelta.scale(1.0 / (count - 1.0));
			boolean useGradient = isGradient(time);

			int rep = getRepetition(time);
			boolean con = isConnected(time);

			List<Point3D> points = new ArrayList<>();
			for(int i = 0; i < count; i++) {
				Vec3 pos = pos1.add(step.scale(i));
				if(!con) {
					points.add(new Point3D(pos, new Vec3(0, 0, 0)));
				}
				for(int j = 0; j < rep; j++) {
					if(useGradient) {
						Vec3 pointColor = col.add(colorStep.scale(i));
						points.add(new Point3D(pos, pointColor));
					} else {
						points.add(new Point3D(pos, col));
					}
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
		copy(node);
		return node;
	}
}
