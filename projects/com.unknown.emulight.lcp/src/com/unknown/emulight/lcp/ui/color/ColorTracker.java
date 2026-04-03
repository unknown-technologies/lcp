package com.unknown.emulight.lcp.ui.color;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;

import javax.swing.JColorChooser;

@SuppressWarnings("serial")
public class ColorTracker implements ActionListener, Serializable {
	JColorChooser chooser;
	Color color;

	public ColorTracker(JColorChooser c) {
		chooser = c;
	}

	public void actionPerformed(ActionEvent e) {
		color = chooser.getColor();
	}

	public Color getColor() {
		return color;
	}
}
