package com.unknown.emulight.lcp.laser.node;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.unknown.emulight.lcp.laser.LaserPart;
import com.unknown.math.g3d.Mtx44;

public class GroupNode extends Node {
	public static final String TYPE = "group";

	private final List<Node> children = new CopyOnWriteArrayList<>();

	protected GroupNode(String type) {
		super(type, false);
	}

	public GroupNode() {
		super(TYPE, false);
	}

	public GroupNode(LaserPart clip) {
		super(TYPE, false);
		setClip(clip);
	}

	@Override
	public List<Node> getChildren() {
		return Collections.unmodifiableList(children);
	}

	@Override
	public void addChild(Node node) {
		children.add(node);
		node.setParent(this);
	}

	@Override
	public void addChild(Node node, int index) {
		children.add(index, node);
		node.setParent(this);
	}

	@Override
	public void removeChild(Node node) {
		children.remove(node);
		node.setParent(null);
	}

	protected List<Shape> render(List<Shape> result, int time, Mtx44 positionTransform, Mtx44 colorTransform,
			boolean applyPositionTransform, boolean applyColorTransform) {
		Mtx44 positionMtx;
		Mtx44 colorMtx;

		if(applyPositionTransform) {
			positionMtx = positionTransform.concat(getTransformation(time));
		} else {
			positionMtx = positionTransform;
		}

		if(applyColorTransform) {
			colorMtx = colorTransform.concat(getColorTransformation(time));
		} else {
			colorMtx = colorTransform;
		}

		for(Node node : children) {
			if(node.isEnabled(time)) {
				node.render(result, time, positionMtx, colorMtx);
			}
		}

		return result;
	}

	@Override
	protected List<Shape> render(List<Shape> result, int time, Mtx44 positionTransform, Mtx44 colorTransform) {
		return render(result, time, positionTransform, colorTransform, true, true);
	}

	@Override
	public GroupNode clone() {
		GroupNode node = new GroupNode();
		copy(node);
		for(Node n : children) {
			node.addChild(n.clone());
		}
		return node;
	}
}
