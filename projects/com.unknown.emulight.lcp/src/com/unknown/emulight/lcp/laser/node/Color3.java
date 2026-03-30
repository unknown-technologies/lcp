package com.unknown.emulight.lcp.laser.node;

import java.awt.Color;

import com.unknown.math.g3d.Vec3;

public class Color3 extends Vec3 {
	private static final long serialVersionUID = 1L;

	public Color3(double r, double g, double b) {
		super(r, g, b);
		if(r < 0 || r > 1 || g < 0 || g > 1 || b < 0 || b > 1) {
			throw new IllegalArgumentException("Invalid color: red=" + r + ",green=" + g + ",blue=" + b);
		}
	}

	public Color3(Vec3 vec) {
		this(vec.x, vec.y, vec.z);
	}

	public Color3(Color color) {
		super(color.getRed() / 255.0, color.getGreen() / 255.0, color.getBlue() / 255.0);
	}

	public double getRed() {
		return x;
	}

	public double getGreen() {
		return y;
	}

	public double getBlue() {
		return z;
	}

	public Color getColor() {
		return new Color((float) x, (float) y, (float) z);
	}
}
