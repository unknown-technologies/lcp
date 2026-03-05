package com.unknown.emulight.lcp.laser.ui.clip;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.TreePath;

import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;

@SuppressWarnings("serial")
public class ClipTreeTransferHandler extends TransferHandler {
	private static final Logger log = Trace.create(ClipTreeTransferHandler.class);

	private DataFlavor nodesFlavor;
	private DataFlavor[] flavors = new DataFlavor[1];

	public ClipTreeTransferHandler() {
		try {
			String mimeType = DataFlavor.javaJVMLocalObjectMimeType + ";class=\"" +
					TreePath[].class.getName() + "\"";
			nodesFlavor = new DataFlavor(mimeType);
			flavors[0] = nodesFlavor;
		} catch(ClassNotFoundException e) {
			log.log(Levels.ERROR, "ClassNotFound: " + e.getMessage(), e);
		}
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

		return new NodesTransferable(paths);
	}

	@Override
	protected void exportDone(JComponent source, Transferable data, int action) {
		// TODO: properly implement copy vs move
		if((action & MOVE) == MOVE) {
			// JTree tree = (JTree) source;
			// ClipTreeModel model = (ClipTreeModel) tree.getModel();
		}
	}

	@Override
	public int getSourceActions(JComponent c) {
		return MOVE; // TODO: COPY_OR_MOVE
	}

	@Override
	public boolean importData(TransferSupport support) {
		if(!canImport(support)) {
			return false;
		}

		TreePath[] nodes = null;
		try {
			Transferable t = support.getTransferable();
			nodes = (TreePath[]) t.getTransferData(nodesFlavor);
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

		for(TreePath source : nodes) {
			ClipTreeNode node = (ClipTreeNode) source.getLastPathComponent();
			int rmidx = node.getParent() != null ? node.getParent().getIndex(node) : -1;
			if(rmidx >= 0 && rmidx < index) {
				index--;
			}
			model.remove(source);
			model.insert(dest, node.getNode(), index++);
		}

		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	public class NodesTransferable implements Transferable {
		private final TreePath[] nodes;

		public NodesTransferable(TreePath[] nodes) {
			this.nodes = nodes;
		}

		@Override
		public TreePath[] getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
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
