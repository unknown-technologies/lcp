package com.unknown.emulight.lcp.laser.ui;

import java.awt.BorderLayout;
import java.awt.FileDialog;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.logging.Logger;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.unknown.emulight.lcp.laser.Clip;
import com.unknown.emulight.lcp.laser.LaserProcessor;
import com.unknown.emulight.lcp.laser.Project;
import com.unknown.net.shownet.LaserDiscoveryListener;
import com.unknown.net.shownet.LaserInfo;
import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;
import com.unknown.xml.dom.Element;
import com.unknown.xml.dom.XMLReader;

@SuppressWarnings("serial")
public class MainWindow extends JFrame {
	private static final Logger log = Trace.create(MainWindow.class);

	private static final int MENU_MODIFIER = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

	private final LaserProcessor processor;

	private final JLabel discoveryStatus;
	private final JLabel status;

	private Project project;

	private ClipEditor clipEditor;

	public MainWindow() throws IOException {
		super("Emulight Laser Control Program");

		setLayout(new BorderLayout());

		FileDialog loadDialog = new FileDialog(this, "Open...", FileDialog.LOAD);
		FileDialog saveDialog = new FileDialog(this, "Save...", FileDialog.SAVE);

		processor = new LaserProcessor(60);
		processor.addDiscoveryAddress(InetAddress.getByName("172.16.10.69"));

		project = new Project(processor);

		TimelineEditor timelineEditor = new TimelineEditor(project);
		clipEditor = new ClipEditor(this, project);

		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("Timeline", timelineEditor);
		tabs.addTab("Clip", clipEditor);
		add(BorderLayout.CENTER, tabs);

		status = new JLabel("READY");
		discoveryStatus = new JLabel();
		JPanel south = new JPanel(new BorderLayout());
		south.add(BorderLayout.CENTER, status);
		south.add(BorderLayout.EAST, discoveryStatus);

		add(BorderLayout.SOUTH, south);

		updateDiscoveryStatus();

		JMenuBar menu = new JMenuBar();

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

		JMenuItem fileMenuExit = new JMenuItem("Exit");
		fileMenuExit.setMnemonic('x');
		fileMenuExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, MENU_MODIFIER));
		fileMenuExit.addActionListener(e -> System.exit(0));

		fileMenu.add(fileMenuListLasers);
		fileMenu.addSeparator();
		fileMenu.add(fileMenuOpen);
		fileMenu.add(fileMenuSave);
		fileMenu.addSeparator();
		fileMenu.add(fileMenuExit);

		menu.add(fileMenu);

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
	}

	public void setStatus(String text) {
		status.setText(text);
	}

	public void load(File file) throws IOException {
		try(InputStream in = new BufferedInputStream(new FileInputStream(file))) {
			Element xml = XMLReader.read(in);
			Clip clip = Clip.read(project, xml);
			clipEditor.setClip(clip);
		}
	}

	public void save(File file) throws IOException {
		Clip clip = clipEditor.getClip();
		Element xml = clip.write();
		Files.write(file.toPath(), xml.toString().getBytes(StandardCharsets.UTF_8));
	}

	private void updateDiscoveryStatus() {
		int available = processor.getAvailableLasers().size();
		int connected = processor.getLasers().size();
		discoveryStatus.setText("Lasers: " + connected + " / " + available);
	}

	public static void setupLookAndFeel() {
		boolean decorations = false;

		try {
			UIManager.setLookAndFeel("com.unknown.plaf.motif.MotifLookAndFeel");
			decorations = true;
		} catch(ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			log.log(Levels.INFO, "Could not load motif look and feel: " + e.getMessage(), e);
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				decorations = true;
			} catch(ClassNotFoundException | InstantiationException | IllegalAccessException
					| UnsupportedLookAndFeelException ex) {
				log.log(Levels.WARNING, "Failed to set system look and feel: " + ex.getMessage(), ex);
			}
		}

		JFrame.setDefaultLookAndFeelDecorated(decorations);
		JDialog.setDefaultLookAndFeelDecorated(decorations);
	}

	public static void main(String[] args) throws IOException {
		Trace.setup();

		// setupLookAndFeel();

		MainWindow main = new MainWindow();
		main.setVisible(true);
	}
}
