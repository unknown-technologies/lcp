package com.unknown.emulight.lcp.laser.node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.unknown.emulight.lcp.laser.Clip;
import com.unknown.math.g3d.Mtx44;

public class GroupNode extends Node {
	private final List<Node> children = new ArrayList<>();

	public GroupNode() {
		super(false);
	}

	public GroupNode(Clip clip) {
		super(false);
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

	@Override
	protected List<Shape> render(List<Shape> result, Mtx44 positionTransform, Mtx44 colorTransform) {
		Mtx44 positionMtx = positionTransform.concat(getTransformation());
		Mtx44 colorMtx = colorTransform.concat(getColorTransformation());

		for(Node node : children) {
			node.render(result, positionMtx, colorMtx);
		}

		return result;
	}

	@Override
	public GroupNode clone() {
		GroupNode node = new GroupNode();
		node.copyFrom(this);
		for(Node n : children) {
			node.addChild(n.clone());
		}
		return node;
	}
}
