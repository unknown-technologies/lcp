package com.unknown.emulight.lcp.laser.node;

import java.util.ArrayList;
import java.util.List;

import com.unknown.emulight.lcp.laser.Point3D;

public class Shape {
	private final Node source;
	private final List<Point3D> points;

	public Shape(Node source) {
		this.source = source;
		points = new ArrayList<>();
	}

	public Shape(Node source, List<Point3D> points) {
		this.source = source;
		this.points = points;
	}

	public Node getSource() {
		return source;
	}

	public List<Point3D> getPoints() {
		return points;
	}
}
