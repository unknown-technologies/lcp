package com.unknown.emulight.lcp.ui.project;

import java.awt.Graphics;
import java.awt.image.ImageObserver;

public abstract class TrackControl {
	protected final ProjectView parent;

	protected TrackControl(ProjectView parent) {
		this.parent = parent;
	}

	public abstract int getWidth();

	public abstract int getHeight();

	public abstract boolean click(int x, int y, int px, int py);

	public abstract void paint(Graphics g, int x, int y, ImageObserver observer);

	public abstract boolean isIntegrated(); // "integrated" buttons like record/monitor?
}
