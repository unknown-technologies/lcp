package com.unknown.emulight.lcp.project;

import java.awt.Window;
import java.io.IOException;
import java.net.InetAddress;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.unknown.emulight.lcp.io.esl.ESL;
import com.unknown.emulight.lcp.io.esl.ESLInterface;
import com.unknown.emulight.lcp.io.esl.SerialInterface;
import com.unknown.emulight.lcp.io.midi.MidiRouter;
import com.unknown.emulight.lcp.laser.LaserProcessor;
import com.unknown.emulight.lcp.laser.LaserReference;
import com.unknown.emulight.lcp.project.SystemConfiguration.LaserAddress;
import com.unknown.emulight.lcp.project.SystemConfiguration.LaserConfig;

public class EmulightSystem {
	private SystemConfiguration config = new SystemConfiguration();

	private final LaserProcessor laser;
	private final MidiRouter midi;
	private final ESL esl;
	private final SerialInterface phy;

	private JFrame mainWindow;

	public EmulightSystem() throws IOException {
		laser = new LaserProcessor(this, 100);
		String line = config.getSerialLine();
		phy = new SerialInterface(line);
		esl = new ESLInterface(phy);
		midi = new MidiRouter(esl, config);

		for(LaserAddress addr : config.getLaserAddresses()) {
			InetAddress a = addr.getAddress();
			if(a != null) {
				laser.addDiscoveryAddress(a);
			}
		}
	}

	public SystemConfiguration getConfig() {
		return config;
	}

	public LaserProcessor getLaserProcessor() {
		return laser;
	}

	public LaserReference getLaser(String name) {
		LaserConfig cfg = config.getLaser(name);
		if(cfg == null) {
			return null;
		}
		return new LaserReference(this, cfg);
	}

	public MidiRouter getMidiRouter() {
		return midi;
	}

	public ESL getESL() {
		return esl;
	}

	public JFrame getMainWindow() {
		return mainWindow;
	}

	public void setMainWindow(JFrame mainWindow) {
		this.mainWindow = mainWindow;
	}

	public void updateUI() {
		boolean decorations = config.getWindowDecorations();
		JFrame.setDefaultLookAndFeelDecorated(decorations);
		JDialog.setDefaultLookAndFeelDecorated(decorations);

		for(Window window : Window.getWindows()) {
			if(window instanceof JFrame) {
				JFrame frame = (JFrame) window;
				SwingUtilities.updateComponentTreeUI(frame);
			} else if(window instanceof JDialog) {
				JDialog dialog = (JDialog) window;
				SwingUtilities.updateComponentTreeUI(dialog);
			}
		}
	}

	public void open() throws IOException {
		if(phy.getLine() != null) {
			phy.open();
		}
	}

	public void shutdown() {
		config.close();

		midi.closeAll();

		laser.shutdown();

		try {
			phy.close();
		} catch(IOException e) {
			// ignore
		}
	}
}
