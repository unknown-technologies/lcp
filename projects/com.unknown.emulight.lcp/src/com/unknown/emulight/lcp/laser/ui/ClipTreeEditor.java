package com.unknown.emulight.lcp.laser.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.DropMode;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

import com.unknown.emulight.lcp.laser.Clip;
import com.unknown.emulight.lcp.laser.node.CircleNode;
import com.unknown.emulight.lcp.laser.node.GroupNode;
import com.unknown.emulight.lcp.laser.node.LineNode;
import com.unknown.emulight.lcp.laser.node.Node;
import com.unknown.emulight.lcp.laser.node.PointNode;
import com.unknown.emulight.lcp.laser.ui.clip.ClipTreeModel;
import com.unknown.emulight.lcp.laser.ui.clip.ClipTreeNode;
import com.unknown.emulight.lcp.laser.ui.clip.ClipTreeTransferHandler;

@SuppressWarnings("serial")
public class ClipTreeEditor extends JPanel {
	private final Callback updated;
	private final ClipTreeModel model;

	private Consumer<Node> selector;
	private JTree tree;

	public ClipTreeEditor(Callback updated) {
		super(new BorderLayout());

		this.updated = updated;

		model = new ClipTreeModel();

		tree = new JTree(model);
		tree.setEditable(true);
		DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
		tree.setCellRenderer(renderer);
		tree.setCellEditor(new ClipTreeCellEditor(tree, renderer));
		tree.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "startEditing");
		tree.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteNode");
		tree.getActionMap().put("deleteNode", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				TreePath[] selection = tree.getSelectionPaths();
				if(selection != null) {
					for(TreePath path : selection) {
						if(path.getPathCount() > 1) {
							model.remove(path);
						}
					}
				}
			}
		});

		add(BorderLayout.CENTER, new JScrollPane(tree));

		tree.addTreeSelectionListener(e -> {
			TreePath path = e.getNewLeadSelectionPath();
			if(path != null && selector != null) {
				ClipTreeNode node = (ClipTreeNode) path.getLastPathComponent();
				selector.accept(node.getNode());
			}
		});

		tree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int row = tree.getRowForLocation(e.getX(), e.getY());
				TreePath path = tree.getPathForLocation(e.getX(), e.getY());
				if(row != -1 && e.getClickCount() == 1 && e.getButton() == MouseEvent.BUTTON3) {
					// open context menu
					JMenuItem add = new JMenuItem("Add node");
					add.setMnemonic('A');
					add.addActionListener(ev -> {
						insert(path, new GroupNode());
					});

					JMenuItem remove = new JMenuItem("Remove node");
					remove.setMnemonic('R');
					remove.addActionListener(ev -> {
						remove(path);
					});

					JMenuItem addPoint = new JMenuItem("Add point");
					addPoint.addActionListener(ev -> {
						insert(path, new PointNode());
					});

					JMenuItem addLine = new JMenuItem("Add line");
					addLine.addActionListener(ev -> {
						insert(path, new LineNode());
					});

					JMenuItem addCircle = new JMenuItem("Add circle");
					addCircle.addActionListener(ev -> {
						insert(path, new CircleNode());
					});

					ClipTreeNode node = (ClipTreeNode) path.getLastPathComponent();
					JPopupMenu menu = new JPopupMenu();
					if(!node.isLeaf()) {
						menu.add(add);
					}
					if(path.getPathCount() > 1) {
						menu.add(remove);
					}
					if(!node.isLeaf()) {
						menu.addSeparator();
						menu.add(addPoint);
						menu.add(addLine);
						menu.add(addCircle);
					}

					JMenuItem refresh = new JMenuItem("Refresh");
					refresh.addActionListener(ev -> model.fireTreeStructureChanged());
					menu.addSeparator();
					menu.add(refresh);

					menu.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		});

		tree.setDragEnabled(true);
		tree.setDropMode(DropMode.ON_OR_INSERT);
		tree.setTransferHandler(new ClipTreeTransferHandler());
	}

	public void setClip(Clip clip) {
		model.setClip(clip);

		tree.setSelectionPath(new TreePath(model.getRoot()));
	}

	public void setSelectionListener(Consumer<Node> selector) {
		this.selector = selector;
	}

	public void nodeChanged(Node node) {
		model.fireTreeNodeChanged(node);
		updated.callback();
	}

	public void nodeRemoved(TreePath path, int index) {
		model.fireTreeNodeRemoved(path, index);
		updated.callback();
	}

	public void nodeInserted(Node node) {
		model.fireTreeNodeInserted(node);
		updated.callback();
	}

	public void select(Node node) {
		TreePath path = ClipTreeModel.getPath(node);
		tree.setSelectionPath(path);
		int row = tree.getRowForPath(path);
		tree.expandRow(row);
	}

	public void insert(TreePath path, Node node) {
		model.insert(path, node);
	}

	public void remove(TreePath path) {
		ClipTreeNode node = (ClipTreeNode) path.getLastPathComponent();
		Node parent = node.getParent().getNode();
		model.remove(path);
		if(selector != null) {
			selector.accept(parent);
		}
	}

	private class ClipTreeCellEditor extends DefaultTreeCellEditor {
		private ClipTreeNode node;

		public ClipTreeCellEditor(JTree tree, DefaultTreeCellRenderer renderer) {
			super(tree, renderer);
		}

		@Override
		public Component getTreeCellEditorComponent(@SuppressWarnings("hiding") JTree tree, Object value,
				boolean isSelected, boolean expanded, boolean leaf, int row) {
			Object result = value;
			if(value instanceof ClipTreeNode) {
				node = (ClipTreeNode) value;
				result = node.toString();
			} else {
				node = null;
			}
			return super.getTreeCellEditorComponent(tree, result, isSelected, expanded, leaf, row);
		}

		@Override
		public Object getCellEditorValue() {
			String value = (String) super.getCellEditorValue();
			value = value.trim();
			if(node.toString().equals(value)) {
				return node;
			} else if(value.length() == 0) {
				ClipTreeNode n = node.rename(null);
				updated.callback();
				return n;
			} else {
				ClipTreeNode n = node.rename(value);
				updated.callback();
				return n;
			}
		}
	}
}
