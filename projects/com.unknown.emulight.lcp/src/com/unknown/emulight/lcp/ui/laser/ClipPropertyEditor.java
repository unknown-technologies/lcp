package com.unknown.emulight.lcp.ui.laser;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.unknown.emulight.lcp.laser.node.CachedImage;
import com.unknown.emulight.lcp.laser.node.Color3;
import com.unknown.emulight.lcp.laser.node.GroupNode;
import com.unknown.emulight.lcp.laser.node.Node;
import com.unknown.emulight.lcp.laser.node.Property;
import com.unknown.emulight.lcp.project.Project;
import com.unknown.emulight.lcp.ui.UIUtils;
import com.unknown.math.g3d.Vec3;
import com.unknown.util.ui.LabeledPairLayout;
import com.unknown.util.ui.MessageBox;
import com.unknown.util.ui.SimpleDocumentListener;

@SuppressWarnings("serial")
public class ClipPropertyEditor extends JPanel {
	private final Callback updated;
	private Node node;

	private int time;

	private List<Updater> updater = new ArrayList<>();
	private boolean bypassEvents = false;
	private boolean inCallback = false;

	private final JPanel props;

	private final Project project;

	private ClipPropertyAutomationEditor automationEditor;

	public ClipPropertyEditor(Project project, Callback updated, ClipPropertyAutomationEditor automationEditor) {
		super(new BorderLayout());
		this.project = project;
		this.updated = updated;

		props = new JPanel(new LabeledPairLayout());
		props.setBorder(UIUtils.padding());
		add(BorderLayout.CENTER, new JScrollPane(props));
		this.automationEditor = automationEditor;
	}

