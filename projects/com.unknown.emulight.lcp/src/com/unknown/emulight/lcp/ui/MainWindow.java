package com.unknown.emulight.lcp.ui;

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
import java.util.List;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.unknown.audio.midi.smf.Event;
import com.unknown.audio.midi.smf.MTrk;
import com.unknown.audio.midi.smf.SMF;
import com.unknown.audio.midi.smf.TempoEvent;
import com.unknown.emulight.lcp.event.ProjectListener;
import com.unknown.emulight.lcp.io.esl.ESLListener;
import com.unknown.emulight.lcp.io.esl.protocol.ESLDescriptor;
import com.unknown.emulight.lcp.io.midi.MidiReceiver;
import com.unknown.emulight.lcp.laser.LaserProcessor;
import com.unknown.emulight.lcp.project.EmulightSystem;
import com.unknown.emulight.lcp.project.PartContainer;
import com.unknown.emulight.lcp.project.Project;
import com.unknown.emulight.lcp.project.SystemConfiguration;
import com.unknown.emulight.lcp.project.TempoTrack;
import com.unknown.emulight.lcp.project.Track;
import com.unknown.emulight.lcp.sequencer.MidiPart;
import com.unknown.emulight.lcp.sequencer.MidiTrack;
import com.unknown.emulight.lcp.sequencer.Sequencer;
import com.unknown.emulight.lcp.ui.help.HelpBrowser;
import com.unknown.emulight.lcp.ui.laser.LaserDiscovery;
import com.unknown.emulight.lcp.ui.project.ProjectEditor;
import com.unknown.emulight.lcp.ui.resources.icons.Icons;
import com.unknown.net.shownet.Laser;
import com.unknown.net.shownet.LaserConnectionListener;
import com.unknown.net.shownet.LaserDiscoveryListener;
import com.unknown.net.shownet.LaserInfo;
import com.unknown.util.io.FourCC;
import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;
import com.unknown.util.ui.MessageBox;
import com.unknown.xml.dom.Element;
import com.unknown.xml.dom.XMLReader;

@SuppressWarnings("serial")
public class MainWindow extends JFrame {
	private static final Logger log = Trace.create(MainWindow.class);

	private static final int MENU_MODIFIER = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

	private static final String TITLE = "Emulight Laser Control Program";

	private final EmulightSystem sys;
	private final LaserProcessor processor;

	private final JLabel discoveryStatus;
	private final JLabel status;

	private Project project;

	private long lastStartPosition = 0;

