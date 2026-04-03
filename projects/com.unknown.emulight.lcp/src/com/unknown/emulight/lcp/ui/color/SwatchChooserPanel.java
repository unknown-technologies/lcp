package com.unknown.emulight.lcp.ui.color;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import javax.accessibility.AccessibleContext;
import javax.swing.Icon;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.colorchooser.AbstractColorChooserPanel;

import com.unknown.emulight.lcp.project.Project;

/**
 * The standard color swatch chooser.
 * <p>
 * <strong>Warning:</strong> Serialized objects of this class will not be compatible with future Swing releases. The
 * current serialization support is appropriate for short term storage or RMI between applications running the same
 * version of Swing. As of 1.4, support for long term storage of all JavaBeans has been added to the
 * <code>java.beans</code> package. Please see {@link java.beans.XMLEncoder}.
 *
 * @author Steve Wilson
 */
@SuppressWarnings("serial") // Same-version serialization only
public class SwatchChooserPanel extends AbstractColorChooserPanel {
	private final Project project;

	private SwatchPanel swatchPanel;
	private RecentSwatchPanel recentSwatchPanel;
	private MouseListener mainSwatchListener;
	private MouseListener recentSwatchListener;
	private KeyListener mainSwatchKeyListener;
	private KeyListener recentSwatchKeyListener;

	public SwatchChooserPanel(Project project) {
		this.project = project;
		setInheritsPopupMenu(true);
	}

	@Override
	public String getDisplayName() {
		return UIManager.getString("ColorChooser.swatchesNameText", getLocale());
	}

	/**
	 * Returns an integer from the defaults table. If <code>key</code> does not map to a valid <code>Integer</code>,
	 * <code>default</code> is returned.
	 *
	 * @param key
	 *                an <code>Object</code> specifying the int
	 * @param defaultValue
	 *                Returned value if <code>key</code> is not available, or is not an Integer
	 * @return the int
	 */
	private int getInteger(Object key, int defaultValue) {
		Object value = UIManager.get(key, getLocale());

		if(value instanceof Integer) {
			return ((Integer) value).intValue();
		} else if(value instanceof String) {
			try {
				return Integer.parseInt((String) value);
			} catch(NumberFormatException e) {
				// ignore
			}
		}
		return defaultValue;
	}

	/**
	 * Provides a hint to the look and feel as to the <code>KeyEvent.VK</code> constant that can be used as a
	 * mnemonic to access the panel. A return value <= 0 indicates there is no mnemonic.
	 * <p>
	 * The return value here is a hint, it is ultimately up to the look and feel to honor the return value in some
	 * meaningful way.
	 * <p>
	 * This implementation looks up the value from the default <code>ColorChooser.swatchesMnemonic</code>, or if it
	 * isn't available (or not an <code>Integer</code>) returns -1. The lookup for the default is done through the
	 * <code>UIManager</code>: <code>UIManager.get("ColorChooser.swatchesMnemonic");</code>.
	 *
	 * @return KeyEvent.VK constant identifying the mnemonic; <= 0 for no mnemonic
	 * @see #getDisplayedMnemonicIndex
	 * @since 1.4
	 */
	@Override
	public int getMnemonic() {
		return getInteger("ColorChooser.swatchesMnemonic", -1);
	}

	/**
	 * Provides a hint to the look and feel as to the index of the character in <code>getDisplayName</code> that
	 * should be visually identified as the mnemonic. The look and feel should only use this if
	 * <code>getMnemonic</code> returns a value > 0.
	 * <p>
	 * The return value here is a hint, it is ultimately up to the look and feel to honor the return value in some
	 * meaningful way. For example, a look and feel may wish to render each <code>AbstractColorChooserPanel</code>
	 * in a <code>JTabbedPane</code>, and further use this return value to underline a character in the
	 * <code>getDisplayName</code>.
	 * <p>
	 * This implementation looks up the value from the default <code>ColorChooser.rgbDisplayedMnemonicIndex</code>,
	 * or if it isn't available (or not an <code>Integer</code>) returns -1. The lookup for the default is done
	 * through the <code>UIManager</code>:
	 * <code>UIManager.get("ColorChooser.swatchesDisplayedMnemonicIndex");</code>.
	 *
	 * @return Character index to render mnemonic for; -1 to provide no visual identifier for this panel.
	 * @see #getMnemonic
	 * @since 1.4
	 */
	@Override
	public int getDisplayedMnemonicIndex() {
		return getInteger("ColorChooser.swatchesDisplayedMnemonicIndex", -1);
	}