	public void setNode(Node node) {
		this.node = node;

		props.removeAll();
		updater.clear();

		for(Property<?> prop : node.getProperties()) {
			JComponent component;
			if(prop.getType().equals(String.class)) {
				@SuppressWarnings("unchecked")
				Property<String> p = (Property<String>) prop;
				String value = p.getValue(time);
				if(value == null) {
					value = "(unnamed)";
				}
				JTextField text = new JTextField(value);
				text.getDocument().addDocumentListener(new SimpleDocumentListener() {
					@Override
					public void update(DocumentEvent e) {
						if(!bypassEvents) {
							p.setValue(time, text.getText());
							callback();
						}
					}
				});
				updater.add(() -> {
					try {
						bypassEvents = true;
						text.setText(p.getValue(time));
					} finally {
						bypassEvents = false;
					}
				});
				component = text;
			} else if(prop.getType().equals(Boolean.class)) {
				@SuppressWarnings("unchecked")
				Property<Boolean> p = (Property<Boolean>) prop;
				JCheckBox checkbox = new JCheckBox();
				checkbox.setSelected(p.getValue(time));
				checkbox.addChangeListener(e -> {
					if(!bypassEvents) {
						p.setValue(time, checkbox.isSelected());
						callback();
					}
				});
				updater.add(() -> {
					try {
						bypassEvents = true;
						checkbox.setSelected(p.getValue(time));
					} finally {
						bypassEvents = false;
					}
				});
				component = checkbox;
			} else if(prop.getType().equals(Integer.class)) {
				@SuppressWarnings("unchecked")
				Property<Integer> p = (Property<Integer>) prop;
				JSpinner number = new JSpinner(
						new SpinnerNumberModel((int) p.getValue(time), (int) p.getMinimum(),
								(int) p.getMaximum(), 1));
				number.setToolTipText("Integer value (" + p.getMinimum() + " to " + p.getMaximum() +
						")");
				number.addChangeListener(e -> {
					if(!bypassEvents) {
						p.setValue(time, (int) number.getValue());
						callback();
					}
				});
				updater.add(() -> {
					try {
						bypassEvents = true;
						number.setValue(p.getValue(time));
					} finally {
						bypassEvents = false;
					}
				});
				component = number;
			} else if(prop.getType().equals(Double.class)) {
				@SuppressWarnings("unchecked")
				Property<Double> p = (Property<Double>) prop;
				JSpinner number = new JSpinner(
						new SpinnerNumberModel((double) p.getValue(time),
								(double) p.getMinimum(), (double) p.getMaximum(), 0.1));
				number.setToolTipText("Floating point value (" + p.getMinimum() + " to " +
						p.getMaximum() + ")");
				number.addChangeListener(e -> {
					if(!bypassEvents) {
						p.setValue(time, (double) number.getValue());
						callback();
					}
				});
				updater.add(() -> {
					try {
						bypassEvents = true;
						number.setValue(p.getValue(time));
					} finally {
						bypassEvents = false;
					}
				});
				component = number;
			} else if(prop.getType().equals(Vec3.class)) {
				@SuppressWarnings("unchecked")
				Property<Vec3> p = (Property<Vec3>) prop;
				Vec3 vec = p.getValue(time);
				Vec3 min = p.getMinimum();
				Vec3 max = p.getMaximum();
				JSpinner spinnerX = new JSpinner(new SpinnerNumberModel(vec.x, min.x, max.x, 0.1));
				JSpinner spinnerY = new JSpinner(new SpinnerNumberModel(vec.y, min.y, max.y, 0.1));
				JSpinner spinnerZ = new JSpinner(new SpinnerNumberModel(vec.z, min.z, max.z, 0.1));

				spinnerX.setToolTipText("X component (" + min.x + " to " + max.x + ")");
				spinnerY.setToolTipText("Y component (" + min.y + " to " + max.y + ")");
				spinnerZ.setToolTipText("Z component (" + min.z + " to " + max.z + ")");

				ChangeListener listener = e -> {
					if(!bypassEvents) {
						double x = (double) spinnerX.getValue();
						double y = (double) spinnerY.getValue();
						double z = (double) spinnerZ.getValue();
						p.setValue(time, new Vec3(x, y, z));
						callback();
					}
				};
				spinnerX.addChangeListener(listener);
				spinnerY.addChangeListener(listener);
				spinnerZ.addChangeListener(listener);

				updater.add(() -> {
					try {
						bypassEvents = true;
						Vec3 v = p.getValue(time);
						spinnerX.setValue(v.x);
						spinnerY.setValue(v.y);
						spinnerZ.setValue(v.z);
					} finally {
						bypassEvents = false;
					}
				});

				JPanel panel = new JPanel(new GridLayout(1, 3));
				panel.add(spinnerX);
				panel.add(spinnerY);
				panel.add(spinnerZ);

				component = panel;
			} else if(prop.getType().equals(Color3.class)) {
				@SuppressWarnings("unchecked")
				Property<Color3> p = (Property<Color3>) prop;
				Color3 vec = p.getValue(time);
				JSpinner spinnerR = new JSpinner(new SpinnerNumberModel(vec.getRed(), 0.0, 1.0, 0.1));
				JSpinner spinnerG = new JSpinner(new SpinnerNumberModel(vec.getGreen(), 0.0, 1.0, 0.1));
				JSpinner spinnerB = new JSpinner(new SpinnerNumberModel(vec.getBlue(), 0.0, 1.0, 0.1));

				spinnerR.setToolTipText("Red component (0.0 to 1.0)");
				spinnerG.setToolTipText("Green component (0.0 to 1.0)");
				spinnerB.setToolTipText("Blue component (0.0 to 1.0)");

				JComponent colorBox = new JComponent() {
					@Override
					protected void paintComponent(Graphics g) {
						super.paintComponent(g);
						double red = (double) spinnerR.getValue();
						double green = (double) spinnerG.getValue();
						double blue = (double) spinnerB.getValue();
						Color3 color = new Color3(red, green, blue);
						g.setColor(color.getColor());
						g.fillRect(0, 0, getWidth(), getHeight());
					}
				};
				colorBox.setToolTipText("Click to open color chooser");
				colorBox.setMinimumSize(new Dimension(22, 22));
				colorBox.setPreferredSize(new Dimension(22, 22));
				colorBox.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
				colorBox.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						Color color = UIUtils.showColorChooser(ClipPropertyEditor.this,
								"Color chooser...", colorBox.getBackground(),
								project.getPalette(), getColors(node), project);
						if(color != null) {
							Color3 col = new Color3(color);
							colorBox.setBackground(color);
							spinnerR.setValue(col.getRed());
							spinnerG.setValue(col.getGreen());
							spinnerB.setValue(col.getBlue());
						}
					}
				});

				ChangeListener listener = e -> {
					if(!bypassEvents) {
						double r = (double) spinnerR.getValue();
						double g = (double) spinnerG.getValue();
						double b = (double) spinnerB.getValue();
						Color3 color = new Color3(r, g, b);
						p.setValue(time, color);
						colorBox.setBackground(color.getColor());
						callback();
					}
				};
				spinnerR.addChangeListener(listener);
				spinnerG.addChangeListener(listener);
				spinnerB.addChangeListener(listener);

				updater.add(() -> {
					try {
						bypassEvents = true;
						Color3 color = p.getValue(time);
						spinnerR.setValue(color.getRed());
						spinnerG.setValue(color.getGreen());
						spinnerB.setValue(color.getBlue());
						colorBox.setBackground(color.getColor());
					} finally {
						bypassEvents = false;
					}
				});

				JPanel controls = new JPanel(new GridLayout(1, 3));
				controls.add(spinnerR);
				controls.add(spinnerG);
				controls.add(spinnerB);

