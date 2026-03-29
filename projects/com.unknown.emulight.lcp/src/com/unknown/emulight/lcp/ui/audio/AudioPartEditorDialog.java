package com.unknown.emulight.lcp.ui.audio;

import java.awt.BorderLayout;
import java.awt.FileDialog;
import java.awt.Toolkit;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.KeyStroke;

import com.unknown.emulight.lcp.audio.AudioData;
import com.unknown.emulight.lcp.audio.AudioPart;
import com.unknown.emulight.lcp.project.PartContainer;
import com.unknown.emulight.lcp.ui.laser.Callback;
import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;
import com.unknown.util.ui.WaveformEditor;

@SuppressWarnings("serial")
public class AudioPartEditorDialog extends JFrame {
	private static final Logger log = Trace.create(AudioPartEditorDialog.class);

	private static final int MENU_MODIFIER = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

	private PartContainer<AudioPart> container;

	private WaveformEditor editor;

	private JLabel status;

	public AudioPartEditorDialog(PartContainer<AudioPart> container, Callback updater) {
		super(getTitle(container));

		this.container = container;

		FileDialog loadSequenceDialog = new FileDialog(this, "Load audio...", FileDialog.LOAD);
		FileDialog saveSequenceDialog = new FileDialog(this, "Save audio...", FileDialog.SAVE);

		setLayout(new BorderLayout());

		AudioData data = container.getPart().getData();

		editor = new WaveformEditor();
		editor.setTimeDivision(0.1);
		editor.setVoltageDivision(0.1);
		editor.setDefaultDivision(0.1, 0.1);

		if(data != null) {
			editor.setSampleRate(data.getSampleRate());
			editor.setSignal(data.getMono());
		} else {
			int sampleRate = container.getTrack().getProject().getSystem().getAudioProcessor()
					.getSampleRate();
			editor.setSampleRate(sampleRate);
			editor.setSignal(new float[0]);
		}

		JScrollBar intensityScroller = new JScrollBar(JScrollBar.VERTICAL, 0, 10, 0, 400);
		AdjustmentListener l = e -> {
			int raw = intensityScroller.getValue();
			float value = 1.0f - raw / 390.0f;
			float intensity = 1.0f + value * 40.0f;
			editor.setBeamIntensity(intensity);
		};
		intensityScroller.addAdjustmentListener(l);
		l.adjustmentValueChanged(null);

		JPanel waveformView = new JPanel(new BorderLayout());
		waveformView.add(BorderLayout.CENTER, editor);
		waveformView.add(BorderLayout.EAST, intensityScroller);

		status = new JLabel("READY");

		add(BorderLayout.CENTER, waveformView);
		add(BorderLayout.SOUTH, status);

		JMenuBar menu = new JMenuBar();

		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic('F');

		JMenuItem loadWaveData = new JMenuItem("Load wave data...");
		loadWaveData.setMnemonic('L');
		loadWaveData.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, MENU_MODIFIER));
		loadWaveData.addActionListener(e -> {
			loadSequenceDialog.setVisible(true);
			if(loadSequenceDialog.getFile() == null) {
				return;
			}
			String filename = loadSequenceDialog.getDirectory() + loadSequenceDialog.getFile();
			try {
				loadWaveData(new File(filename));
				setStatus("Wave data loaded from " + filename);
				updater.callback();
			} catch(IOException ex) {
				setStatus("I/O Error: " + ex.getMessage());
				log.log(Levels.ERROR, "Failed to load wave data from file", ex);
			}
		});

		JMenuItem saveWaveData = new JMenuItem("Save wave data...");
		saveWaveData.setMnemonic('S');
		saveWaveData.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, MENU_MODIFIER));
		saveWaveData.addActionListener(e -> {
			saveSequenceDialog.setVisible(true);
			if(saveSequenceDialog.getFile() == null) {
				return;
			}
			String filename = saveSequenceDialog.getDirectory() + saveSequenceDialog.getFile();
			try {
				saveWaveData(new File(filename));
				setStatus("Wave data saved to " + filename);
			} catch(IOException ex) {
				setStatus("I/O Error: " + ex.getMessage());
				log.log(Levels.ERROR, "Failed to save wave data to file", ex);
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
		exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
		exit.addActionListener(e -> dispose());

		fileMenu.add(loadWaveData);
		fileMenu.add(saveWaveData);
		fileMenu.addSeparator();
		fileMenu.add(preferences);
		fileMenu.addSeparator();
		fileMenu.add(exit);

		menu.add(fileMenu);
		setJMenuBar(menu);

		setSize(640, 480);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}

	private static String getTitle(PartContainer<AudioPart> container) {
		if(container.getPart().getName() == null) {
			return "Part";
		} else {
			return "Part: " + container.getPart().getName();
		}
	}

	private void setStatus(String text) {
		status.setText(text);
	}

	private void loadWaveData(File file) throws IOException {
		AudioData data = new AudioData(file);
		container.getPart().setData(data);

		editor.setSampleRate(data.getSampleRate());
		editor.setSignal(data.getMono());
	}

	private void saveWaveData(File file) throws IOException {
		AudioData data = container.getPart().getData();
		if(data == null) {
			return;
		}

		data.writeWAV(file);
	}
}
