package com.unknown.emulight.lcp.laser.node;

import java.util.ArrayList;
import java.util.List;

import com.unknown.emulight.lcp.laser.Point3D;
import com.unknown.math.g3d.Mtx44;
import com.unknown.math.g3d.Vec3;

public class PointNode extends Node {
	private final Property<Vec3> position = new Property<>(StandardPropertyNames.POSITION, new Vec3(0, 0, 0));
	private final Property<Color3> color = new Property<>(StandardPropertyNames.COLOR, new Color3(1, 1, 1));
	private final Property<Integer> repetition = new Property<>(StandardPropertyNames.REPETITION, 1);

	public PointNode() {
		super(true);

		addProperty(position);
		addProperty(color);
		addProperty(repetition);
	}

	public Vec3 getPosition() {
		return position.getValue();
	}

	public void setPosition(Vec3 position) {
		this.position.setValue(position);
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

	@Override
	protected List<Shape> render(List<Shape> result, Mtx44 positionTransform, Mtx44 colorTransform) {
		Mtx44 positionMtx = positionTransform.concat(getTransformation());
		Mtx44 colorMtx = colorTransform.concat(getColorTransformation());

		Vec3 pos = positionMtx.mult(position.getValue());
		Vec3 col = colorMtx.mult(color.getValue());

		int rep = getRepetition();
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
