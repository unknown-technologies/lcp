package com.unknown.emulight.lcp.ui.project;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.unknown.emulight.lcp.project.PartContainer;
import com.unknown.emulight.lcp.project.Project;

@SuppressWarnings("serial")
public class ProjectEditor extends JPanel {
	private PartContainer<?> selectedPart;

	public ProjectEditor(Project project) {
		super(new BorderLayout());

		ProjectView view = new ProjectView(project);

		JTextField name = new JTextField();
		name.setText("");
		name.setEnabled(false);
		Dimension nameSize = name.getMinimumSize();
		Dimension minsz = new Dimension(100, nameSize.height);
		name.setMinimumSize(minsz);
		name.setPreferredSize(minsz);
		name.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void changedUpdate(DocumentEvent e) {
				update();
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				update();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				update();
			}

			private void update() {
				if(selectedPart == null) {
					return;
				}
				String s = name.getText().trim();
				if(s.length() == 0) {
					selectedPart.getPart().setName(null);
				} else {
					selectedPart.getPart().setName(s);
				}
				view.repaint();
			}
		});

		JSpinner start = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, project.getPPQ()));
		start.setEnabled(false);
		start.addChangeListener(e -> {
			int time = (int) start.getValue();
			if(selectedPart != null && selectedPart.getTime() != time) {
				view.setSelectedPart(selectedPart.move(time));
			}
		});

		JSpinner length = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, project.getPPQ()));
		length.setEnabled(false);
		length.addChangeListener(e -> {
			int len = (int) length.getValue();
			if(selectedPart != null) {
				selectedPart.setLength(len);
				view.repaint();
			}
		});

		JPanel partProperties = new JPanel(new FlowLayout(FlowLayout.LEFT));
		partProperties.add(new JLabel("Name:"));
		partProperties.add(name);
		partProperties.add(new JLabel("Start:"));
		partProperties.add(start);
		partProperties.add(new JLabel("Length:"));
		partProperties.add(length);

		view.addPartSelectionListener(part -> {
			selectedPart = part;
			if(part != null) {
				String s = part.getPart().getName();
				name.setText(s == null ? "" : s);
				name.setEnabled(true);
				start.setValue((int) selectedPart.getTime());
				length.setValue((int) selectedPart.getLength());
				start.setEnabled(true);
				length.setEnabled(true);
			} else {
				name.setText("");
				name.setEnabled(false);
				start.setValue(0);
				length.setValue(0);
				start.setEnabled(false);
				length.setEnabled(false);
			}
		});

		add(BorderLayout.NORTH, partProperties);
		add(BorderLayout.CENTER, view);
	}
}
