package com.unknown.emulight.lcp.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import com.unknown.emulight.lcp.project.Project;
import com.unknown.emulight.lcp.ui.resources.icons.Icons;
import com.unknown.util.ui.LabeledPairLayout;

@SuppressWarnings("serial")
public class ProjectSettingsDialog extends JDialog {
	public ProjectSettingsDialog(Project project) {
		super(project.getSystem().getMainWindow(), "Project Settings", true);
		setIconImages(List.of(Icons.getImage(Icons.SETTINGS, 32), Icons.getImage(Icons.SETTINGS, 16)));

		JPanel content = new JPanel(new LabeledPairLayout());

		JTextField name = new JTextField(project.getName());
		name.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				project.setName(name.getText().trim());
			}
		});

		content.setBorder(UIUtils.border("General"));
		content.add(LabeledPairLayout.LABEL, new JLabel("Name:"));
		content.add(LabeledPairLayout.COMPONENT, name);
		content.add(LabeledPairLayout.LABEL, new JLabel("PPQ:"));
		content.add(LabeledPairLayout.COMPONENT, new JLabel(Integer.toString(project.getPPQ())));

		JButton close = new JButton("Close");
		close.addActionListener(e -> dispose());

		JPanel buttons = new JPanel(new FlowLayout());
		buttons.add(close);

		setLayout(new BorderLayout());
		add(BorderLayout.CENTER, content);
		add(BorderLayout.SOUTH, buttons);

		setSize(320, 240);

		KeyStroke escKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		Object quit = new Object();
		JRootPane root = getRootPane();
		root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escKey, quit);
		root.getActionMap().put(quit, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}
}
