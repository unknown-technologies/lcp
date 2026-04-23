package com.unknown.emulight.lcp.project;

import java.awt.Window;
import java.io.IOException;
import java.net.InetAddress;
import java.util.logging.Logger;

import javax.sound.sampled.LineUnavailableException;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.unknown.emulight.lcp.audio.AudioProcessor;
import com.unknown.emulight.lcp.dmx.LightProcessor;
import com.unknown.emulight.lcp.event.ConfigChangeListener;
import com.unknown.emulight.lcp.io.esl.ESL;
import com.unknown.emulight.lcp.io.esl.ESLInterface;
import com.unknown.emulight.lcp.io.esl.SerialInterface;
import com.unknown.emulight.lcp.io.midi.MidiRouter;
import com.unknown.emulight.lcp.laser.LaserProcessor;
import com.unknown.emulight.lcp.laser.LaserReference;
import com.unknown.emulight.lcp.project.SystemConfiguration.DMXPortConfig;
import com.unknown.emulight.lcp.project.SystemConfiguration.LaserAddress;
import com.unknown.emulight.lcp.project.SystemConfiguration.LaserConfig;
import com.unknown.emulight.lcp.project.SystemConfiguration.LookAndFeel;
import com.unknown.emulight.lcp.project.SystemConfiguration.MidiPortConfig;
import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;

public class EmulightSystem {
	private static final Logger log = Trace.create(EmulightSystem.class);

	private SystemConfiguration config = new SystemConfiguration();

	private final LaserProcessor laser;
	private final LightProcessor light;
	private final AudioProcessor audio;
	private final MidiRouter midi;
	private final ESL esl;
	private final SerialInterface phy;

	private JFrame mainWindow;

	public EmulightSystem() throws IOException {
		laser = new LaserProcessor(config, 100);
		light = new LightProcessor(this, config, 40);
		audio = new AudioProcessor(config.getSampleRate());
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

		config.addConfigChangeListener(new ConfigChangeListener() {
			@Override
			public void configChanged(String key, String value) {
				switch(key) {
				case SystemConfiguration.LOOKANDFEEL:
					try {
						LookAndFeel laf = config.getLookAndFeel();
						UIManager.setLookAndFeel(laf.getUIClass());
						updateUI();
					} catch(ClassNotFoundException | InstantiationException | IllegalAccessException
							| UnsupportedLookAndFeelException e) {
						log.log(Levels.WARNING, "Failed to set L&F: " + e.getMessage());
					}
					break;
				case SystemConfiguration.WINDOW_DECORATIONS:
					JFrame.setDefaultLookAndFeelDecorated(config.getWindowDecorations());
					JDialog.setDefaultLookAndFeelDecorated(config.getWindowDecorations());
					break;
				case SystemConfiguration.BLOCK_SIZE:
					try {
						audio.setBlockSize(config.getBlockSize());
					} catch(LineUnavailableException e) {
						log.log(Levels.ERROR, "Cannot restart audio output: " + e.getMessage());
					}
					break;
				case SystemConfiguration.SAMPLE_RATE:
					try {
						audio.setSampleRate(config.getSampleRate());
					} catch(LineUnavailableException e) {
						log.log(Levels.ERROR, "Cannot restart audio output: " + e.getMessage());
					}
					break;
				}
			}

			@Override
			public void midiPortChanged(MidiPortConfig port) {
				// empty
			}

			@Override
			public void dmxPortChanged(DMXPortConfig port) {
				// empty
			}

			@Override
			public void laserChanged(LaserConfig cfg) {
				// empty
			}
		});
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

	public LightProcessor getLightProcessor() {
		return light;
	}

	public AudioProcessor getAudioProcessor() {
		return audio;
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
		try {
			audio.open();
		} catch(LineUnavailableException e) {
			log.log(Levels.ERROR, "Cannot open audio output: " + e.getMessage());
		}

		if(phy.getLine() != null) {
			phy.open();
		}
	}

	public void shutdown() {
		config.close();

		midi.closeAll();

		audio.close();

		laser.shutdown();

		try {
			phy.close();
		} catch(IOException e) {
			// ignore
		}
	}
}
