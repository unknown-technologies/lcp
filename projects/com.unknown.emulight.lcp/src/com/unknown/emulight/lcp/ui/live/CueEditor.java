package com.unknown.emulight.lcp.ui.live;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;
import javax.swing.border.BevelBorder;

import com.unknown.emulight.lcp.laser.LaserCue;
import com.unknown.emulight.lcp.live.Cue;
import com.unknown.emulight.lcp.project.EmulightSystem;
import com.unknown.emulight.lcp.project.Track;
import com.unknown.emulight.lcp.ui.UIUtils;
import com.unknown.emulight.lcp.ui.laser.LaserCueEditor;

@SuppressWarnings("serial")
public abstract class CueEditor extends JDialog {
	protected final EmulightSystem sys;
	protected final Cue<?> cue;

	protected CueEditor(EmulightSystem sys, Cue<?> cue) {
		super(sys.getMainWindow(), "Cue: " + cue.getName(), true);
		this.sys = sys;
		this.cue = cue;

		JComponent root = getRootPane();
		KeyStroke escKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		Object quit = new Object();
		root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escKey, quit);
		root.getActionMap().put(quit, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
				destroy();
			}
		});

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				dispose();
				destroy();
			}
		});
	}

	protected JComponent createColorBox() {
		JComponent colorBox = new JComponent() {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				g.setColor(cue.getProject().getColor(cue.getColor()));
				g.fillRect(0, 0, getWidth(), getHeight());
			}
		};
		colorBox.setToolTipText("Click to open color chooser");
		colorBox.setMinimumSize(new Dimension(22, 22));
		colorBox.setPreferredSize(new Dimension(22, 22));
		colorBox.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		colorBox.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				Color color = UIUtils.showPaletteColorChooser(CueEditor.this,
						"Cue color...", cue.getProject().getColor(cue.getColor()),
						cue.getProject().getPalette());
				if(color != null) {
					int idx = cue.getProject().getPalette().getColorIndex(color);
					if(idx != -1) {
						cue.setColor(idx);
					}
					colorBox.repaint();
				}
			}
		});
		return colorBox;
	}

	public static CueEditor show(EmulightSystem sys, Cue<?> cue) {
		switch(cue.getType()) {
		case Track.LASER:
			return new LaserCueEditor(sys, (LaserCue) cue);
		default:
			return null;
		}
	}

	public void destroy() {
		// empty
	}
}
