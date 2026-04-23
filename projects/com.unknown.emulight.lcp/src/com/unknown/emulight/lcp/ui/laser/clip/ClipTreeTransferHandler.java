package com.unknown.emulight.lcp.ui.laser.clip;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.TreePath;

import com.unknown.emulight.lcp.laser.node.Node;
import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;

@SuppressWarnings("serial")
public class ClipTreeTransferHandler extends TransferHandler {
	private static final Logger log = Trace.create(ClipTreeTransferHandler.class);

	private final DataFlavor nodesFlavor;
	private final DataFlavor[] flavors;

	public ClipTreeTransferHandler() {
		String mimeType = DataFlavor.javaJVMLocalObjectMimeType + ";class=\"" + Node[].class.getName() + "\"";
		nodesFlavor = new DataFlavor(mimeType, "Node");
		flavors = new DataFlavor[] { nodesFlavor };
	}

	@Override
	public boolean canImport(TransferSupport support) {
		if(!support.isDrop()) {
			return false;
		}

		support.setShowDropLocation(true);
		if(!support.isDataFlavorSupported(nodesFlavor)) {
			return false;
		}

		// do not allow a drop on the drag source selections
		JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
		JTree tree = (JTree) support.getComponent();
		TreePath path = dl.getPath();

		ClipTreeNode target = (ClipTreeNode) path.getLastPathComponent();
		if(!target.getAllowsChildren()) {
			return false;
		}

		// if the source and destination trees are different, everything is fine
		Node[] nodes = null;
		try {
			Transferable t = support.getTransferable();
			nodes = (Node[]) t.getTransferData(nodesFlavor);
		} catch(UnsupportedFlavorException e) {
			log.log(Levels.WARNING, "Unsupported flavor: " + e.getMessage());
			return false;
		} catch(IOException e) {
			log.log(Levels.WARNING, "I/O error: " + e.getMessage(), e);
			return false;
		}

		if(nodes.length == 0) {
			return false;
		}

		ClipTreeModel model = (ClipTreeModel) tree.getModel();
		Node root = nodes[0].getRootNode();
		if(model.getRoot().getNode() != root) {
			return true;
		}

		// TODO: make sure this also works across different tree views which operate on the same model
		int dropRow = tree.getRowForPath(path);
		int[] selRows = tree.getSelectionRows();
		for(int i = 0; i < selRows.length; i++) {
			if(selRows[i] == dropRow) {
				return false;
			}

			ClipTreeNode treeNode = (ClipTreeNode) tree.getPathForRow(selRows[i]).getLastPathComponent();
			if(treeNode.getPath().isDescendant(path)) {
				return false;
			}
		}

		return true;
	}

	@Override
	protected Transferable createTransferable(JComponent c) {
		JTree tree = (JTree) c;
		TreePath[] paths = tree.getSelectionPaths();

		if(paths == null) {
			return null;
		}

		Node[] nodes = new Node[paths.length];
		for(int i = 0; i < paths.length; i++) {
			ClipTreeNode node = (ClipTreeNode) paths[i].getLastPathComponent();
			nodes[i] = node.getNode().clone();
		}

		return new NodesTransferable(paths, nodes);
	}

	@Override
	protected void exportDone(JComponent source, Transferable data, int action) {
		NodesTransferable transfer = (NodesTransferable) data;

		if((action & MOVE) == MOVE) {
			JTree tree = (JTree) source;
			ClipTreeModel model = (ClipTreeModel) tree.getModel();
			for(TreePath src : transfer.getPaths()) {
				model.remove(src);
			}
		}
	}

	@Override
	public int getSourceActions(JComponent c) {
		return COPY_OR_MOVE;
	}

	@Override
	public boolean importData(TransferSupport support) {
		if(!canImport(support)) {
			return false;
		}

		Node[] nodes = null;
		try {
			Transferable t = support.getTransferable();
			nodes = (Node[]) t.getTransferData(nodesFlavor);
		} catch(UnsupportedFlavorException e) {
			log.log(Levels.WARNING, "Unsupported flavor: " + e.getMessage());
			return false;
		} catch(IOException e) {
			log.log(Levels.WARNING, "I/O error: " + e.getMessage(), e);
			return false;
		}

		JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
		int childIndex = dl.getChildIndex();
		TreePath dest = dl.getPath();
		ClipTreeNode parent = (ClipTreeNode) dest.getLastPathComponent();
		JTree tree = (JTree) support.getComponent();
		ClipTreeModel model = (ClipTreeModel) tree.getModel();

		int index = childIndex; // DropMode.INSERT
		if(childIndex == -1) {  // DropMode.ON
			index = parent.getChildCount();
		}

		for(Node source : nodes) {
			model.insert(dest, source, index++);
		}

		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	public class NodesTransferable implements Transferable {
		private final TreePath[] paths;
		private final Node[] nodes;

		public NodesTransferable(TreePath[] paths, Node[] nodes) {
			this.paths = paths;
			this.nodes = nodes;
		}

		private TreePath[] getPaths() {
			return paths;
		}

		@Override
		public Node[] getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
			if(!isDataFlavorSupported(flavor)) {
				throw new UnsupportedFlavorException(flavor);
			}
			return nodes;
		}

		@Override
		public DataFlavor[] getTransferDataFlavors() {
			return flavors;
		}

		@Override
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return nodesFlavor.equals(flavor);
		}
	}
}
