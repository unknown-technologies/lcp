package com.unknown.emulight.lcp.ui.project;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;

import com.unknown.emulight.lcp.audio.AudioTrack;
import com.unknown.emulight.lcp.laser.LaserTrack;
import com.unknown.emulight.lcp.project.EmulightSystem;
import com.unknown.emulight.lcp.project.Track;
import com.unknown.emulight.lcp.sequencer.MidiTrack;
import com.unknown.emulight.lcp.ui.audio.AudioTrackEditor;
import com.unknown.emulight.lcp.ui.laser.LaserTrackEditor;
import com.unknown.emulight.lcp.ui.midi.MidiTrackEditor;

@SuppressWarnings("serial")
public abstract class TrackEditor extends JDialog {
	protected final EmulightSystem sys;

	protected TrackEditor(EmulightSystem sys, Track<?> track) {
		super(sys.getMainWindow(), "Track: " + track.getName());
		this.sys = sys;

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

	public static TrackEditor show(EmulightSystem sys, Track<?> track) {
		switch(track.getType()) {
		case Track.AUDIO:
			return new AudioTrackEditor(sys, (AudioTrack) track);
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
