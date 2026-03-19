package com.unknown.emulight.lcp.ui.laser;

import java.awt.BorderLayout;
import java.awt.FileDialog;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.Timer;

import com.unknown.emulight.lcp.event.SequencerListener;
import com.unknown.emulight.lcp.laser.LaserPart;
import com.unknown.emulight.lcp.project.PartContainer;
import com.unknown.emulight.lcp.project.Project;
import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;
import com.unknown.xml.dom.Element;
import com.unknown.xml.dom.XMLReader;

@SuppressWarnings("serial")
public class LaserPartEditorDialog extends JFrame {
	private static final Logger log = Trace.create(LaserPartEditorDialog.class);

	private static final int MENU_MODIFIER = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

	private PartContainer<LaserPart> container;

	private ClipEditor editor;
	private JLabel status;

	public LaserPartEditorDialog(PartContainer<LaserPart> container) {
		super(getTitle(container));

		this.container = container;

		FileDialog loadClipDialog = new FileDialog(this, "Load clip...", FileDialog.LOAD);
		FileDialog saveClipDialog = new FileDialog(this, "Save clip...", FileDialog.SAVE);

		setLayout(new BorderLayout());

		editor = new ClipEditor(this, container, t -> setStatus("Time: " + t));

		status = new JLabel("READY");

		add(BorderLayout.CENTER, editor);
		add(BorderLayout.SOUTH, status);

		JMenuBar menu = new JMenuBar();

		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic('F');

		JMenuItem loadClip = new JMenuItem("Load clip...");
		loadClip.setMnemonic('L');
		loadClip.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, MENU_MODIFIER));
		loadClip.addActionListener(e -> {
			loadClipDialog.setVisible(true);
			if(loadClipDialog.getFile() == null) {
				return;
			}
			String filename = loadClipDialog.getDirectory() + loadClipDialog.getFile();
			try {
				loadClip(new File(filename));
				setStatus("Clip loaded from " + filename);
			} catch(IOException ex) {
				setStatus("I/O Error: " + ex.getMessage());
				log.log(Levels.ERROR, "Failed to load clip from file", ex);
			}
		});

		JMenuItem saveClip = new JMenuItem("Save clip...");
		saveClip.setMnemonic('S');
		saveClip.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, MENU_MODIFIER));
		saveClip.addActionListener(e -> {
			saveClipDialog.setVisible(true);
			if(saveClipDialog.getFile() == null) {
				return;
			}
			String filename = saveClipDialog.getDirectory() + saveClipDialog.getFile();
			try {
				saveClip(new File(filename));
				setStatus("Clip saved to " + filename);
			} catch(IOException ex) {
				setStatus("I/O Error: " + ex.getMessage());
				log.log(Levels.ERROR, "Failed to save clip to file", ex);
			}
		});

		JMenuItem exit = new JMenuItem("Quit");
		exit.setMnemonic('Q');
		exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, MENU_MODIFIER));
		exit.addActionListener(e -> dispose());

		fileMenu.add(loadClip);
		fileMenu.add(saveClip);
		fileMenu.addSeparator();
		// fileMenu.add(preferences);
		// fileMenu.addSeparator();
		fileMenu.add(exit);

		menu.add(fileMenu);
		setJMenuBar(menu);

		setSize(1280, 720);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		Project project = container.getTrack().getProject();
		Timer timer = new Timer(50, e -> editor.setPosition(project.getSequencer().getTick()));
		timer.setRepeats(true);

		SequencerListener listener = new SequencerListener() {
			public void playbackStarted() {
				timer.start();
			}

			public void playbackStopped() {
				timer.stop();
				editor.setPosition(project.getSequencer().getTick());
			}

			public void positionChanged(long tick) {
				editor.setPosition(project.getSequencer().getTick());
			}
		};

		project.getSequencer().addListener(listener);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				project.getSequencer().removeListener(listener);
			}
		});
	}

	private static String getTitle(PartContainer<LaserPart> container) {
		if(container.getPart().getName() == null) {
			return "Part";
		} else {
			return "Part: " + container.getPart().getName();
		}
	}

	private void setStatus(String text) {
		status.setText(text);
	}

	private void loadClip(File file) throws IOException {
		try(InputStream in = new BufferedInputStream(new FileInputStream(file))) {
			Element xml = XMLReader.read(in);
			LaserPart part = container.getPart();
			part.read(xml);
			String partName = xml.getAttribute("name");
			if(partName != null) {
				part.setName(partName);
			}
			editor.setClip(container);
		}
	}

	private void saveClip(File file) throws IOException {
		Element xml = new Element("clip");
		LaserPart part = container.getPart();
		if(part.getName() != null) {
			xml.addAttribute("name", part.getName());
		}
		container.getPart().write(xml);
		Files.write(file.toPath(), xml.toString().getBytes(StandardCharsets.UTF_8));
	}
}
