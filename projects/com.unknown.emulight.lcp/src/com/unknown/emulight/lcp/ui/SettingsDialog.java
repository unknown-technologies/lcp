package com.unknown.emulight.lcp.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

import javax.sound.midi.MidiDevice.Info;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.unknown.emulight.lcp.io.esl.ESL;
import com.unknown.emulight.lcp.io.esl.protocol.ESLDescriptor;
import com.unknown.emulight.lcp.io.midi.ESLMidiOut;
import com.unknown.emulight.lcp.io.midi.MidiInPort;
import com.unknown.emulight.lcp.io.midi.MidiOutPort;
import com.unknown.emulight.lcp.io.midi.MidiRouter;
import com.unknown.emulight.lcp.io.midi.NetworkMidiIn;
import com.unknown.emulight.lcp.io.midi.NetworkMidiOut;
import com.unknown.emulight.lcp.project.EmulightSystem;
import com.unknown.emulight.lcp.project.Project;
import com.unknown.emulight.lcp.project.SystemConfiguration.DMXPortConfig;
import com.unknown.emulight.lcp.project.SystemConfiguration.LaserConfig;
import com.unknown.emulight.lcp.project.SystemConfiguration.LookAndFeel;
import com.unknown.emulight.lcp.ui.laser.LaserControlDialog;
import com.unknown.emulight.lcp.ui.laser.LaserInfoDialog;
import com.unknown.emulight.lcp.ui.resources.icons.Icons;
import com.unknown.net.shownet.InterfaceId;
import com.unknown.net.shownet.Laser;
import com.unknown.net.shownet.Laser.State;
import com.unknown.net.shownet.LaserConnectionListener;
import com.unknown.net.shownet.LaserDiscoveryListener;
import com.unknown.net.shownet.LaserInfo;
import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;
import com.unknown.util.ui.ExtendedTableModel;
import com.unknown.util.ui.LabeledPairLayout;
import com.unknown.util.ui.LabeledPairLayout.LayoutGroup;
import com.unknown.util.ui.MixedTable;

@SuppressWarnings("serial")
public class SettingsDialog extends JDialog {
	private static final Logger log = Trace.create(SettingsDialog.class);

	private static final int MENU_MODIFIER = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

	private MidiInPort[] inputs;
	private MidiOutPort[] outputs;

	private ESLMidiOut[] eslOutputs;

	private MidiInModel midiInModel;
	private MidiOutModel midiOutModel;

	private ESLMidiInModel eslMidiInModel;
	private ESLMidiOutModel eslMidiOutModel;

	private LaserModel laserModel;

	private DMXModel dmxModel;

	private final ESL esl;
	private final MidiRouter router;

	private EmulightSystem sys;
	private LaserDiscoveryListener laserDiscoveryListener;
	private LaserConnectionListener laserConnectionListener;

	private AudioDelayMeasurementTool audioDelayMeasurement;

