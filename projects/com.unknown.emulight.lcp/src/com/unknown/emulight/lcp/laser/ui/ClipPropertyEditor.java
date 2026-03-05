package com.unknown.emulight.lcp.laser.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;

import com.unknown.emulight.lcp.laser.node.Color3;
import com.unknown.emulight.lcp.laser.node.Node;
import com.unknown.emulight.lcp.laser.node.Property;
import com.unknown.math.g3d.Vec3;
import com.unknown.util.ui.LabeledPairLayout;
import com.unknown.util.ui.SimpleDocumentListener;

@SuppressWarnings("serial")
public class ClipPropertyEditor extends JPanel {
	private final Callback updated;
	private Node node;

	public ClipPropertyEditor(Callback updated) {
		super(new LabeledPairLayout());
		this.updated = updated;
	}

	public void setNode(Node node) {
		this.node = node;

		removeAll();

		for(Property<?> prop : node.getProperties()) {
			JComponent component;
			if(prop.getType().equals(String.class)) {
				@SuppressWarnings("unchecked")
				Property<String> p = (Property<String>) prop;
				String value = p.getValue();
				if(value == null) {
					value = "(unnamed)";
				}
				JTextField text = new JTextField(value);
				text.getDocument().addDocumentListener(new SimpleDocumentListener() {
					@Override
					public void update(DocumentEvent e) {
						p.setValue(text.getText());
						updated.callback();
					}
				});
				component = text;
			} else if(prop.getType().equals(Boolean.class)) {
				@SuppressWarnings("unchecked")
				Property<Boolean> p = (Property<Boolean>) prop;
				JCheckBox checkbox = new JCheckBox();
				checkbox.setSelected(p.getValue());
				checkbox.addChangeListener(e -> {
					p.setValue(checkbox.isSelected());
					updated.callback();
				});
				component = checkbox;
			} else if(prop.getType().equals(Integer.class)) {
				@SuppressWarnings("unchecked")
				Property<Integer> p = (Property<Integer>) prop;
				JSpinner number = new JSpinner(new SpinnerNumberModel((int) p.getValue(), 0, 1000, 1));
				number.addChangeListener(e -> {
					p.setValue((Integer) number.getValue());
					updated.callback();
				});
				component = number;
			} else if(prop.getType().equals(Double.class)) {
				@SuppressWarnings("unchecked")
				Property<Double> p = (Property<Double>) prop;
				JSpinner number = new JSpinner(
						new SpinnerNumberModel((double) p.getValue(), -1000.0, 1000.0, 0.1));
				number.addChangeListener(e -> {
					p.setValue((Double) number.getValue());
					updated.callback();
				});
				component = number;
			} else if(prop.getType().equals(Vec3.class)) {
				@SuppressWarnings("unchecked")
				Property<Vec3> p = (Property<Vec3>) prop;
				Vec3 vec = p.getValue();
				JSpinner spinnerX = new JSpinner(new SpinnerNumberModel(vec.x, -1.0, 1.0, 0.1));
				JSpinner spinnerY = new JSpinner(new SpinnerNumberModel(vec.y, -1.0, 1.0, 0.1));
				JSpinner spinnerZ = new JSpinner(new SpinnerNumberModel(vec.z, -1.0, 1.0, 0.1));

				ChangeListener listener = e -> {
					double x = (double) spinnerX.getValue();
					double y = (double) spinnerY.getValue();
					double z = (double) spinnerZ.getValue();
					p.setValue(new Vec3(x, y, z));
					updated.callback();
				};
				spinnerX.addChangeListener(listener);
				spinnerY.addChangeListener(listener);
				spinnerZ.addChangeListener(listener);

				JPanel panel = new JPanel(new GridLayout(1, 3));
				panel.add(spinnerX);
				panel.add(spinnerY);
				panel.add(spinnerZ);

				component = panel;
			} else if(prop.getType().equals(Color3.class)) {
				@SuppressWarnings("unchecked")
				Property<Color3> p = (Property<Color3>) prop;
				Color3 vec = p.getValue();
				JSpinner spinnerR = new JSpinner(new SpinnerNumberModel(vec.getRed(), 0.0, 1.0, 0.1));
				JSpinner spinnerG = new JSpinner(new SpinnerNumberModel(vec.getGreen(), 0.0, 1.0, 0.1));
				JSpinner spinnerB = new JSpinner(new SpinnerNumberModel(vec.getBlue(), 0.0, 1.0, 0.1));

				JPanel colorBox = new JPanel();
				colorBox.setBackground(vec.getColor());
				colorBox.setMinimumSize(new Dimension(22, 22));
				colorBox.setPreferredSize(new Dimension(22, 22));
				colorBox.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
				colorBox.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						Color color = JColorChooser.showDialog(ClipPropertyEditor.this,
								"Color chooser...", colorBox.getBackground());
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
					double r = (double) spinnerR.getValue();
					double g = (double) spinnerG.getValue();
					double b = (double) spinnerB.getValue();
					Color3 color = new Color3(r, g, b);
					p.setValue(color);
					colorBox.setBackground(color.getColor());
					updated.callback();
				};
				spinnerR.addChangeListener(listener);
				spinnerG.addChangeListener(listener);
				spinnerB.addChangeListener(listener);

				JPanel controls = new JPanel(new GridLayout(1, 3));
				controls.add(spinnerR);
				controls.add(spinnerG);
				controls.add(spinnerB);

				JPanel panel = new JPanel(new BorderLayout());
				panel.add(BorderLayout.WEST, colorBox);
				panel.add(BorderLayout.CENTER, controls);

				component = panel;
			} else {
				component = new JLabel();
			}
			add(LabeledPairLayout.LABEL, new JLabel(prop.getName() + ":"));
			add(LabeledPairLayout.COMPONENT, component);
		}

		revalidate();
		repaint();
	}

	public Node getNode() {
		return node;
	}
}
