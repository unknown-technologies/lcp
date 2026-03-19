package com.unknown.emulight.lcp.ui.project;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;

import com.unknown.emulight.lcp.laser.LaserTrack;
import com.unknown.emulight.lcp.project.EmulightSystem;
import com.unknown.emulight.lcp.project.Track;
import com.unknown.emulight.lcp.sequencer.MidiTrack;
import com.unknown.emulight.lcp.ui.laser.LaserTrackEditor;
import com.unknown.emulight.lcp.ui.midi.MidiTrackEditor;

@SuppressWarnings("serial")
public abstract class TrackEditor extends JDialog {
	private static final int MENU_MODIFIER = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

	protected TrackEditor(EmulightSystem sys, Track<?> track) {
		super(sys.getMainWindow(), "Track: " + track.getName());

		JComponent root = getRootPane();
		KeyStroke quitKey = KeyStroke.getKeyStroke(KeyEvent.VK_Q, MENU_MODIFIER);
		KeyStroke escKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(quitKey, quitKey);
		root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escKey, quitKey);
		root.getActionMap().put(quitKey, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
	}

	public static TrackEditor show(EmulightSystem sys, Track<?> track) {
		switch(track.getType()) {
		case Track.MIDI:
			return new MidiTrackEditor(sys, (MidiTrack) track);
		case Track.LASER:
			return new LaserTrackEditor(sys, (LaserTrack) track);
		default:
			return null;
		}
	}

	public void destroy() {
		// empty
	}
}