				JPanel panel = new JPanel(new BorderLayout());
				panel.add(BorderLayout.WEST, colorBox);
				panel.add(BorderLayout.CENTER, controls);

				component = panel;
			} else if(prop.getType().equals(CachedImage.class)) {
				@SuppressWarnings("unchecked")
				Property<CachedImage> p = (Property<CachedImage>) prop;

				CachedImage img = p.getValue(time);

				JTextField path = new JTextField();
				path.setEditable(false);

				if(img != null) {
					path.setText(img.getFile().toString());
				} else {
					path.setText("(none)");
					path.setEnabled(false);
				}

				JButton load = new JButton("Load");
				load.addActionListener(e -> {
					CachedImage image = p.getValue(time);
					File file;
					if(image != null) {
						file = image.getFile();
					} else {
						file = null;
					}

					JFileChooser chooser = new JFileChooser(file);
					FileNameExtensionFilter filter = new FileNameExtensionFilter("Images", "jpg",
							"jpeg", "png", "gif");
					chooser.setFileFilter(filter);
					chooser.showOpenDialog(this);

					File selected = chooser.getSelectedFile();
					if(selected != null) {
						try {
							CachedImage newimg = new CachedImage(selected);
							p.setValue(time, newimg);
							path.setEnabled(true);
							path.setText(selected.toString());
						} catch(IOException ex) {
							MessageBox.showError(this,
									"Failed to load file: " + ex.getMessage());
						}
					}
				});
				Dimension size = load.getPreferredSize();
				Dimension sz = new Dimension(size.width, 22);
				load.setSize(sz);
				load.setPreferredSize(sz);
				load.setMaximumSize(sz);

				JPanel panel = new JPanel(new BorderLayout());
				panel.add(BorderLayout.CENTER, path);
				panel.add(BorderLayout.EAST, load);

				component = panel;
			} else {
				component = new JLabel();
			}

			JCheckBox enableAutomation = new JCheckBox();
			enableAutomation.setToolTipText("Enable/Disable animation");
			enableAutomation.setEnabled(!prop.isStatic());
			enableAutomation.setSelected(prop.isAutomation() && !prop.isStatic());
			enableAutomation.addChangeListener(e -> {
				if(!bypassEvents) {
					prop.setAutomation(enableAutomation.isSelected());
					callback();
				}
			});

			updater.add(() -> {
				try {
					bypassEvents = true;
					enableAutomation.setSelected(prop.isAutomation() && !prop.isStatic());
				} finally {
					bypassEvents = false;
				}
			});

			JButton showAutomation = new JButton("...");
			showAutomation.setToolTipText("Show automation editor");
			showAutomation.setEnabled(!prop.isStatic());
			showAutomation.setMinimumSize(new Dimension(22, 22));
			showAutomation.setPreferredSize(new Dimension(22, 22));
			showAutomation.addActionListener(e -> {
				automationEditor.setProperty(prop);
				automationEditor.setVisible(true);
				automationEditor.toFront();
			});

			JPanel automation = new JPanel(new GridLayout(1, 2));
			automation.add(enableAutomation);
			automation.add(showAutomation);

			JPanel panel = new JPanel(new BorderLayout());
			panel.add(BorderLayout.CENTER, component);
			panel.add(BorderLayout.EAST, automation);

			props.add(LabeledPairLayout.LABEL, new JLabel(prop.getName() + ":"));
			props.add(LabeledPairLayout.COMPONENT, panel);
		}

		props.revalidate();
		repaint();
	}

	private void callback() {
		try {
			inCallback = true;
			updated.callback();
			automationEditor.update();
		} finally {
			inCallback = false;
		}
	}

	public Node getNode() {
		return node;
	}

	public void update() {
		if(inCallback) {
			return;
		}

		for(Updater u : updater) {
			u.update();
		}
	}

	public void setTime(int time) {
		this.time = time;
		update();
	}

	@FunctionalInterface
	private interface Updater {
		void update();
	}

	@SuppressWarnings("unchecked")
	private static void addColors(Set<Color> colors, Node node) {
		for(Property<?> property : node.getProperties()) {
			if(property.getType().equals(Color3.class)) {
				Property<Color3> prop = (Property<Color3>) property;
				for(Color3 color : prop.getValues().values()) {
					colors.add(color.getColor());
				}
			}
		}
		if(node instanceof GroupNode) {
			for(Node n : node.getChildren()) {
				addColors(colors, n);
			}
		}
	}

	private static Color[] getColors(Node node) {
		Set<Color> colors = new HashSet<>();
		addColors(colors, node.getRootNode());
		Color[] result = colors.toArray(new Color[colors.size()]);
		Arrays.sort(result, (a, b) -> Integer.compareUnsigned(a.getRGB() & 0xFFFFFF, b.getRGB() & 0xFFFFFF));
		return result;
	}
}