	public SettingsDialog(Project project) {
		super(project.getSystem().getMainWindow(), "Settings", true);
		setIconImages(List.of(Icons.getImage(Icons.SETTINGS, 32), Icons.getImage(Icons.SETTINGS, 16)));
		setSize(640, 480);

		this.sys = project.getSystem();

		esl = sys.getESL();
		router = sys.getMidiRouter();

		audioDelayMeasurement = new AudioDelayMeasurementTool(project);

		// GENERAL TAB
		JPanel general = new JPanel(new GridLayout(2, 1));

		JComboBox<String> serial = new JComboBox<>(SerialLineInfo.getSerialLines());
		serial.setEditable(true);
		serial.setSelectedItem(sys.getConfig().getSerialLine());
		serial.addItemListener(e -> {
			String text = (String) serial.getSelectedItem();
			if(text != null) {
				text = text.trim();
				if(text.length() > 0) {
					sys.getConfig().setSerialLine(text);
				}
			}
		});

		JLabel status = new JLabel("<unknown>");
		JLabel address = new JLabel("<unknown>");

		address.setText(Integer.toString(esl.getRouter()));

		String openclose = esl.isOpen() ? "Connected" : "Disconnected";
		if(esl.isError()) {
			status.setText(openclose + ", Error");
		} else {
			status.setText(openclose);
		}

		LayoutGroup generalLayoutGroup = new LayoutGroup();
		JPanel pcif = new JPanel(new LabeledPairLayout(generalLayoutGroup));
		pcif.setBorder(BorderFactory.createTitledBorder("Connection"));
		pcif.add(LabeledPairLayout.LABEL, new JLabel("Serial Line:"));
		pcif.add(LabeledPairLayout.COMPONENT, serial);
		pcif.add(LabeledPairLayout.LABEL, new JLabel("Status:"));
		pcif.add(LabeledPairLayout.COMPONENT, status);
		pcif.add(LabeledPairLayout.LABEL, new JLabel("Address:"));
		pcif.add(LabeledPairLayout.COMPONENT, address);

		JTextField defaultAuthor = new JTextField(project.getAuthor());
		defaultAuthor.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				String author = defaultAuthor.getText().trim();
				if(author.length() == 0) {
					author = null;
				}
				sys.getConfig().setDefaultAuthor(author);
			}
		});
		defaultAuthor.setText(sys.getConfig().getDefaultAuthor());

		JPanel projectDefaults = new JPanel(new LabeledPairLayout(generalLayoutGroup));
		projectDefaults.setBorder(BorderFactory.createTitledBorder("Project Defaults"));
		projectDefaults.add(LabeledPairLayout.LABEL, new JLabel("Author:"));
		projectDefaults.add(LabeledPairLayout.COMPONENT, defaultAuthor);

		general.add(pcif);
		general.add(projectDefaults);

		// MIDI TAB
		JPanel midi = new JPanel(new BorderLayout());

		JPanel midiIn = new JPanel(new BorderLayout());
		midiIn.setBorder(BorderFactory.createTitledBorder("MIDI Inputs"));
		JTable midiInTable = new MixedTable(midiInModel = new MidiInModel());
		midiInTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane midiInScroller = new JScrollPane(midiInTable);
		midiIn.add(BorderLayout.CENTER, midiInScroller);

		JPopupMenu udpMidiInPopup = new JPopupMenu();

		JMenuItem udpMidiInAdd = new JMenuItem("Add");
		udpMidiInAdd.addActionListener(e -> {
			router.addNetworkMidiInPort("unnamed network port");
			updateMidiInputs();
		});

		JMenuItem udpMidiInRemove = new JMenuItem("Remove");
		udpMidiInRemove.addActionListener(e -> {
			int row = midiInTable.getSelectedRow();
			if(row == -1) {
				return;
			} else {
				MidiInPort port = inputs[row];
				if(port instanceof NetworkMidiIn) {
					NetworkMidiIn netport = (NetworkMidiIn) port;
					netport.delete();
					updateMidiInputs();
				}
			}
		});

		udpMidiInPopup.add(udpMidiInAdd);
		udpMidiInPopup.add(udpMidiInRemove);
		midiInTable.setComponentPopupMenu(udpMidiInPopup);
		midiInScroller.setComponentPopupMenu(udpMidiInPopup);
		udpMidiInPopup.addPopupMenuListener(new PopupMenuListener() {
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						int row = midiInTable.rowAtPoint(SwingUtilities.convertPoint(
								udpMidiInPopup, new Point(0, 0), midiInTable));
						if(row != -1) {
							midiInTable.setRowSelectionInterval(row, row);
						}

						row = midiInTable.getSelectedRow();
						if(row != -1) {
							boolean en = midiInModel
									.getPort(row) instanceof NetworkMidiIn;
							udpMidiInRemove.setEnabled(en);
						} else {
							udpMidiInRemove.setEnabled(false);
						}
					}
				});
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
				// unused
			}

			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {
				// unused
			}
		});

		JPanel midiOut = new JPanel(new BorderLayout());
		midiOut.setBorder(BorderFactory.createTitledBorder("MIDI Outputs"));
		JTable midiOutTable = new MixedTable(midiOutModel = new MidiOutModel());
		midiOutTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane midiOutScroller = new JScrollPane(midiOutTable);
		midiOut.add(BorderLayout.CENTER, midiOutScroller);

		JPopupMenu udpMidiOutPopup = new JPopupMenu();

		JMenuItem udpMidiOutAdd = new JMenuItem("Add");
		udpMidiOutAdd.addActionListener(e -> {
			router.addNetworkMidiOutPort("unnamed network port");
			updateMidiOutputs();
		});

		JMenuItem udpMidiOutRemove = new JMenuItem("Remove");
		udpMidiOutRemove.addActionListener(e -> {
			int row = midiOutTable.getSelectedRow();
			if(row == -1) {
				return;
			} else {
				MidiOutPort port = outputs[row];
				if(port instanceof NetworkMidiOut) {
					NetworkMidiOut netport = (NetworkMidiOut) port;
					netport.delete();
					updateMidiOutputs();
				}
			}
		});

		udpMidiOutPopup.add(udpMidiOutAdd);
		udpMidiOutPopup.add(udpMidiOutRemove);
		midiOutTable.setComponentPopupMenu(udpMidiOutPopup);
		midiOutScroller.setComponentPopupMenu(udpMidiOutPopup);
		udpMidiOutPopup.addPopupMenuListener(new PopupMenuListener() {
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						int row = midiOutTable.rowAtPoint(SwingUtilities.convertPoint(
								udpMidiOutPopup, new Point(0, 0), midiOutTable));
						if(row != -1) {
							midiOutTable.setRowSelectionInterval(row, row);
						}

						row = midiOutTable.getSelectedRow();
						if(row != -1) {
							boolean en = midiOutModel
									.getPort(row) instanceof NetworkMidiOut;
							udpMidiOutRemove.setEnabled(en);
						} else {
							udpMidiOutRemove.setEnabled(false);
						}
					}
				});
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
				// unused
			}

			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {
				// unused
			}
		});

		JPanel midiInOut = new JPanel(new GridLayout(2, 1));
		midiInOut.add(midiIn);
		midiInOut.add(midiOut);
		midi.add(BorderLayout.CENTER, midiInOut);

		// ESL MIDI TAB
		JPanel eslMidi = new JPanel(new BorderLayout());

		JPanel eslMidiIn = new JPanel(new BorderLayout());
		eslMidiIn.setBorder(BorderFactory.createTitledBorder("ESL MIDI Inputs"));
		JTable eslMidiInTable = new MixedTable(eslMidiInModel = new ESLMidiInModel());
		eslMidiInTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		eslMidiIn.add(BorderLayout.CENTER, new JScrollPane(eslMidiInTable));

		JPanel eslMidiOut = new JPanel(new BorderLayout());
		eslMidiOut.setBorder(BorderFactory.createTitledBorder("ESL MIDI Outputs"));
		JTable eslMidiOutTable = new MixedTable(eslMidiOutModel = new ESLMidiOutModel());
		eslMidiOutTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane eslMidiOutScroller = new JScrollPane(eslMidiOutTable);
		eslMidiOut.add(BorderLayout.CENTER, eslMidiOutScroller);

		JPopupMenu eslMidiOutPopup = new JPopupMenu();

		JMenuItem eslMidiOutAdd = new JMenuItem("Add");
		eslMidiOutAdd.addActionListener(e -> {
			router.addESLMidiOutPort("unnamed ESL port");
			updateESLOutputs();
		});

		JMenuItem eslMidiOutRemove = new JMenuItem("Remove");
		eslMidiOutRemove.addActionListener(e -> {
			int row = eslMidiOutTable.getSelectedRow();
			if(row == -1) {
				return;
			} else {
				ESLMidiOut port = eslOutputs[row];
				port.delete();
				updateESLOutputs();
			}
		});

		eslMidiOutPopup.add(eslMidiOutAdd);
		eslMidiOutPopup.add(eslMidiOutRemove);
		eslMidiOutTable.setComponentPopupMenu(eslMidiOutPopup);
		eslMidiOutScroller.setComponentPopupMenu(eslMidiOutPopup);
		eslMidiOutPopup.addPopupMenuListener(new PopupMenuListener() {
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						int row = eslMidiOutTable.rowAtPoint(SwingUtilities.convertPoint(
								eslMidiOutPopup, new Point(0, 0), eslMidiOutTable));
						if(row != -1) {
							eslMidiOutTable.setRowSelectionInterval(row, row);
						}

						row = eslMidiOutTable.getSelectedRow();
						eslMidiOutRemove.setEnabled(row != -1);
					}
				});
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
				// unused
			}

			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {
				// unused
			}
		});

		JPanel eslMidiInOut = new JPanel(new GridLayout(2, 1));
		eslMidiInOut.add(eslMidiIn);
		eslMidiInOut.add(eslMidiOut);
		eslMidi.add(BorderLayout.CENTER, eslMidiInOut);

		// AUDIO TAB
		JPanel audio = new JPanel(new BorderLayout());
		JComboBox<Integer> sampleRate = new JComboBox<>(new Integer[] { 32000, 44100, 48000 });
		sampleRate.addItemListener(e -> {
			int value = (int) sampleRate.getSelectedItem();
			sys.getConfig().setSampleRate(value);
			audioDelayMeasurement.update();
		});
		sampleRate.setSelectedItem(sys.getConfig().getSampleRate());

		JComboBox<Integer> blockSize = new JComboBox<>(new Integer[] { 512, 1024, 2048 });
		blockSize.addItemListener(e -> {
			int value = (int) blockSize.getSelectedItem();
			sys.getConfig().setBlockSize(value);
		});
		blockSize.setSelectedItem(sys.getConfig().getBlockSize());

		JSpinner outputDelay = new JSpinner(new SpinnerNumberModel(
				(int) (sys.getConfig().getOutputDelay() / 1_000_000), 0, Integer.MAX_VALUE, 1));
		outputDelay.addChangeListener(e -> {
			int value = (int) outputDelay.getValue();
			sys.getConfig().setOutputDelay(value * 1_000_000);
		});

		JPanel outputDelayPanel = new JPanel(new BorderLayout());
		outputDelayPanel.add(BorderLayout.CENTER, outputDelay);
		outputDelayPanel.add(BorderLayout.EAST, new JLabel("ms"));

		JToggleButton measureDelay = new JToggleButton("Measure");
		measureDelay.setToolTipText("<html>Start/Stop the manual delay measurement process. When enabled:" +
				"<ul>" +
				"<li>All lasers output a flashing circle pattern</li>" +
				"<li>The audio system outputs a series of clicks</li>" +
				"</ul>" +
				"You have to adjust the output delay until the flashing circle and the click sound " +
				"happen at the same time.</html>");
		measureDelay.addActionListener(e -> {
			boolean state = measureDelay.isSelected();
			if(state) {
				audioDelayMeasurement.start();
			} else {
				audioDelayMeasurement.stop();
			}
		});

		JPanel delayMeasurement = new JPanel(new BorderLayout());
		delayMeasurement.add(BorderLayout.CENTER, outputDelayPanel);
		delayMeasurement.add(BorderLayout.EAST, measureDelay);

		JPanel audioConfig = new JPanel(new LabeledPairLayout());
		audioConfig.setBorder(BorderFactory.createTitledBorder("Connection"));
		audioConfig.add(LabeledPairLayout.LABEL, new JLabel("Sample Rate:"));
		audioConfig.add(LabeledPairLayout.COMPONENT, sampleRate);
		audioConfig.add(LabeledPairLayout.LABEL, new JLabel("Block Size:"));
		audioConfig.add(LabeledPairLayout.COMPONENT, blockSize);
		audioConfig.add(LabeledPairLayout.LABEL, new JLabel("Output Delay:"));
		audioConfig.add(LabeledPairLayout.COMPONENT, delayMeasurement);
		audio.add(BorderLayout.CENTER, audioConfig);

		// LASER TAB
		JPanel laser = new JPanel(new BorderLayout());
		JTable laserTable = new MixedTable(laserModel = new LaserModel());
		laserTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JButton laserDiscoveryButton = new JButton("Laser addresses...");
		laserDiscoveryButton.setToolTipText("Define additional addresses for lasers which cannot be " +
				"found via discovery. This is useful for lasers in remote subnets.");
		laserDiscoveryButton.addActionListener(e -> {
			LaserDiscoveryAddressDialog dlg = new LaserDiscoveryAddressDialog(this, sys);
			dlg.setLocationRelativeTo(this);
			dlg.setVisible(true);
		});
		JPanel laserButtons = new JPanel(new FlowLayout());
		laserButtons.add(laserDiscoveryButton);
		laser.add(BorderLayout.CENTER, new JScrollPane(laserTable));
		laser.add(BorderLayout.SOUTH, laserButtons);

		JPopupMenu laserPopup = new JPopupMenu();

		JMenuItem laserInfo = new JMenuItem("Info...");
		laserInfo.addActionListener(e -> {
			int row = laserTable.getSelectedRow();
			if(row == -1) {
				return;
			} else {
				LaserData info = laserModel.getLaser(row);
				LaserInfoDialog dlg = new LaserInfoDialog(this, sys, info.id);
				dlg.setLocationRelativeTo(this);
				dlg.setVisible(true);
			}
		});

		JMenuItem laserControl = new JMenuItem("Manual control...");
		laserControl.addActionListener(e -> {
			int row = laserTable.getSelectedRow();
			if(row == -1) {
				return;
			} else {
				LaserData info = laserModel.getLaser(row);
				LaserControlDialog dlg = new LaserControlDialog(this, sys, info.id);
				dlg.setLocationRelativeTo(this);
				dlg.setVisible(true);
			}
		});

		JMenuItem laserRemove = new JMenuItem("Remove");
		laserRemove.addActionListener(e -> {
			int row = laserTable.getSelectedRow();
			if(row == -1) {
				return;
			} else {
				LaserData info = laserModel.getLaser(row);
				if(info.address == null) {
					info.cfg.delete();
					laserModel.update();
				}
			}
		});

		laserPopup.add(laserInfo);
		laserPopup.add(laserControl);
		laserPopup.addSeparator();
		laserPopup.add(laserRemove);
		laserTable.setComponentPopupMenu(laserPopup);
		laserPopup.addPopupMenuListener(new PopupMenuListener() {
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						int row = laserTable.rowAtPoint(SwingUtilities.convertPoint(
								laserPopup, new Point(0, 0), laserTable));
						if(row != -1) {
							laserTable.setRowSelectionInterval(row, row);
						}

						row = laserTable.getSelectedRow();
						if(row != -1) {
							LaserData info = laserModel.getLaser(row);
							laserInfo.setEnabled(true);
							laserControl.setEnabled(info.address != null);
							laserRemove.setEnabled(info.address == null);
						} else {
							laserInfo.setEnabled(false);
							laserControl.setEnabled(false);
							laserRemove.setEnabled(false);
						}
					}
				});
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
				// unused
			}

			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {
				// unused
			}
		});

		// DMX TAB
		JPanel dmx = new JPanel(new BorderLayout());

		JTable dmxTable = new MixedTable(dmxModel = new DMXModel());
		dmxTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane dmxScroller = new JScrollPane(dmxTable);
		dmx.add(BorderLayout.CENTER, dmxScroller);

		JPopupMenu dmxPopup = new JPopupMenu();

		JMenuItem dmxAdd = new JMenuItem("Add");
		dmxAdd.addActionListener(e -> {
			String hostname = JOptionPane.showInputDialog(this, "ArtNet node address:",
					"Add ArtNet Node...", JOptionPane.OK_CANCEL_OPTION | JOptionPane.PLAIN_MESSAGE);
			if(hostname != null) {
				hostname = hostname.trim();
				if(hostname.length() > 0) {
					try {
						InetAddress addr = InetAddress.getByName(hostname);
						sys.getConfig().addDMXPort("unnamed DMX port", hostname, addr);
						dmxModel.update();
					} catch(UnknownHostException ex) {
						JOptionPane.showMessageDialog(this, "Host not found", "Error",
								JOptionPane.OK_OPTION | JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		});

		JMenuItem dmxRemove = new JMenuItem("Remove");
		dmxRemove.addActionListener(e -> {
			int row = dmxTable.getSelectedRow();
			if(row == -1) {
				return;
			} else {
				DMXPortConfig port = dmxModel.get(row);
				port.delete();
				dmxModel.update();
			}
		});

		dmxPopup.add(dmxAdd);
		dmxPopup.add(dmxRemove);
		dmxTable.setComponentPopupMenu(dmxPopup);
		dmxScroller.setComponentPopupMenu(dmxPopup);
		dmxPopup.addPopupMenuListener(new PopupMenuListener() {
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						int row = dmxTable.rowAtPoint(SwingUtilities.convertPoint(
								dmxPopup, new Point(0, 0), dmxTable));
						if(row != -1) {
							dmxTable.setRowSelectionInterval(row, row);
						}

						row = dmxTable.getSelectedRow();
						dmxRemove.setEnabled(row != -1);
					}
				});
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
				// unused
			}

			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {
				// unused
			}
		});

		// INTERFACE TAB
		JPanel ui = new JPanel(new LabeledPairLayout());

		LookAndFeel selectedLaf = sys.getConfig().getLookAndFeel();
		LookAndFeel[] lafs = LookAndFeel.values();
		String[] lafNames = new String[lafs.length];
		int lafSelectedIdx = 0;
		for(int i = 0; i < lafs.length; i++) {
			lafNames[i] = lafs[i].getName();
			if(lafs[i] == selectedLaf) {
				lafSelectedIdx = i;
			}
			if(lafs[i].getUIClassName() == null) {
				continue;
			}
			try {
				@SuppressWarnings("unchecked")
				Class<javax.swing.LookAndFeel> clazz = (Class<javax.swing.LookAndFeel>) Class
						.forName(lafs[i].getUIClass());
				javax.swing.LookAndFeel laf = clazz.getDeclaredConstructor().newInstance();
				lafNames[i] = laf.getName() + " [" + laf.getDescription() + "]";
			} catch(ClassNotFoundException | InstantiationException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException | NoSuchMethodException
					| SecurityException e) {
				log.log(Levels.WARNING, "Failed to load L&F class: " + e.getMessage());
			}
		}

		JComboBox<String> laf = new JComboBox<>(lafNames);
		laf.setSelectedIndex(lafSelectedIdx);
		laf.addItemListener(e -> {
			int idx = laf.getSelectedIndex();
			if(idx >= 0 && idx < lafs.length) {
				SwingUtilities.invokeLater(() -> sys.getConfig().setLookAndFeel(lafs[idx]));
			}
		});

		JCheckBox windowDecorations = new JCheckBox();
		windowDecorations.setToolTipText("<html>When checked, the design's window decorations are used. " +
				"When unchecked, the system's window decorations are used.<br/>" +
				"Changing this option requires a restart of the application.</html>");
		windowDecorations.setSelected(sys.getConfig().getWindowDecorations());
		windowDecorations.addChangeListener(e -> {
			sys.getConfig().setWindowDecorations(windowDecorations.isSelected());
		});

		ui.add(LabeledPairLayout.LABEL, new JLabel("Design:"));
		ui.add(LabeledPairLayout.COMPONENT, laf);
		ui.add(LabeledPairLayout.LABEL, new JLabel("Window Decorations:"));
		ui.add(LabeledPairLayout.COMPONENT, windowDecorations);

		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("General", general);
		tabs.addTab("MIDI", midi);
		tabs.addTab("ESL MIDI", eslMidi);
		tabs.addTab("Audio", audio);
		tabs.addTab("Laser", laser);
		tabs.addTab("DMX", dmx);
		tabs.addTab("Interface", ui);

		JButton close = new JButton("Close");
		close.addActionListener(e -> {
			close();
			dispose();
		});

		JButton save = new JButton("Save");
		save.addActionListener(e -> {
			try {
				sys.getConfig().write();
				close();
				dispose();
			} catch(IOException ex) {
				log.log(Levels.ERROR, "Failed to write config", ex);
			}
		});

		JPanel buttons = new JPanel(new FlowLayout());
		buttons.add(save);
		buttons.add(close);

		setLayout(new BorderLayout());
		add(BorderLayout.CENTER, tabs);
		add(BorderLayout.SOUTH, buttons);

		refreshMidi();

		laserDiscoveryListener = new LaserDiscoveryListener() {
			@Override
			public void laserDiscovered(LaserInfo info) {
				SwingUtilities.invokeLater(() -> laserModel.update());
			}

			@Override
			public void laserLost(LaserInfo info) {
				SwingUtilities.invokeLater(() -> laserModel.update());
			}
		};
		laserConnectionListener = new LaserConnectionListener() {
			@Override
			public void laserConnected(Laser l) {
				SwingUtilities.invokeLater(() -> laserModel.updateStatus());
			}

			public void laserDisconnected(Laser l) {
				SwingUtilities.invokeLater(() -> laserModel.updateStatus());
			}
		};
		sys.getLaserProcessor().addLaserDiscoveryListener(laserDiscoveryListener);
		sys.getLaserProcessor().addLaserConnectionListener(laserConnectionListener);

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				close();
			}
		});

		KeyStroke quitKey = KeyStroke.getKeyStroke(KeyEvent.VK_Q, MENU_MODIFIER);
		KeyStroke escKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		Object quit = new Object();
		JRootPane root = getRootPane();
		root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(quitKey, quit);
		root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escKey, quit);
		root.getActionMap().put(quit, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				close();
				dispose();
			}
		});

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}

	private void close() {
		audioDelayMeasurement.stop();
		audioDelayMeasurement.destroy();
		sys.getLaserProcessor().removeLaserConnectionListener(laserConnectionListener);
		sys.getLaserProcessor().removeLaserDiscoveryListener(laserDiscoveryListener);
	}

	private void refreshMidi() {
		MidiRouter midi = sys.getMidiRouter();
		inputs = midi.getInputs();
		outputs = midi.getOutputs();

		updateESLOutputs();

		midiInModel.update();
		midiOutModel.update();

		eslMidiInModel.update();
	}

	private void updateMidiInputs() {
		MidiRouter midi = sys.getMidiRouter();
		inputs = midi.getInputs();
		midiInModel.update();
	}

	private void updateMidiOutputs() {
		MidiRouter midi = sys.getMidiRouter();
		outputs = midi.getOutputs();
		midiOutModel.update();
	}

	private void updateESLOutputs() {
		eslOutputs = router.getESLOutputs();
		Arrays.sort(eslOutputs, (a, b) -> Integer.compareUnsigned(a.getAddress() << 8 | a.getPort(),
				b.getAddress() << 8 | b.getPort()));

		eslMidiOutModel.update();
	}

	private class MidiInModel extends ExtendedTableModel {
		@Override
		public int getColumnAlignment(int col) {
			return SwingConstants.LEFT;
		}

		@Override
		public String getColumnName(int col) {
			switch(col) {
			case 0:
				return "Name";
			case 1:
				return "Description";
			case 2:
				return "Vendor";
			case 3:
				return "Alias";
			case 4:
				return "Active";
			case 5:
				return "\"All\" Bus";
			default:
				return null;
			}
		}

		@Override
		public boolean isCellEditable(int row, int col) {
			if(inputs[row] instanceof NetworkMidiIn) {
				return col == 0 || col > 2;
			} else {
				return col > 2;
			}
		}

		@Override
		public int getColumnCount() {
			return 6;
		}

		@Override
		public int getRowCount() {
			return inputs.length;
		}

		@Override
		public Object getValueAt(int row, int col) {
			MidiInPort input = inputs[row];
			Info info = input.getInfo();
			switch(col) {
			case 0:
				return info.getName();
			case 1:
				return info.getDescription();
			case 2:
				return info.getVendor();
			case 3:
				return input.getAlias() == null ? "" : input.getAlias();
			case 4:
				return input.isActive();
			case 5:
				return input.isAll();
			default:
				return null;
			}
		}

		@Override
		public void setValueAt(Object value, int row, int col) {
			if(col == 0) {
				MidiInPort input = inputs[row];
				if(input instanceof NetworkMidiIn) {
					NetworkMidiIn netin = (NetworkMidiIn) input;
					String target = (String) value;
					int port;
					try {
						port = Integer.parseInt(target);
						if(port <= 0 || port > 65535) {
							return;
						}
					} catch(NumberFormatException e) {
						return;
					}
					netin.setPort(port);
				}
			} else if(col == 3) {
				String alias = (String) value;
				if(alias != null) {
					alias = alias.trim();
					if(alias.length() == 0) {
						alias = null;
					}
				}
				inputs[row].setAlias(alias);
			} else if(col == 4) {
				inputs[row].setActive((boolean) value);
			} else if(col == 5) {
				inputs[row].setAll((boolean) value);
			}
		}

		public MidiInPort getPort(int row) {
			return inputs[row];
		}

		public void update() {
			fireTableDataChanged();
		}
	}

	private class MidiOutModel extends ExtendedTableModel {
		@Override
		public int getColumnAlignment(int col) {
			return SwingConstants.LEFT;
		}

		@Override
		public String getColumnName(int col) {
			switch(col) {
			case 0:
				return "Name";
			case 1:
				return "Description";
			case 2:
				return "Vendor";
			case 3:
				return "Alias";
			case 4:
				return "Active";
			case 5:
				return "Send Clock";
			default:
				return null;
			}
		}

		@Override
		public boolean isCellEditable(int row, int col) {
			if(outputs[row] instanceof NetworkMidiOut) {
				return col == 0 || col > 2;
			} else {
				return col > 2;
			}
		}

		@Override
		public int getColumnCount() {
			return 6;
		}

		@Override
		public int getRowCount() {
			return outputs.length;
		}

		@Override
		public Object getValueAt(int row, int col) {
			MidiOutPort output = outputs[row];
			Info info = output.getInfo();
			switch(col) {
			case 0:
				return info.getName();
			case 1:
				return info.getDescription();
			case 2:
				return info.getVendor();
			case 3:
				return output.getAlias() == null ? "" : output.getAlias();
			case 4:
				return output.isActive();
			case 5:
				return output.isClock();
			default:
				return null;
			}
		}

		@Override
		public void setValueAt(Object value, int row, int col) {
			if(col == 0) {
				MidiOutPort output = outputs[row];
				if(output instanceof NetworkMidiOut) {
					NetworkMidiOut netout = (NetworkMidiOut) output;
					String target = (String) value;
					int split = target.indexOf(':');
					if(split == -1) {
						return;
					}
					String hostname = target.substring(0, split);
					String portname = target.substring(split + 1);
					int port;
					try {
						port = Integer.parseInt(portname);
						if(port <= 0 || port > 65535) {
							return;
						}
					} catch(NumberFormatException e) {
						return;
					}
					netout.setTarget(hostname, port);
				}
			} else if(col == 3) {
				String alias = (String) value;
				if(alias != null) {
					alias = alias.trim();
					if(alias.length() == 0) {
						alias = null;
					}
				}
				outputs[row].setAlias(alias);
			} else if(col == 4) {
				outputs[row].setActive((boolean) value);
			} else if(col == 5) {
				outputs[row].setClock((boolean) value);
			}
		}

		public MidiOutPort getPort(int row) {
			return outputs[row];
		}

		public void update() {
			fireTableDataChanged();
		}
	}

	private class ESLMidiInModel extends ExtendedTableModel {
		@Override
		public int getColumnAlignment(int col) {
			return SwingConstants.LEFT;
		}

		@Override
		public String getColumnName(int col) {
			switch(col) {
			case 0:
				return "Address";
			case 1:
				return "Port";
			case 2:
				return "Name";
			case 3:
				return "Alias";
			case 4:
				return "Active";
			default:
				return null;
			}
		}

		@Override
		public boolean isCellEditable(int row, int col) {
			return col > 2;
		}

		@Override
		public int getColumnCount() {
			return 5;
		}

		@Override
		public int getRowCount() {
			return 0;
		}

		@Override
		public Object getValueAt(int row, int col) {
			// TODO: implement ESL inputs
			return null;
		}

		@Override
		public void setValueAt(Object value, int row, int col) {
			// TODO: implement ESL inputs
		}

		public void update() {
			fireTableDataChanged();
		}
	}

	private class ESLMidiOutModel extends ExtendedTableModel {
		@Override
		public int getColumnAlignment(int col) {
			return SwingConstants.LEFT;
		}

		@Override
		public String getColumnName(int col) {
			switch(col) {
			case 0:
				return "Address";
			case 1:
				return "Port";
			case 2:
				return "Name";
			case 3:
				return "Alias";
			case 4:
				return "Active";
			default:
				return null;
			}
		}

		@Override
		public boolean isCellEditable(int row, int col) {
			return col != 2;
		}

		@Override
		public int getColumnCount() {
			return 5;
		}

		@Override
		public int getRowCount() {
			return eslOutputs.length;
		}

		@Override
		public Object getValueAt(int row, int col) {
			ESLMidiOut output = eslOutputs[row];
			switch(col) {
			case 0:
				return output.getAddress();
			case 1:
				return output.getPort();
			case 2: {
				ESLDescriptor dev = esl.getDescriptor(output.getAddress());
				if(dev != null) {
					return dev.getName();
				} else {
					return "<unconnected>";
				}
			}
			case 3:
				return output.getAlias() == null ? "" : output.getAlias();
			case 4:
				return output.isActive();
			default:
				return null;
			}
		}

		@Override
		public void setValueAt(Object value, int row, int col) {
			if(col == 0) {
				eslOutputs[row].setAddress((int) value);
				fireTableCellUpdated(row, 2);
				updateESLOutputs();
			} else if(col == 1) {
				eslOutputs[row].setPort((int) value);
				updateESLOutputs();
			} else if(col == 3) {
				String alias = (String) value;
				if(alias != null) {
					alias = alias.trim();
					if(alias.length() == 0) {
						alias = null;
					}
					eslOutputs[row].setAlias(alias);
				}
			} else if(col == 4) {
				eslOutputs[row].setActive((boolean) value);
			}
		}

		public void update() {
			fireTableDataChanged();
		}
	}

	private static class LaserData {
		public final InetAddress address;
		public final InterfaceId id;
		public LaserConfig cfg;

		public LaserData(InetAddress address, InterfaceId id) {
			this.address = address;
			this.id = id;
		}

		@Override
		public boolean equals(Object o) {
			if(o == null) {
				return false;
			}
			if(!(o instanceof LaserData)) {
				return false;
			}
			LaserData d = (LaserData) o;
			return Objects.equals(address, d.address) && Objects.equals(id, d.id) && (cfg == d.cfg);
		}

		@Override
		public int hashCode() {
			return Objects.hash(address, id, cfg);
		}
	}

	private class LaserModel extends ExtendedTableModel {
		private List<LaserData> lasers = new ArrayList<>();

		public LaserModel() {
			update();
		}

		public void update() {
			List<LaserData> newLasers = new ArrayList<>();

			Set<InterfaceId> discovered = new HashSet<>();
			Set<LaserInfo> discoveredLasers = sys.getLaserProcessor().getAvailableLasers();
			for(LaserInfo info : discoveredLasers) {
				InterfaceId id = info.getInterfaceId();
				if(id == null) {
					continue;
				}
				discovered.add(id);
				LaserData data = new LaserData(info.getAddress(), info.getInterfaceId());
				data.cfg = sys.getConfig().getLaser(id);
				newLasers.add(data);
			}

			for(LaserConfig cfg : sys.getConfig().getLasers()) {
				InterfaceId id = cfg.getId();
				if(id != null && discovered.contains(id)) {
					continue;
				} else {
					LaserData data = new LaserData(null, id);
					data.cfg = cfg;
					newLasers.add(data);
				}
			}

			Collections.sort(newLasers, (a, b) -> {
				if(a.address != null && b.address != null) {
					return a.address.getHostAddress().compareTo(b.address.getHostName());
				} else if(a.cfg != null && b.cfg != null) {
					return a.cfg.getName().compareTo(b.cfg.getName());
				} else {
					return 0;
				}
			});

			List<LaserData> oldLasers = lasers;
			lasers = newLasers;

			if(newLasers.size() != oldLasers.size()) {
				fireTableDataChanged();
			} else {
				boolean changes = false;
				for(int i = 0; i < oldLasers.size(); i++) {
					LaserData a = newLasers.get(i);
					LaserData b = oldLasers.get(i);
					if(!a.equals(b)) {
						changes = true;
						break;
					}
				}
				if(changes) {
					fireTableDataChanged();
				}
			}
		}

		public LaserData getLaser(int idx) {
			return lasers.get(idx);
		}

		@Override
		public int getColumnAlignment(int col) {
			return SwingConstants.LEFT;
		}

		@Override
		public String getColumnName(int col) {
			switch(col) {
			case 0:
				return "Address";
			case 1:
				return "Interface ID";
			case 2:
				return "Status";
			case 3:
				return "Name";
			case 4:
				return "Active";
			default:
				return null;
			}
		}

		@Override
		public boolean isCellEditable(int row, int col) {
			return col == 3 || col == 4;
		}

		@Override
		public int getColumnCount() {
			return 5;
		}

		@Override
		public int getRowCount() {
			return lasers.size();
		}

		@Override
		public Object getValueAt(int row, int col) {
			LaserData data = lasers.get(row);
			switch(col) {
			case 0:
				return data.address != null ? data.address.getHostAddress() : "<unknown>";
			case 1:
				return data.id != null ? data.id.toString() : "<unknown>";
			case 2:
				if(data.address == null) {
					return "Unavailable";
				} else {
					Laser laser = sys.getLaserProcessor().getLaser(data.address);
					if(laser == null) {
						return "Available";
					} else {
						State state = laser.getConnectionState();
						switch(state) {
						case INIT:
							return "Initializing";
						case CONFIGURING:
							return "Initializing";
						case CONNECTED:
							return "Connected";
						case DISCONNECTED:
							return "Disconnected";
						}
					}
				}
			case 3:
				if(data.cfg != null) {
					return data.cfg.getName();
				} else if(data.id != null) {
					return data.id.toString();
				} else {
					return "";
				}
			case 4:
				return data.cfg != null ? data.cfg.isActive() : false;
			default:
				return null;
			}
		}

		@Override
		public void setValueAt(Object value, int row, int col) {
			if(col == 3) {
				String name = (String) value;
				name = name.trim();
				if(name.length() == 0) {
					return;
				}

				LaserData info = lasers.get(row);
				if(info.id != null && info.cfg == null) {
					info.cfg = sys.getConfig().addLaser(name, info.id);
					fireTableRowsUpdated(row, row);
				} else if(info.cfg != null) {
					try {
						info.cfg.setName(name);
					} catch(IllegalArgumentException e) {
						// swallow
					}
				}
			} else if(col == 4) {
				boolean active = (boolean) value;

				LaserData info = lasers.get(row);
				if(info.cfg != null) {
					info.cfg.setActive(active);
				} else if(info.id != null) {
					String name = info.id.toString();
					info.cfg = sys.getConfig().addLaser(name, info.id);
					info.cfg.setActive(active);
					fireTableRowsUpdated(row, row);
				}
			}
		}

		public void updateStatus() {
			fireTableDataChanged();
		}
	}

	private class DMXModel extends ExtendedTableModel {
		private List<DMXPortConfig> ports = new ArrayList<>();

		private DMXModel() {
			update();
		}

		public void update() {
			ports.clear();
			ports.addAll(sys.getConfig().getDMXPorts());
			ports.sort((a, b) -> a.getHostname().compareTo(b.getHostname()));
			fireTableDataChanged();
		}

		public DMXPortConfig get(int index) {
			return ports.get(index);
		}

		@Override
		public int getColumnAlignment(int col) {
			return SwingConstants.LEFT;
		}

		@Override
		public String getColumnName(int col) {
			switch(col) {
			case 0:
				return "Hostname";
			case 1:
				return "Universe";
			case 2:
				return "Name";
			case 3:
				return "Active";
			default:
				return null;
			}
		}

		@Override
		public boolean isCellEditable(int row, int col) {
			return col > 0;
		}

		@Override
		public int getColumnCount() {
			return 4;
		}

		@Override
		public int getRowCount() {
			return ports.size();
		}

		@Override
		public Object getValueAt(int row, int col) {
			DMXPortConfig cfg = ports.get(row);
			switch(col) {
			case 0:
				return cfg.getHostname();
			case 1:
				return cfg.getUniverse();
			case 2:
				return cfg.getName();
			case 3:
				return cfg.isActive();
			default:
				return null;
			}
		}

		@Override
		public void setValueAt(Object value, int row, int col) {
			DMXPortConfig cfg = ports.get(row);
			switch(col) {
			case 1:
				cfg.setUniverse((int) value);
				break;
			case 2:
				try {
					cfg.setName((String) value);
				} catch(IllegalArgumentException e) {
					// swallow
				}
				break;
			case 3:
				cfg.setActive((boolean) value);
				break;
			}
		}
	}
}
