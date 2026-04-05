package com.unknown.emulight.lcp.ui.help;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.StyleSheet;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

import com.unknown.emulight.lcp.ui.resources.icons.Icons;
import com.unknown.util.HexFormatter;
import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;
import com.unknown.util.ui.ExtendedHTMLEditorKit;

@SuppressWarnings("serial")
public class HelpBrowser extends JDialog {
	private static final Logger log = Trace.create(HelpBrowser.class);

	private static final int MENU_MODIFIER = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

	private Help help;
	private JTree tree;
	private JTextPane htmlview;

	public HelpBrowser(JFrame parent) {
		super(parent, "Help");

		setIconImages(List.of(Icons.get(Icons.HELP_BOOK, 16).getImage(),
				Icons.get(Icons.HELP_BOOK, 32).getImage(), Icons.get(Icons.HELP_BOOK, 48).getImage()));

		setLayout(new BorderLayout());

		ImageIcon bookIcon = Icons.get(Icons.BOOK, 16);
		ImageIcon bookOpenIcon = Icons.get(Icons.BOOK_OPEN, 16);
		ImageIcon pageIcon = Icons.get(Icons.HELP_SHEET, 16);

		help = new Help();
		tree = new JTree(help);
		tree.setCellRenderer(new DefaultTreeCellRenderer() {
			@Override
			public Component getTreeCellRendererComponent(JTree t, Object value, boolean isSelected,
					boolean expanded, boolean leaf, int row, boolean focus) {
				super.getTreeCellRendererComponent(t, value, isSelected, expanded, leaf, row, focus);
				if(!leaf) {
					if(expanded) {
						setIcon(bookOpenIcon);
					} else {
						setIcon(bookIcon);
					}
				} else {
					setIcon(pageIcon);
				}
				return this;
			}
		});

		Color background = UIManager.getColor("TextField.background");
		String bgcolor = "#" + HexFormatter.tohex(background.getRGB() & 0xFFFFFF, 6);

		ExtendedHTMLEditorKit kit = new ExtendedHTMLEditorKit();
		StyleSheet defaultStyle = kit.getStyleSheet();
		StyleSheet style = new StyleSheet();
		style.addStyleSheet(defaultStyle);
		style.addRule("table { border: 1px black solid; background-color: black; }");
		style.addRule("td, th { background-color: " + bgcolor + "; }");
		style.addRule("th { font-weight: bold; }");
		style.addRule("code { background-color: #cccccc; }");
		style.addRule("td, th, h1, h2, h3, h4, p { font-family: \"SansSerif\"; }");
		kit.setStyleSheet(style);

		htmlview = new JTextPane();
		htmlview.setEditorKit(kit);
		htmlview.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
		htmlview.setBackground(background);
		htmlview.setEditable(false);
		htmlview.setContentType("text/html");

		htmlview.addHyperlinkListener(new HyperlinkListener() {
			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if(e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
					URL url = e.getURL();
					if(url.toString().startsWith("http://") ||
							url.toString().startsWith("https://")) {
						Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop()
								: null;
						if(desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
							try {
								desktop.browse(url.toURI());
							} catch(Exception ex) {
								log.log(Levels.ERROR, "Failed to launch browser: " +
										ex.getMessage(), ex);
							}
						}
					} else {
						try {
							htmlview.setPage(url);
						} catch(IOException ex) {
							log.log(Levels.ERROR, "Failed to navigate to " + url + ": " +
									ex.getMessage(), ex);
						}
					}
				}
			}
		});

		tree.addTreeSelectionListener(e -> {
			TreePath path = e.getPath();
			HelpNode node = (HelpNode) path.getLastPathComponent();
			if(node instanceof Page) {
				Page page = (Page) node;
				try {
					htmlview.setPage(page.getURL());
				} catch(IOException ex) {
					log.log(Levels.ERROR, "Failed to navigate to " + page.getURL() + ": " +
							ex.getMessage(), ex);
				}
			} else if(node instanceof Category) {
				Category cat = (Category) node;
				if(cat.getURL() != null) {
					try {
						htmlview.setPage(cat.getURL());
					} catch(IOException ex) {
						log.log(Levels.ERROR, "Failed to navigate to " + cat.getURL() + ": " +
								ex.getMessage(), ex);
					}
				}
			}
		});

		tree.setSelectionPath(new TreePath(help.getRoot()));

		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		split.setLeftComponent(new JScrollPane(tree));
		split.setRightComponent(new JScrollPane(htmlview));
		split.setResizeWeight(0.1);

		add(BorderLayout.CENTER, split);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setSize(640, 480);

		JRootPane root = getRootPane();
		KeyStroke quitKey = KeyStroke.getKeyStroke(KeyEvent.VK_Q, MENU_MODIFIER);
		KeyStroke escKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		Object action = new Object();
		root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(quitKey, action);
		root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escKey, action);
		root.getActionMap().put(action, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
	}
}
