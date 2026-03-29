package com.unknown.emulight.lcp.ui.laser.clip;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import com.unknown.emulight.lcp.laser.LaserPart;
import com.unknown.emulight.lcp.laser.node.Node;
import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;

public class ClipTreeModel implements TreeModel {
	private static final Logger log = Trace.create(ClipTreeModel.class);

	private List<TreeModelListener> listeners = new ArrayList<>();

	private ClipTreeNode root;

	public void setClip(LaserPart clip) {
		if(clip == null) {
			root = null;
		} else {
			root = new ClipTreeNode(clip.getRoot());
		}
		fireTreeStructureChanged();
	}

	public TreePath getPath(Node node) {
		List<Node> path = new ArrayList<>();
		for(Node n = node; n != null; n = n.getParent()) {
			path.add(n);
		}
		Collections.reverse(path);
		ClipTreeNode[] nodes = new ClipTreeNode[path.size()];
		nodes[0] = root;
		for(int i = 1; i < path.size(); i++) {
			ClipTreeNode parent = nodes[i - 1];
			nodes[i] = new ClipTreeNode(parent, path.get(i));
		}

		return new TreePath(nodes);
	}

	public void fireTreeStructureChanged() {
		fireTreeStructureChanged(new TreePath(getRoot()));
	}

	public void fireTreeStructureChanged(Node node) {
		fireTreeStructureChanged(getPath(node));
	}

	private void fireTreeStructureChanged(TreePath path) {
		TreeModelEvent e = new TreeModelEvent(this, path);
		for(TreeModelListener listener : listeners) {
			try {
				listener.treeStructureChanged(e);
			} catch(Throwable t) {
				log.log(Levels.WARNING, "Tree listener failed: " + t.getMessage(), t);
			}
		}
	}

	public void fireTreeNodeChanged(Node node) {
		TreeModelEvent e;
		if(node.getParent() == null) {
			e = new TreeModelEvent(this, new TreePath(new ClipTreeNode(node)), null, null);
		} else {
			int[] indices = new int[1];
			ClipTreeNode[] changed = new ClipTreeNode[indices.length];
			TreePath path = getPath(node);
			changed[0] = new ClipTreeNode((ClipTreeNode) path.getParentPath().getLastPathComponent(), node);
			indices[0] = changed[0].getParent().getIndex(changed[0]);
			e = new TreeModelEvent(this, path.getParentPath(), indices, changed);
		}
		for(TreeModelListener listener : listeners) {
			try {
				listener.treeNodesChanged(e);
			} catch(Throwable t) {
				log.log(Levels.WARNING, "Tree listener failed: " + t.getMessage(), t);
			}
		}
	}

	public void fireTreeNodeInserted(Node node) {
		TreePath path = getPath(node);
		int index = node.getParent().getChildren().indexOf(node);
		TreeModelEvent e = new TreeModelEvent(this, path.getParentPath(), new int[] { index },
				new Object[] { path.getLastPathComponent() });
		for(TreeModelListener listener : listeners) {
			try {
				listener.treeNodesInserted(e);
			} catch(Throwable t) {
				log.log(Levels.WARNING, "Tree listener failed: " + t.getMessage(), t);
			}
		}
	}

	public void fireTreeNodeInserted(Node node, int index) {
		TreePath path = getPath(node);
		TreeModelEvent e = new TreeModelEvent(this, path.getParentPath(), new int[] { index },
				new Object[] { path.getLastPathComponent() });
		for(TreeModelListener listener : listeners) {
			try {
				listener.treeNodesInserted(e);
			} catch(Throwable t) {
				log.log(Levels.WARNING, "Tree listener failed: " + t.getMessage(), t);
			}
		}
	}

	public void fireTreeNodeRemoved(TreePath path, int index) {
		TreeModelEvent e = new TreeModelEvent(this, path.getParentPath(), new int[] { index },
				new Object[] { path.getLastPathComponent() });
		for(TreeModelListener listener : listeners) {
			try {
				listener.treeNodesRemoved(e);
			} catch(Throwable t) {
				log.log(Levels.WARNING, "Tree listener failed: " + t.getMessage(), t);
			}
		}
	}

	public void insert(TreePath path, Node node) {
		ClipTreeNode n = (ClipTreeNode) path.getLastPathComponent();
		n.getNode().addChild(node);
		fireTreeNodeInserted(node);
	}

	public void insert(TreePath path, Node node, int index) {
		ClipTreeNode n = (ClipTreeNode) path.getLastPathComponent();
		n.getNode().addChild(node, index);
		fireTreeNodeInserted(node);
	}

	public void remove(TreePath path) {
		ClipTreeNode node = (ClipTreeNode) path.getLastPathComponent();
		Node parent = node.getParent().getNode();
		int index = node.getParent().getIndex(node);
		parent.removeChild(node.getNode());
		fireTreeNodeRemoved(path, index);
	}

	@Override
	public ClipTreeNode getChild(Object parent, int index) {
		ClipTreeNode node = (ClipTreeNode) parent;
		return node.getChildAt(index);
	}

	@Override
	public int getChildCount(Object parent) {
		ClipTreeNode node = (ClipTreeNode) parent;
		return node.getChildCount();
	}

	@Override
	public int getIndexOfChild(Object parent, Object child) {
		ClipTreeNode node = (ClipTreeNode) parent;
		ClipTreeNode ch = (ClipTreeNode) child;
		return node.getIndex(ch);
	}

	@Override
	public ClipTreeNode getRoot() {
		return root;
	}

	@Override
	public boolean isLeaf(Object node) {
		ClipTreeNode n = (ClipTreeNode) node;
		return n.isLeaf();
	}

	@Override
	public void addTreeModelListener(TreeModelListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeTreeModelListener(TreeModelListener listener) {
		listeners.remove(listener);
	}

	@Override
	public void valueForPathChanged(TreePath path, Object newValue) {
		// TODO Auto-generated method stub
	}
}
