package com.unknown.emulight.lcp.ui;

import java.awt.Dimension;
import java.awt.LayoutManager;

import javax.swing.JPanel;

@SuppressWarnings("serial")
public class SizeRestrictedPanel extends JPanel {
	public SizeRestrictedPanel() {
	}

	public SizeRestrictedPanel(LayoutManager layout) {
		super(layout);
	}

	@Override
	public Dimension getMaximumSize() {
		Dimension size = getPreferredSize();
		size.width = Integer.MAX_VALUE;
		return size;
	}
}
