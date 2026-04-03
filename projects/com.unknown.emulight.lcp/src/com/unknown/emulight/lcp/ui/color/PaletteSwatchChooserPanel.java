package com.unknown.emulight.lcp.ui.color;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serializable;

import javax.accessibility.AccessibleContext;
import javax.swing.Icon;
import javax.swing.JColorChooser;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.colorchooser.AbstractColorChooserPanel;

@SuppressWarnings("serial")
public class PaletteSwatchChooserPanel extends AbstractColorChooserPanel {
	private final Color[] colors;

	private PaletteSwatchPanel swatchPanel;
	private PaletteSwatchKeyListener swatchKeyListener;
	private PaletteSwatchListener swatchListener;

	public PaletteSwatchChooserPanel(Color[] colors) {
		assert colors != null;
		this.colors = colors;
		setInheritsPopupMenu(true);
	}

	@Override
	public void updateChooser() {
		Color color = getColorFromModel();
		if(color != null) {
			swatchPanel.setColor(color);
		}
	}

	@Override
	protected void buildChooser() {
		swatchPanel = new PaletteSwatchPanel();
		swatchPanel.putClientProperty(AccessibleContext.ACCESSIBLE_NAME_PROPERTY, getDisplayName());
		swatchPanel.setInheritsPopupMenu(true);

		swatchKeyListener = new PaletteSwatchKeyListener();
		swatchListener = new PaletteSwatchListener();
		swatchPanel.addMouseListener(swatchListener);
		swatchPanel.addKeyListener(swatchKeyListener);

		JPanel mainHolder = new JPanel(new BorderLayout());
		Border border = new CompoundBorder(new LineBorder(Color.black),
				new LineBorder(Color.white));
		mainHolder.setBorder(border);
		mainHolder.add(swatchPanel, BorderLayout.CENTER);

		GridBagLayout gb = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		JPanel superHolder = new JPanel(gb);

		gbc.anchor = GridBagConstraints.LAST_LINE_START;
		gbc.gridwidth = 1;
		gbc.gridheight = 2;
		Insets oldInsets = gbc.insets;
		gbc.insets = new Insets(0, 0, 0, 10);
		superHolder.add(mainHolder, gbc);
		gbc.insets = oldInsets;

		add(superHolder);
	}

	@Override
	public void uninstallChooserPanel(JColorChooser enclosingChooser) {
		super.uninstallChooserPanel(enclosingChooser);
		swatchPanel.removeMouseListener(swatchListener);
		swatchPanel.removeKeyListener(swatchKeyListener);

		swatchPanel = null;
		swatchListener = null;
		swatchKeyListener = null;

		removeAll();
	}

	@Override
	public String getDisplayName() {
		return "Project Colors";
	}

	@Override
	public Icon getSmallDisplayIcon() {
		return null;
	}

	@Override
	public Icon getLargeDisplayIcon() {
		return null;
	}

	private class PaletteSwatchPanel extends SwatchPanel {
		@Override
		protected void initValues() {
			Color[] palette = PaletteSwatchChooserPanel.this.colors;
			double size = Math.sqrt(palette.length);
			int columns = (int) Math.ceil(size);
			int rows = palette.length / columns;
			if((palette.length % columns) != 0) {
				rows++;
			}

			swatchSize = UIManager.getDimension("ColorChooser.swatchesSwatchSize", getLocale());
			numSwatches = new Dimension(columns, rows);
			gap = new Dimension(1, 1);
		}

		@Override
		protected void initColors() {
			Color defaultRecentColor = UIManager.getColor("ColorChooser.swatchesDefaultRecentColor",
					getLocale());
			int numColors = numSwatches.width * numSwatches.height;

			colors = new Color[numColors];
			for(int i = 0; i < numColors; i++) {
				colors[i] = defaultRecentColor;
			}

			Color[] palette = PaletteSwatchChooserPanel.this.colors;
			System.arraycopy(palette, 0, colors, 0, palette.length);
		}
	}

	private class PaletteSwatchListener extends MouseAdapter implements Serializable {
		@Override
		public void mousePressed(MouseEvent e) {
			if(isEnabled()) {
				Color color = swatchPanel.getColorForLocation(e.getX(), e.getY());
				swatchPanel.setSelectedColorFromLocation(e.getX(), e.getY());
				getColorSelectionModel().setSelectedColor(color);
				swatchPanel.requestFocusInWindow();
			}
		}
	}

	private class PaletteSwatchKeyListener extends KeyAdapter {
		@Override
		public void keyPressed(KeyEvent e) {
			if(KeyEvent.VK_SPACE == e.getKeyCode()) {
				Color color = swatchPanel.getSelectedColor();
				getColorSelectionModel().setSelectedColor(color);
			}
		}
	}
}
