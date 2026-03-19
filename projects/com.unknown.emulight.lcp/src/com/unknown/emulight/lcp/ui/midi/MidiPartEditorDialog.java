package com.unknown.emulight.lcp.ui.midi;

import java.awt.BorderLayout;
import java.awt.FileDialog;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import com.unknown.audio.midi.smf.EndOfTrackEvent;
import com.unknown.audio.midi.smf.MIDIEvent;
import com.unknown.audio.midi.smf.MTrk;
import com.unknown.audio.midi.smf.SMF;
import com.unknown.audio.midi.smf.SequenceNameEvent;
import com.unknown.audio.midi.smf.TempoEvent;
import com.unknown.emulight.lcp.project.PartContainer;
import com.unknown.emulight.lcp.sequencer.MidiPart;
import com.unknown.emulight.lcp.sequencer.MidiTrack;
import com.unknown.emulight.lcp.sequencer.Note;
import com.unknown.emulight.lcp.ui.event.PreviewListener;
import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;

@SuppressWarnings("serial")
public class MidiPartEditorDialog extends JFrame {
	private static final Logger log = Trace.create(MidiPartEditorDialog.class);

	private static final int MENU_MODIFIER = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

	private PartContainer<MidiPart> container;

	private MidiPartEditor editor;

	private JLabel status;

	public MidiPartEditorDialog(PartContainer<MidiPart> container) {
		super(getTitle(container));

		this.container = container;

		FileDialog loadSequenceDialog = new FileDialog(this, "Load sequence...", FileDialog.LOAD);
		FileDialog saveSequenceDialog = new FileDialog(this, "Save sequence...", FileDialog.SAVE);

		setLayout(new BorderLayout());

		MidiTrack track = (MidiTrack) container.getTrack();

		editor = new MidiPartEditor(container.getPart());
		editor.setStartTime(container.getTime());
		editor.addPreviewListener(new PreviewListener() {
			@Override
			public void pressed(Note note) {
				track.noteOn(note);
			}

			@Override
			public void released(Note note) {
				track.noteOff(note);
			}
		});

		status = new JLabel("READY");

		add(BorderLayout.CENTER, editor);
		add(BorderLayout.SOUTH, status);

		JMenuBar menu = new JMenuBar();

		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic('F');

		JMenuItem loadSequence = new JMenuItem("Load sequence...");
		loadSequence.setMnemonic('L');
		loadSequence.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, MENU_MODIFIER));
		loadSequence.addActionListener(e -> {
			loadSequenceDialog.setVisible(true);
			if(loadSequenceDialog.getFile() == null) {
				return;
			}
			String filename = loadSequenceDialog.getDirectory() + loadSequenceDialog.getFile();
			try {
				loadSequence(new File(filename));
				setStatus("Sequence loaded from " + filename);
			} catch(IOException ex) {
				setStatus("I/O Error: " + ex.getMessage());
				log.log(Levels.ERROR, "Failed to load sequence from file", ex);
			}
		});

		JMenuItem saveSequence = new JMenuItem("Save sequence...");
		saveSequence.setMnemonic('S');
		saveSequence.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, MENU_MODIFIER));
		saveSequence.addActionListener(e -> {
			saveSequenceDialog.setVisible(true);
			if(saveSequenceDialog.getFile() == null) {
				return;
			}
			String filename = saveSequenceDialog.getDirectory() + saveSequenceDialog.getFile();
			try {
				saveSequence(new File(filename));
				setStatus("Sequence saved to " + filename);
			} catch(IOException ex) {
				setStatus("I/O Error: " + ex.getMessage());
				log.log(Levels.ERROR, "Failed to save sequence to file", ex);
			}
		});

		JMenuItem preferences = new JMenuItem("Part Settings");
		preferences.setMnemonic('P');
		preferences.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, MENU_MODIFIER));
		preferences.addActionListener(e -> {
			// RSeqConfigDialog cfg = new RSeqConfigDialog(this);
			// sys.getDesktop().addFrame(cfg);
			// cfg.show();
		});

		JMenuItem exit = new JMenuItem("Quit");
		exit.setMnemonic('Q');
		exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, MENU_MODIFIER));
		exit.addActionListener(e -> dispose());

		fileMenu.add(loadSequence);
		fileMenu.add(saveSequence);
		fileMenu.addSeparator();
		fileMenu.add(preferences);
		fileMenu.addSeparator();
		fileMenu.add(exit);

		menu.add(fileMenu);
		setJMenuBar(menu);

		setSize(640, 480);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}

	private static String getTitle(PartContainer<MidiPart> container) {
		if(container.getPart().getName() == null) {
			return "Part";
		} else {
			return "Part: " + container.getPart().getName();
		}
	}

	private void setStatus(String text) {
		status.setText(text);
	}

	private void loadSequence(File file) throws IOException {
		try(InputStream in = new BufferedInputStream(new FileInputStream(file))) {
			SMF smf = new SMF(in);
			loadSMF(smf);
		}
	}

	private void saveSequence(File file) throws IOException {
		try(OutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
			SMF smf = getSMF();
			smf.write(out);
		}
	}

	private void loadSMF(SMF smf) {
		MidiPart part = container.getPart();
		part.clear();
		part.loadAllTracks(smf);
		editor.reload();
	}

	private SMF getSMF() {
		MidiPart part = container.getPart();
		SMF smf = new SMF();
		smf.getHeader().setPPQ(part.getPPQ());

		MTrk track = new MTrk();

		if(part.getName() != null) {
			track.addEvent(new SequenceNameEvent(0, part.getName()));
		}

		double bpm = container.getTrack().getProject().getTempoTrack().getTempo(0);
		if(bpm != 120) { // exact comparison is intentional
			track.addEvent(new TempoEvent(0, TempoEvent.getMicroTempo(bpm)));
		}

		for(MIDIEvent evt : part.toMidi(0, MidiTrack.ANY)) {
			track.addEvent(evt);
		}

		if(!track.getEvents().isEmpty()) {
			long time = track.getEvents().get(track.getEvents().size() - 1).getTime();
			track.addEvent(new EndOfTrackEvent(time));
		}

		smf.addTrack(track);
		return smf;
	}
}
