package com.unknown.emulight.lcp.ui.color;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JComponent;

@SuppressWarnings("serial")
public class ColorBox extends JComponent {
	private Color color = Color.BLACK;

	public void setColor(Color color) {
		this.color = color;
		repaint();
	}

	public Color getColor() {
		return color;
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		g.setColor(color);
		g.fillRect(0, 0, getWidth(), getHeight());
	}
}