	@Override
	public Icon getSmallDisplayIcon() {
		return null;
	}

	@Override
	public Icon getLargeDisplayIcon() {
		return null;
	}

	/**
	 * The background color, foreground color, and font are already set to the defaults from the defaults table
	 * before this method is called.
	 */
	@Override
	public void installChooserPanel(JColorChooser enclosingChooser) {
		super.installChooserPanel(enclosingChooser);
	}

	@Override
	protected void buildChooser() {
		String recentStr = UIManager.getString("ColorChooser.swatchesRecentText", getLocale());

		GridBagLayout gb = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		JPanel superHolder = new JPanel(gb);

		swatchPanel = new MainSwatchPanel();
		swatchPanel.putClientProperty(AccessibleContext.ACCESSIBLE_NAME_PROPERTY,
				getDisplayName());
		swatchPanel.setInheritsPopupMenu(true);

		recentSwatchPanel = new RecentSwatchPanel();
		recentSwatchPanel.putClientProperty(AccessibleContext.ACCESSIBLE_NAME_PROPERTY,
				recentStr);

		mainSwatchKeyListener = new MainSwatchKeyListener();
		mainSwatchListener = new MainSwatchListener();
		swatchPanel.addMouseListener(mainSwatchListener);
		swatchPanel.addKeyListener(mainSwatchKeyListener);
		recentSwatchListener = new RecentSwatchListener();
		recentSwatchKeyListener = new RecentSwatchKeyListener();
		recentSwatchPanel.addMouseListener(recentSwatchListener);
		recentSwatchPanel.addKeyListener(recentSwatchKeyListener);

		JPanel mainHolder = new JPanel(new BorderLayout());
		Border border = new CompoundBorder(new LineBorder(Color.black),
				new LineBorder(Color.white));
		mainHolder.setBorder(border);
		mainHolder.add(swatchPanel, BorderLayout.CENTER);

		gbc.anchor = GridBagConstraints.LAST_LINE_START;
		gbc.gridwidth = 1;
		gbc.gridheight = 2;
		Insets oldInsets = gbc.insets;
		gbc.insets = new Insets(0, 0, 0, 10);
		superHolder.add(mainHolder, gbc);
		gbc.insets = oldInsets;

		recentSwatchPanel.setInheritsPopupMenu(true);
		JPanel recentHolder = new JPanel(new BorderLayout());
		recentHolder.setBorder(border);
		recentHolder.setInheritsPopupMenu(true);
		recentHolder.add(recentSwatchPanel, BorderLayout.CENTER);

		JLabel l = new JLabel(recentStr);
		l.setLabelFor(recentSwatchPanel);

		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.gridheight = 1;
		gbc.weighty = 1.0;
		superHolder.add(l, gbc);

		gbc.weighty = 0;
		gbc.gridheight = GridBagConstraints.REMAINDER;
		gbc.insets = new Insets(0, 0, 0, 2);
		superHolder.add(recentHolder, gbc);
		superHolder.setInheritsPopupMenu(true);

		add(superHolder);
	}

