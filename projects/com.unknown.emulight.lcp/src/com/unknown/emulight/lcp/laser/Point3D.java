package com.unknown.emulight.lcp.laser;

import com.unknown.math.g3d.Mtx44;
import com.unknown.math.g3d.Vec3;

public class Point3D {
	private Vec3 position;
	private Vec3 color;

	public Point3D() {
		position = new Vec3(0, 0, 0);
		color = new Vec3(0, 0, 0);
	}

	public Point3D(Vec3 position, Vec3 color) {
		this.position = position;
		this.color = color;
	}

	public Vec3 getPosition() {
		return position;
	}

	public void setPosition(Vec3 position) {
		this.position = position;
	}

	public Vec3 getColor() {
		return color;
	}

	public void setColor(Vec3 color) {
		this.color = color;
	}

	public Point3D transform(Mtx44 positionTransform, Mtx44 colorTransform) {
		return new Point3D(positionTransform.mult(position), colorTransform.mult(color));
	}
}
