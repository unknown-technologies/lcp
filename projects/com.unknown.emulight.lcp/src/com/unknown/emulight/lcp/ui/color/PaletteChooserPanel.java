package com.unknown.emulight.lcp.ui.color;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.colorchooser.AbstractColorChooserPanel;

import com.unknown.emulight.lcp.project.Palette;

@SuppressWarnings("serial")
public class PaletteChooserPanel extends AbstractColorChooserPanel {
	private final Palette palette;

	private static final int COLORS_PER_ROW = 9;

	public int getColorCount() {
		return palette.getColorCount();
	}

	public Color getColor(int i) {
		if(i < 0) {
			throw new IllegalArgumentException("invalid color " + i);
		}
		return palette.getColor(i);
	}

	public PaletteChooserPanel(Palette palette) {
		this.palette = palette;
	}

	private void setColor(int i) {
		getColorSelectionModel().setSelectedColor(getColor(i));
	}

	@Override
	public void updateChooser() {
		// nothing
	}

	@Override
	protected void buildChooser() {
		setLayout(new BorderLayout());
		int rows = (int) Math.ceil(getColorCount() / (double) COLORS_PER_ROW);
		JPanel colors = new JPanel(new GridLayout(rows, COLORS_PER_ROW));

		Dimension size = new Dimension(50, 50);

		Color selected = getColorFromModel();

		JButton focus = null;

		for(int i = 0; i < getColorCount(); i++) {
			final int c = i;
			Color color = getColor(i);

			JButton button = new JButton();
			button.setBackground(color);
			button.setSelected(false);
			button.addActionListener(e -> {
				setColor(c);
			});

			String tooltip = color.getRed() + ", " + color.getGreen() + ", " + color.getBlue();

			button.setMinimumSize(size);
			button.setPreferredSize(size);
			button.setSize(size);
			button.setToolTipText(tooltip);
			colors.add(button);

			if(color.equals(selected)) {
				focus = button;
			}
		}

		for(int i = getColorCount() % COLORS_PER_ROW; i < COLORS_PER_ROW; i++) {
			colors.add(new JPanel());
		}

		add(BorderLayout.CENTER, colors);

		if(focus != null) {
			JButton btn = focus;
			SwingUtilities.invokeLater(() -> btn.requestFocusInWindow());
		}
	}

	@Override
	public void uninstallChooserPanel(JColorChooser enclosingChooser) {
		super.uninstallChooserPanel(enclosingChooser);
		removeAll();
	}

	@Override
	public String getDisplayName() {
		return "Palette";
	}

	@Override
	public int getMnemonic() {
		return KeyEvent.VK_P;
	}

	@Override
	public int getDisplayedMnemonicIndex() {
		return 0;
	}

	@Override
	public Icon getSmallDisplayIcon() {
		return null;
	}

	@Override
	public Icon getLargeDisplayIcon() {
		return null;
	}
}