	@Override
	public void uninstallChooserPanel(JColorChooser enclosingChooser) {
		super.uninstallChooserPanel(enclosingChooser);
		swatchPanel.removeMouseListener(mainSwatchListener);
		swatchPanel.removeKeyListener(mainSwatchKeyListener);
		recentSwatchPanel.removeMouseListener(recentSwatchListener);
		recentSwatchPanel.removeKeyListener(recentSwatchKeyListener);

		swatchPanel = null;
		recentSwatchPanel = null;
		mainSwatchListener = null;
		mainSwatchKeyListener = null;
		recentSwatchListener = null;
		recentSwatchKeyListener = null;

		removeAll(); // strip out all the sub-components
	}

	@Override
	protected void paintComponent(Graphics g) {
		// totally ridiculous, but this is necessary to only transfer the last changed color to the list of
		// recent colors
		Color color = getColorFromModel();
		if(color != null) {
			swatchPanel.setColor(color);
			recentSwatchPanel.setMostRecentColor(color);
		}
	}

	@Override
	public void updateChooser() {
		Color color = getColorFromModel();
		if(color != null) {
			swatchPanel.setColor(color);
			if(isFocusOwner()) {
				recentSwatchPanel.setMostRecentColor(color);
			}
		}
	}

	private class RecentSwatchKeyListener extends KeyAdapter {
		@Override
		public void keyPressed(KeyEvent e) {
			if(KeyEvent.VK_SPACE == e.getKeyCode()) {
				Color color = recentSwatchPanel.getSelectedColor();
				getColorSelectionModel().setSelectedColor(color);
			}
		}
	}

	private class MainSwatchKeyListener extends KeyAdapter {
		@Override
		public void keyPressed(KeyEvent e) {
			if(KeyEvent.VK_SPACE == e.getKeyCode()) {
				Color color = swatchPanel.getSelectedColor();
				getColorSelectionModel().setSelectedColor(color);
				recentSwatchPanel.setMostRecentColor(color);
			}
		}
	}

	private class RecentSwatchListener extends MouseAdapter implements Serializable {
		@Override
		public void mousePressed(MouseEvent e) {
			if(isEnabled()) {
				Color color = recentSwatchPanel.getColorForLocation(e.getX(), e.getY());
				recentSwatchPanel.setSelectedColorFromLocation(e.getX(), e.getY());
				getColorSelectionModel().setSelectedColor(color);
				recentSwatchPanel.requestFocusInWindow();
			}
		}
	}

	private class MainSwatchListener extends MouseAdapter implements Serializable {
		@Override
		public void mousePressed(MouseEvent e) {
			if(isEnabled()) {
				Color color = swatchPanel.getColorForLocation(e.getX(), e.getY());
				getColorSelectionModel().setSelectedColor(color);
				swatchPanel.setSelectedColorFromLocation(e.getX(), e.getY());
				recentSwatchPanel.setMostRecentColor(color);
				swatchPanel.requestFocusInWindow();
			}
		}
	}

	private class RecentSwatchPanel extends SwatchPanel {
		private int count = 0;

		@Override
		protected void initValues() {
			swatchSize = UIManager.getDimension("ColorChooser.swatchesRecentSwatchSize", getLocale());
			numSwatches = new Dimension(5, 8);
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

			List<Color> lastColors = project.getRecentColors();
			count = 0;
			for(Color color : lastColors) {
				colors[count++] = color;
			}
		}

		public void setMostRecentColor(Color c) {
			if(colors[0].equals(c)) {
				return;
			}

			for(int i = 1; i < colors.length; i++) {
				if(colors[i].equals(c)) {
					// move color to front
					System.arraycopy(colors, 0, colors, 1, i);
					colors[0] = c;
					Color[] recent = Arrays.copyOf(colors, count);
					project.setRecentColors(List.of(recent));
					repaint();
					return;
				}
			}

			if(count >= colors.length) {
				count = colors.length;
			} else {
				count++;
			}

			System.arraycopy(colors, 0, colors, 1, colors.length - 1);
			colors[0] = c;

			Color[] recent = Arrays.copyOf(colors, count);
			project.setRecentColors(List.of(recent));
			repaint();
		}

	}