	public MainWindow(EmulightSystem sys) {
		super(TITLE);

		setLayout(new BorderLayout());

		FileDialog loadDialog = new FileDialog(this, "Open...", FileDialog.LOAD);
		FileDialog saveDialog = new FileDialog(this, "Save...", FileDialog.SAVE);
		FileDialog importMidiDialog = new FileDialog(this, "Import MIDI...", FileDialog.LOAD);

		sys.setMainWindow(this);
		this.sys = sys;
		project = new Project(sys);
		processor = sys.getLaserProcessor();

		// log ESL stuff directly into system log
		sys.getESL().addESLListener(new ESLListener() {
			@Override
			public void deviceListChanged(ESLDescriptor[] descriptors) {
				// devlist.update(descriptors);
			}

			@Override
			public void log(int address, String message) {
				ESLDescriptor desc = sys.getESL().getDescriptor(address);
				LogRecord record = new LogRecord(Levels.INFO, message.trim());
				record.setSourceClassName("ESL");
				String addr = Integer.toString(address);
				if(addr.length() == 1) {
					addr = "0" + addr;
				}
				if(desc != null) {
					record.setSourceMethodName("DEV" + addr + "(" +
							FourCC.ascii(desc.getDeviceId()).trim() + ")");
				} else {
					record.setSourceMethodName("DEV" + addr);
				}
				log.log(record);
				// logview.log(address, message);
			}
		});

		// try to open ESL connection if possible; ignore it if this fails
		try {
			sys.open();
		} catch(IOException e) {
			log.log(Levels.ERROR, "Failed to open serial connection: " + e.getMessage());
		}

		ProjectEditor editor = new ProjectEditor(project);

		add(BorderLayout.CENTER, editor);

		status = new JLabel("READY");
		discoveryStatus = new JLabel();
		JPanel south = new JPanel(new BorderLayout());
		south.add(BorderLayout.CENTER, status);
		south.add(BorderLayout.EAST, discoveryStatus);

		add(BorderLayout.SOUTH, south);

		updateDiscoveryStatus();

		JMenuBar menu = new JMenuBar();

		// File menu
		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic('F');

		JMenuItem fileMenuListLasers = new JMenuItem("Laser Discovery...");
		fileMenuListLasers.setMnemonic('L');
		fileMenuListLasers.addActionListener(e -> {
			LaserDiscovery discovery = new LaserDiscovery(MainWindow.this, processor);
			discovery.setVisible(true);
		});

		JMenuItem fileMenuOpen = new JMenuItem("Open...");
		fileMenuOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, MENU_MODIFIER));
		fileMenuOpen.setMnemonic('O');
		fileMenuOpen.addActionListener(e -> {
			loadDialog.setVisible(true);
			if(loadDialog.getFile() == null) {
				return;
			}
			String filename = loadDialog.getDirectory() + loadDialog.getFile();
			try {
				load(new File(filename));
				setStatus("Project loaded from " + filename);
			} catch(IOException ex) {
				log.log(Levels.WARNING, "Failed to load project: " + ex.getMessage(), ex);
				setStatus("I/O Error: " + ex.getMessage());
			}
		});

		JMenuItem fileMenuSave = new JMenuItem("Save...");
		fileMenuSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, MENU_MODIFIER));
		fileMenuSave.setMnemonic('S');
		fileMenuSave.addActionListener(e -> {
			saveDialog.setVisible(true);
			if(saveDialog.getFile() == null) {
				return;
			}
			String filename = saveDialog.getDirectory() + saveDialog.getFile();
			try {
				save(new File(filename));
				setStatus("Project saved to " + filename);
			} catch(IOException ex) {
				log.log(Levels.WARNING, "Failed to save project: " + ex.getMessage(), ex);
				setStatus("I/O Error: " + ex.getMessage());
			}
		});

		JMenu fileMenuImport = new JMenu("Import");
		fileMenuImport.setMnemonic('I');

		JMenuItem importMidi = new JMenuItem("Import MIDI file...");
		importMidi.setMnemonic('M');
		importMidi.addActionListener(e -> {
			importMidiDialog.setVisible(true);
			if(importMidiDialog.getFile() == null) {
				return;
			}
			String filename = importMidiDialog.getDirectory() + importMidiDialog.getFile();
			try {
				importMidi(new File(filename));
				setStatus("Sequence loaded from " + filename);
			} catch(IOException ex) {
				setStatus("I/O Error: " + ex.getMessage());
				log.log(Levels.ERROR, "Failed to load sequence from file", ex);
			}
		});

		fileMenuImport.add(importMidi);

		JMenuItem fileMenuSettings = new JMenuItem("Settings");
		fileMenuSettings.setMnemonic('S');
		fileMenuSettings.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
		fileMenuSettings.addActionListener(e -> {
			SettingsDialog settings = new SettingsDialog(sys);
			settings.setLocationRelativeTo(this);
			settings.setVisible(true);
		});

		JMenuItem fileMenuExit = new JMenuItem("Exit");
		fileMenuExit.setMnemonic('x');
		fileMenuExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, MENU_MODIFIER));
		fileMenuExit.addActionListener(e -> {
			close();
			System.exit(0);
		});

		fileMenu.add(fileMenuListLasers);
		fileMenu.addSeparator();
		fileMenu.add(fileMenuOpen);
		fileMenu.add(fileMenuSave);
		fileMenu.addSeparator();
		fileMenu.add(fileMenuImport);
		fileMenu.addSeparator();
		fileMenu.add(fileMenuSettings);
		fileMenu.addSeparator();
		fileMenu.add(fileMenuExit);

		// Transport menu
		JMenu transportMenu = new JMenu("Transport");
		transportMenu.setMnemonic('T');

		JMenuItem transportMenuPlay = new JMenuItem("Play");
		transportMenuPlay.setMnemonic('y');
		transportMenuPlay.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0));
		transportMenuPlay.addActionListener(e -> {
			Sequencer seq = project.getSequencer();
			if(seq.isPlaying()) {
				long tick = seq.getTick();
				project.stop();
				seq.setTick(tick);
				setStatus("Playback stopped");
			} else {
				lastStartPosition = seq.getTick();
				project.play();
				setStatus("Playback started");
			}
		});

		JMenuItem transportMenuStop = new JMenuItem("Stop");
		transportMenuStop.setMnemonic('S');
		transportMenuStop.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0, 0));
		transportMenuStop.addActionListener(e -> {
			Sequencer seq = project.getSequencer();
			if(seq.isPlaying()) {
				long tick = seq.getTick();
				project.stop();
				seq.setTick(tick);
				setStatus("Playback stopped");
			} else {
				seq.setTick(lastStartPosition);
			}
		});

		JMenuItem transportMenuStart = new JMenuItem("Go to start");
		transportMenuStart.setMnemonic('G');
		transportMenuStart.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD1, 0));
		transportMenuStart.addActionListener(e -> project.getSequencer().setTick(0));

		transportMenu.add(transportMenuPlay);
		transportMenu.add(transportMenuStop);
		transportMenu.add(transportMenuStart);

		JMenu eslMenu = new JMenu("ESL");
		eslMenu.setMnemonic('E');

		JMenuItem eslMenuSynchronizeTime = new JMenuItem("Synchronize time");
		eslMenuSynchronizeTime.setMnemonic('t');
		eslMenuSynchronizeTime.addActionListener(e -> {
			try {
				sys.getESL().synchronizeTime();
			} catch(IOException ex) {
				error("Failed to send time: " + ex.getMessage(), ex);
			}
		});

		JMenuItem eslMenuReqDevices = new JMenuItem("Request devices");
		eslMenuReqDevices.setMnemonic('d');
		eslMenuReqDevices.addActionListener(e -> {
			try {
				sys.getESL().enumerateDevices();
			} catch(IOException ex) {
				error("Failed to request devices: " + ex.getMessage(), ex);
			}
		});

		JMenuItem eslMenuReset = new JMenuItem("Reset");
		eslMenuReset.setMnemonic('R');
		eslMenuReset.addActionListener(e -> {
			try {
				sys.getESL().reset();
			} catch(IOException ex) {
				error("Failed to reset system: " + ex.getMessage(), ex);
			}
		});

		eslMenu.add(eslMenuReqDevices);
		eslMenu.add(eslMenuSynchronizeTime);
		eslMenu.add(eslMenuReset);

		JMenu toolsMenu = new JMenu("Tools");

		JMenuItem toolsMenuBPM = new JMenuItem("BPM...");
		toolsMenuBPM.setMnemonic('B');
		toolsMenuBPM.addActionListener(e -> {
			BeatTapperDialog dlg = new BeatTapperDialog(this);
			dlg.setLocationRelativeTo(this);
			dlg.setVisible(true);
		});

		toolsMenu.add(toolsMenuBPM);

		JMenuItem help = new JMenuItem("Help");
		help.setMnemonic('H');
		help.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
		help.addActionListener(e -> {
			HelpBrowser dlg = new HelpBrowser(this);
			dlg.setLocationRelativeTo(this);
			dlg.setVisible(true);
		});

		JMenu helpMenu = new JMenu("Help");
		helpMenu.setMnemonic('H');
		helpMenu.add(help);

		menu.add(fileMenu);
		menu.add(transportMenu);
		menu.add(eslMenu);
		menu.add(toolsMenu);
		// hack, since JMenuBar::setHelpMenu is still not implemented after 25 years
		menu.add(Box.createGlue());
		menu.add(helpMenu);

		setJMenuBar(menu);

		setSize(1920, 1080);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		processor.addLaserDiscoveryListener(new LaserDiscoveryListener() {
			@Override
			public void laserDiscovered(LaserInfo laser) {
				SwingUtilities.invokeLater(() -> updateDiscoveryStatus());
			}

			@Override
			public void laserLost(LaserInfo laser) {
				SwingUtilities.invokeLater(() -> updateDiscoveryStatus());
			}
		});

		processor.addLaserConnectionListener(new LaserConnectionListener() {
			@Override
			public void laserConnected(Laser laser) {
				SwingUtilities.invokeLater(() -> updateDiscoveryStatus());
			}

			@Override
			public void laserDisconnected(Laser laser) {
				SwingUtilities.invokeLater(() -> updateDiscoveryStatus());
			}
		});

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				close();
			}
		});

		ProjectListener listener = new ProjectListener() {
			public void propertyChanged(String key) {
				switch(key) {
				case ProjectListener.NAME:
					updateTitle();
				}
			}

			public void trackAdded(Track<?> track) {
				// nothing
			}

			public void trackRemoved(Track<?> track) {
				// nothing
			}
		};

		project.addProjectListener(listener);

		MidiReceiver allBusReceiver = (st, data1, data2) -> project.inputAllBus(st, data1, data2);
		project.getSystem().getMidiRouter().addAllBusReceiver(allBusReceiver);

		updateTitle();

		setIconImages(List.of(Icons.get(Icons.EMULIGHT, 16).getImage(),
				Icons.get(Icons.EMULIGHT, 32).getImage(),
				Icons.get(Icons.EMULIGHT, 48).getImage(),
				Icons.get(Icons.EMULIGHT, 64).getImage()));
	}

	private void error(String msg, Exception e) {
		log.log(Levels.WARNING, msg, e);
		setStatus(msg);
		MessageBox.showError(this, msg);
	}

	private void updateTitle() {
		String title = project.getName();
		if(title == null) {
			setTitle(TITLE);
		} else {
			setTitle(title + " - " + TITLE);
		}
	}

	private void close() {
		sys.shutdown();
	}

	public void setStatus(String text) {
		status.setText(text);
	}

	public void load(File file) throws IOException {
		try(InputStream in = new BufferedInputStream(new FileInputStream(file))) {
			Element xml = XMLReader.read(in);
			project.load(xml);
		}
	}

	public void save(File file) throws IOException {
		Element xml = project.write();
		Files.write(file.toPath(), xml.toString().getBytes(StandardCharsets.UTF_8));
	}

	public void importMidi(File file) throws IOException {
		try(InputStream in = new BufferedInputStream(new FileInputStream(file))) {
			SMF smf = new SMF(in);
			importMidi(smf);
		}
	}

	public void importMidi(SMF smf) {
		int smfPPQ = smf.getHeader().getPPQ();
		int projectPPQ = project.getPPQ();

		TempoTrack tempoTrack = project.getTempoTrack();
		tempoTrack.setTempo(0, 120);

		for(MTrk miditrk : smf.getTracks()) {
			for(Event event : miditrk.getEvents()) {
				if(event instanceof TempoEvent) {
					TempoEvent tempo = (TempoEvent) event;

					long time = tempo.getTime() * projectPPQ / smfPPQ;
					double bpm = tempo.getBPM();

					tempoTrack.setTempo(time, bpm);
				}
			}

			if(miditrk.hasNonMetaEvents()) {
				// add this track
				String name = miditrk.getTrackName();
				if(name == null) {
					name = "Untitled";
				}

				MidiTrack track = new MidiTrack(project, name);
				project.addTrack(track);

				MidiPart part = new MidiPart(projectPPQ);
				part.loadTrack(miditrk, smfPPQ, projectPPQ);
				part.setName(name);

				// detect channel
				int channel = part.getChannel();
				if(channel == -1) {
					track.setChannel(MidiTrack.ANY);
				} else {
					track.setChannel(channel);
				}

				// round part size to grid
				long first = part.getFirstTick();
				if(first % projectPPQ != 0) {
					first -= first % projectPPQ;
				}

				part.move(-first);

				PartContainer<?> container = track.addPart(first, part);
				long len = part.getLength();
				if(len % projectPPQ != 0) {
					len += (projectPPQ - (len % projectPPQ));
					container.setLength(len);
				}
			}
		}
	}

	private void updateDiscoveryStatus() {
		int available = processor.getAvailableLasers().size();
		int connected = processor.getLasers().size();
		discoveryStatus.setText("Lasers: " + connected + " / " + available);
	}

	public static void setupLookAndFeel(SystemConfiguration cfg) {
		boolean decorations = true;

		try {
			UIManager.setLookAndFeel(cfg.getLookAndFeel().getUIClass());
			decorations = cfg.getWindowDecorations();
		} catch(ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			log.log(Levels.INFO, "Could not load motif look and feel: " + e.getMessage());
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				decorations = true;
			} catch(ClassNotFoundException | InstantiationException | IllegalAccessException
					| UnsupportedLookAndFeelException ex) {
				log.log(Levels.WARNING, "Failed to set system look and feel: " + ex.getMessage());
			}
		}

		JFrame.setDefaultLookAndFeelDecorated(decorations);
		JDialog.setDefaultLookAndFeelDecorated(decorations);
	}

	public static void main(String[] args) throws IOException {
		Trace.setup();

		EmulightSystem sys = new EmulightSystem();
		setupLookAndFeel(sys.getConfig());

		MainWindow main = new MainWindow(sys);
		main.setVisible(true);
	}
}
