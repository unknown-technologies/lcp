package com.unknown.emulight.lcp.ui.laser.clip;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import com.unknown.emulight.lcp.laser.node.Node;

public class ClipTreeNode implements TreeNode {
	private final ClipTreeNode parent;
	private final Node node;

	public ClipTreeNode(ClipTreeNode other) {
		this(other.parent, other.node);
	}

	public ClipTreeNode(Node node) {
		this(null, node);
	}

	public ClipTreeNode(ClipTreeNode parent, Node node) {
		this.parent = parent;
		this.node = node;
	}

	public ClipTreeNode rename(String name) {
		node.setName(name);
		return new ClipTreeNode(parent, node);
	}

	public Node getNode() {
		return node;
	}

	public TreePath getPath() {
		List<ClipTreeNode> path = new ArrayList<>();
		for(ClipTreeNode n = this; n != null; n = n.getParent()) {
			path.add(n);
		}
		Collections.reverse(path);
		return new TreePath(path.toArray(new ClipTreeNode[path.size()]));
	}

	@Override
	public String toString() {
		String name = node.getName();
		if(name != null) {
			return name;
		} else {
			return "(unnamed)";
		}
	}

	@Override
	public Enumeration<ClipTreeNode> children() {
		return new Enumeration<>() {
			private Iterator<Node> nodes = node.getChildren().iterator();

			@Override
			public boolean hasMoreElements() {
				return nodes.hasNext();
			}

			@Override
			public ClipTreeNode nextElement() {
				Node n = nodes.next();
				return new ClipTreeNode(ClipTreeNode.this, n);
			}
		};
	}

	@Override
	public boolean getAllowsChildren() {
		return !node.isLeaf();
	}

	@Override
	public ClipTreeNode getChildAt(int index) {
		return new ClipTreeNode(this, node.getChildren().get(index));
	}

	@Override
	public int getChildCount() {
		return node.getChildren().size();
	}

	@Override
	public int getIndex(TreeNode treeNode) {
		if(treeNode instanceof ClipTreeNode) {
			ClipTreeNode n = (ClipTreeNode) treeNode;
			return node.getChildren().indexOf(n.node);
		} else {
			return -1;
		}
	}

	@Override
	public ClipTreeNode getParent() {
		return parent;
	}

	@Override
	public boolean isLeaf() {
		return node.isLeaf();
	}

	@Override
	public boolean equals(Object o) {
		if(o == null || !(o instanceof ClipTreeNode)) {
			return false;
		}

		ClipTreeNode n = (ClipTreeNode) o;

		// reference comparison is intentional
		return n.node == node;
	}

	@Override
	public int hashCode() {
		return node.hashCode();
	}
}
