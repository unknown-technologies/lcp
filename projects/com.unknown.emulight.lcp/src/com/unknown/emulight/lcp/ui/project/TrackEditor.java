package com.unknown.emulight.lcp.ui.project;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;
import javax.swing.border.BevelBorder;

import com.unknown.emulight.lcp.audio.AudioTrack;
import com.unknown.emulight.lcp.dmx.DMXTrack;
import com.unknown.emulight.lcp.laser.LaserTrack;
import com.unknown.emulight.lcp.project.EmulightSystem;
import com.unknown.emulight.lcp.project.Track;
import com.unknown.emulight.lcp.sequencer.MidiTrack;
import com.unknown.emulight.lcp.ui.UIUtils;
import com.unknown.emulight.lcp.ui.audio.AudioTrackEditor;
import com.unknown.emulight.lcp.ui.dmx.DMXTrackEditor;
import com.unknown.emulight.lcp.ui.laser.LaserTrackEditor;
import com.unknown.emulight.lcp.ui.midi.MidiTrackEditor;

@SuppressWarnings("serial")
public abstract class TrackEditor extends JDialog {
	protected final EmulightSystem sys;
	protected final Track<?> track;
	protected final JComponent trackColorBox;

	protected TrackEditor(EmulightSystem sys, Track<?> track) {
		super(sys.getMainWindow(), "Track: " + track.getName());
		this.sys = sys;
		this.track = track;

		trackColorBox = createColorBox();

		JComponent root = getRootPane();
		KeyStroke escKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		Object quit = new Object();
		root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escKey, quit);
		root.getActionMap().put(quit, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
	}

	protected JComponent getTrackColorBox() {
		return trackColorBox;
	}

	private JComponent createColorBox() {
		JComponent colorBox = new JComponent() {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				g.setColor(track.getProject().getColor(track.getColor()));
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
				Color color = UIUtils.showPaletteColorChooser(TrackEditor.this,
						"Track color...", track.getProject().getColor(track.getColor()),
						track.getProject().getPalette());
				if(color != null) {
					int idx = track.getProject().getPalette().getColorIndex(color);
					if(idx != -1) {
						track.setColor(idx);
					}
					colorBox.repaint();
				}
			}
		});
		return colorBox;
	}

	public static TrackEditor show(EmulightSystem sys, Track<?> track) {
		switch(track.getType()) {
		case Track.AUDIO:
			return new AudioTrackEditor(sys, (AudioTrack) track);
		case Track.MIDI:
			return new MidiTrackEditor(sys, (MidiTrack) track);
		case Track.LASER:
			return new LaserTrackEditor(sys, (LaserTrack) track);
		case Track.DMX:
			return new DMXTrackEditor(sys, (DMXTrack) track);
		default:
			return null;
		}
	}

	public void destroy() {
		// empty
	}
}
