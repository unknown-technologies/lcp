package com.unknown.emulight.lcp.laser.ui;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import com.unknown.emulight.lcp.laser.Project;

@SuppressWarnings("serial")
public class TimelineEditor extends JPanel {
	private Project project;

	public TimelineEditor(Project project) {
		super(new BorderLayout());

		this.project = project;
	}

	public Project getProject() {
		return project;
	}
}