	private static class MainSwatchPanel extends SwatchPanel {
		@Override
		protected void initValues() {
			swatchSize = UIManager.getDimension("ColorChooser.swatchesSwatchSize", getLocale());
			numSwatches = new Dimension(31, 9);
			gap = new Dimension(1, 1);
		}

		@Override
		protected void initColors() {
			int[] rawValues = initRawValues();
			int numColors = rawValues.length / 3;

			colors = new Color[numColors];
			for(int i = 0; i < numColors; i++) {
				colors[i] = new Color(rawValues[(i * 3)], rawValues[(i * 3) + 1],
						rawValues[(i * 3) + 2]);
			}
		}

		private static int[] initRawValues() {
			int[] rawValues = {
					255, 255, 255, // first row.
					204, 255, 255,
					204, 204, 255,
					204, 204, 255,
					204, 204, 255,
					204, 204, 255,
					204, 204, 255,
					204, 204, 255,
					204, 204, 255,
					204, 204, 255,
					204, 204, 255,
					255, 204, 255,
					255, 204, 204,
					255, 204, 204,
					255, 204, 204,
					255, 204, 204,
					255, 204, 204,
					255, 204, 204,
					255, 204, 204,
					255, 204, 204,
					255, 204, 204,
					255, 255, 204,
					204, 255, 204,
					204, 255, 204,
					204, 255, 204,
					204, 255, 204,
					204, 255, 204,
					204, 255, 204,
					204, 255, 204,
					204, 255, 204,
					204, 255, 204,
					204, 204, 204,  // second row.
					153, 255, 255,
					153, 204, 255,
					153, 153, 255,
					153, 153, 255,
					153, 153, 255,
					153, 153, 255,
					153, 153, 255,
					153, 153, 255,
					153, 153, 255,
					204, 153, 255,
					255, 153, 255,
					255, 153, 204,
					255, 153, 153,
					255, 153, 153,
					255, 153, 153,
					255, 153, 153,
					255, 153, 153,
					255, 153, 153,
					255, 153, 153,
					255, 204, 153,
					255, 255, 153,
					204, 255, 153,
					153, 255, 153,
					153, 255, 153,
					153, 255, 153,
					153, 255, 153,
					153, 255, 153,
					153, 255, 153,
					153, 255, 153,
					153, 255, 204,
					204, 204, 204,  // third row
					102, 255, 255,
					102, 204, 255,
					102, 153, 255,
					102, 102, 255,
					102, 102, 255,
					102, 102, 255,
					102, 102, 255,
					102, 102, 255,
					153, 102, 255,
					204, 102, 255,
					255, 102, 255,
					255, 102, 204,
					255, 102, 153,
					255, 102, 102,
					255, 102, 102,
					255, 102, 102,
					255, 102, 102,
					255, 102, 102,
					255, 153, 102,
					255, 204, 102,
					255, 255, 102,
					204, 255, 102,
					153, 255, 102,
					102, 255, 102,
					102, 255, 102,
					102, 255, 102,
					102, 255, 102,
					102, 255, 102,
					102, 255, 153,
					102, 255, 204,
					153, 153, 153, // fourth row
					51, 255, 255,
					51, 204, 255,
					51, 153, 255,
					51, 102, 255,
					51, 51, 255,
					51, 51, 255,
					51, 51, 255,
					102, 51, 255,
					153, 51, 255,
					204, 51, 255,
					255, 51, 255,
					255, 51, 204,
					255, 51, 153,
					255, 51, 102,
					255, 51, 51,
					255, 51, 51,
					255, 51, 51,
					255, 102, 51,
					255, 153, 51,
					255, 204, 51,
					255, 255, 51,
					204, 255, 51,
					153, 255, 51,
					102, 255, 51,
					51, 255, 51,
					51, 255, 51,
					51, 255, 51,
					51, 255, 102,
					51, 255, 153,
					51, 255, 204,
					153, 153, 153, // Fifth row
					0, 255, 255,
					0, 204, 255,
					0, 153, 255,
					0, 102, 255,
					0, 51, 255,
					0, 0, 255,
					51, 0, 255,
					102, 0, 255,
					153, 0, 255,
					204, 0, 255,
					255, 0, 255,
					255, 0, 204,
					255, 0, 153,
					255, 0, 102,
					255, 0, 51,
					255, 0, 0,
					255, 51, 0,
					255, 102, 0,
					255, 153, 0,
					255, 204, 0,
					255, 255, 0,
					204, 255, 0,
					153, 255, 0,
					102, 255, 0,
					51, 255, 0,
					0, 255, 0,
					0, 255, 51,
					0, 255, 102,
					0, 255, 153,
					0, 255, 204,
					102, 102, 102, // sixth row
					0, 204, 204,
					0, 204, 204,
					0, 153, 204,
					0, 102, 204,
					0, 51, 204,
					0, 0, 204,
					51, 0, 204,
					102, 0, 204,
					153, 0, 204,
					204, 0, 204,
					204, 0, 204,
					204, 0, 204,
					204, 0, 153,
					204, 0, 102,
					204, 0, 51,
					204, 0, 0,
					204, 51, 0,
					204, 102, 0,
					204, 153, 0,
					204, 204, 0,
					204, 204, 0,
					204, 204, 0,
					153, 204, 0,
					102, 204, 0,
					51, 204, 0,
					0, 204, 0,
					0, 204, 51,
					0, 204, 102,
					0, 204, 153,
					0, 204, 204,
					102, 102, 102, // seventh row
					0, 153, 153,
					0, 153, 153,
					0, 153, 153,
					0, 102, 153,
					0, 51, 153,
					0, 0, 153,
					51, 0, 153,
					102, 0, 153,
					153, 0, 153,
					153, 0, 153,
					153, 0, 153,
					153, 0, 153,
					153, 0, 153,
					153, 0, 102,
					153, 0, 51,
					153, 0, 0,
					153, 51, 0,
					153, 102, 0,
					153, 153, 0,
					153, 153, 0,
					153, 153, 0,
					153, 153, 0,
					153, 153, 0,
					102, 153, 0,
					51, 153, 0,
					0, 153, 0,
					0, 153, 51,
					0, 153, 102,
					0, 153, 153,
					0, 153, 153,
					51, 51, 51, // eighth row
					0, 102, 102,
					0, 102, 102,
					0, 102, 102,
					0, 102, 102,
					0, 51, 102,
					0, 0, 102,
					51, 0, 102,
					102, 0, 102,
					102, 0, 102,
					102, 0, 102,
					102, 0, 102,
					102, 0, 102,
					102, 0, 102,
					102, 0, 102,
					102, 0, 51,
					102, 0, 0,
					102, 51, 0,
					102, 102, 0,
					102, 102, 0,
					102, 102, 0,
					102, 102, 0,
					102, 102, 0,
					102, 102, 0,
					102, 102, 0,
					51, 102, 0,
					0, 102, 0,
					0, 102, 51,
					0, 102, 102,
					0, 102, 102,
					0, 102, 102,
					0, 0, 0, // ninth row
					0, 51, 51,
					0, 51, 51,
					0, 51, 51,
					0, 51, 51,
					0, 51, 51,
					0, 0, 51,
					51, 0, 51,
					51, 0, 51,
					51, 0, 51,
					51, 0, 51,
					51, 0, 51,
					51, 0, 51,
					51, 0, 51,
					51, 0, 51,
					51, 0, 51,
					51, 0, 0,
					51, 51, 0,
					51, 51, 0,
					51, 51, 0,
					51, 51, 0,
					51, 51, 0,
					51, 51, 0,
					51, 51, 0,
					51, 51, 0,
					0, 51, 0,
					0, 51, 51,
					0, 51, 51,
					0, 51, 51,
					0, 51, 51,
					51, 51, 51 };
			return rawValues;
		}
	}
}