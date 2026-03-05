package com.unknown.emulight.lcp.laser.ui;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.InetAddress;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import com.unknown.emulight.lcp.laser.LaserProcessor;
import com.unknown.emulight.lcp.laser.Project;
import com.unknown.net.shownet.LaserDiscoveryListener;
import com.unknown.net.shownet.LaserInfo;
import com.unknown.util.log.Trace;

@SuppressWarnings("serial")
public class MainWindow extends JFrame {
	private static final int MENU_MODIFIER = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

	private final LaserProcessor processor;

	private final JLabel discoveryStatus;

	private Project project;

	public MainWindow() throws IOException {
		super("Emulight Laser Control Program");

		setLayout(new BorderLayout());

		processor = new LaserProcessor(60);
		processor.addDiscoveryAddress(InetAddress.getByName("172.16.10.69"));

		project = new Project(processor);

		TimelineEditor timelineEditor = new TimelineEditor(project);
		ClipEditor clipEditor = new ClipEditor(project);

		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("Timeline", timelineEditor);
		tabs.addTab("Clip", clipEditor);
		add(BorderLayout.CENTER, tabs);

		discoveryStatus = new JLabel();
		JPanel south = new JPanel(new BorderLayout());
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

		JMenuItem fileMenuExit = new JMenuItem("Exit");
		fileMenuExit.setMnemonic('x');
		fileMenuExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, MENU_MODIFIER));
		fileMenuExit.addActionListener(e -> System.exit(0));

		fileMenu.add(fileMenuListLasers);
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

	private void updateDiscoveryStatus() {
		int available = processor.getAvailableLasers().size();
		int connected = processor.getLasers().size();
		discoveryStatus.setText("Lasers: " + connected + " / " + available);
	}

	public static void main(String[] args) throws IOException {
		Trace.setup();
		MainWindow main = new MainWindow();
		main.setVisible(true);
	}
}
