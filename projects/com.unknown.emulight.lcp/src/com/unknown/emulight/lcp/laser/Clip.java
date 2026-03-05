package com.unknown.emulight.lcp.laser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.unknown.emulight.lcp.laser.node.GroupNode;
import com.unknown.emulight.lcp.laser.node.Node;
import com.unknown.math.g3d.Mtx44;
import com.unknown.math.g3d.Vec3;
import com.unknown.net.shownet.Point;

public class Clip {
	private final Project project;

	private int duration = 1;
	private int speed = 1000;
	private String name;

	private Node root = new GroupNode(this);

	public Clip(Project project) {
		this.project = project;
		root.setName("root");
	}

	public Project getProject() {
		return project;
	}

	public int getSpeed() {
		return speed;
	}

	public void setSpeed(int speed) {
		this.speed = speed;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public Node getRoot() {
		return root;
	}

	public List<Point> getPoints() {
		return Collections.emptyList();
	}

	private static short clamp(int x) {
		if(x < 0) {
			return 0;
		} else if(x > 0xFFFF) {
			return (short) 0xFFFF;
		} else {
			return (short) x;
		}
	}

	public List<Point> render(int time) {
		return render(time, new Mtx44(), new Mtx44());
	}

	public List<Point> render(int time, Mtx44 projection, Mtx44 color) {
		Mtx44 positionMtx = projection.scaleApply(0x8000, 0x8000, 1).transApply(0x8000, 0x8000, 0);
		Mtx44 colorMtx = color.scaleApply(0xFFFF, 0xFFFF, 0xFFFF);

		List<Point3D> points = root.render(positionMtx, colorMtx);

		List<Point> result = new ArrayList<>();
		for(Point3D point : points) {
			Vec3 pos = point.getPosition();
			Vec3 col = point.getColor();

			Point p = new Point();
			int x = (int) Math.round(pos.x);
			int y = (int) Math.round(pos.y);
			int r = (int) Math.round(col.x);
			int g = (int) Math.round(col.y);
			int b = (int) Math.round(col.z);

			// TODO: limit points by intersecting them with the visible area

			p.x = clamp(x);
			p.y = clamp(y);
			p.red = clamp(r);
			p.green = clamp(g);
			p.blue = clamp(b);

			result.add(p);
		}

		if(result.size() == 0) {
			// empty frame
			result.add(new Point());
		}

		return result;
	}
}
