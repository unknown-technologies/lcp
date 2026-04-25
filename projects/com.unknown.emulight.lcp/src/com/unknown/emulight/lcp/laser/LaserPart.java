package com.unknown.emulight.lcp.laser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.unknown.emulight.lcp.laser.node.GroupNode;
import com.unknown.emulight.lcp.laser.node.Node;
import com.unknown.emulight.lcp.project.AbstractPart;
import com.unknown.emulight.lcp.project.Project;
import com.unknown.math.g3d.Mtx44;
import com.unknown.math.g3d.Vec3;
import com.unknown.net.shownet.Point;
import com.unknown.xml.dom.Element;

public class LaserPart extends AbstractPart {
	private final Project project;

	private long duration = 1000;
	private int speed = 1000;
	private boolean loop = false;

	private Node root = new GroupNode(this);

	public LaserPart(Project project) {
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

	@Override
	public long getLength() {
		return duration;
	}

	public void setLength(long duration) {
		this.duration = duration;
	}

	public boolean isLoop() {
		return loop;
	}

	public void setLoop(boolean loop) {
		this.loop = loop;
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

		List<Point3D> points = root.render(time, positionMtx, colorMtx);

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

		if(result.isEmpty()) {
			// empty frame
			result.add(new Point());
		}

		return result;
	}

	@Override
	public void write(Element xml) {
		xml.addAttribute("duration", Long.toUnsignedString(duration));
		xml.addAttribute("speed", Integer.toString(speed));
		xml.addAttribute("loop", loop ? "true" : "false");
		xml.addChild(root.write());
	}

	@Override
	public void read(Element xml) throws IOException {
		try {
			duration = Long.parseLong(xml.getAttribute("duration"));
		} catch(NumberFormatException e) {
			throw new IOException("invalid duration: " + xml.getAttribute("duration"), e);
		}

		try {
			speed = Integer.parseInt(xml.getAttribute("speed"));
		} catch(NumberFormatException e) {
			throw new IOException("invalid speed: " + xml.getAttribute("speed"), e);
		}

		loop = Boolean.parseBoolean(xml.getAttribute("loop"));

		GroupNode newRoot = null;
		for(Element e : xml.getChildren()) {
			switch(e.name) {
			case "node":
				if(newRoot != null) {
					throw new IOException("more than one root node");
				}
				Node node = Node.read(this, e);
				if(!(node instanceof GroupNode)) {
					throw new IOException("not a group node");
				}
				newRoot = (GroupNode) node;
			}
		}

		if(newRoot != null) {
			root = newRoot;
		} else {
			throw new IOException("no root node defined");
		}
	}

	@Override
	public LaserPart clone() {
		LaserPart part = new LaserPart(project);
		copy(part);
		part.setName(getName());
		part.duration = duration;
		part.loop = loop;
		part.speed = speed;
		part.root = root.clone();
		return part;
	}
}
