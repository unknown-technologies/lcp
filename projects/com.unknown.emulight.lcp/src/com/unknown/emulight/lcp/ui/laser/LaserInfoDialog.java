package com.unknown.emulight.lcp.ui.laser;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.html.StyleSheet;

import com.unknown.emulight.lcp.laser.LaserProcessor;
import com.unknown.emulight.lcp.project.EmulightSystem;
import com.unknown.emulight.lcp.ui.UIUtils;
import com.unknown.net.shownet.InterfaceId;
import com.unknown.net.shownet.Laser;
import com.unknown.net.shownet.Laser.State;
import com.unknown.net.shownet.LaserConnectionListener;
import com.unknown.net.shownet.LaserDiscoveryListener;
import com.unknown.net.shownet.LaserInfo;
import com.unknown.util.HexFormatter;
import com.unknown.util.ui.CopyableLabel;
import com.unknown.util.ui.ExtendedHTMLEditorKit;
import com.unknown.util.ui.LabeledPairLayout;

@SuppressWarnings("serial")
public class LaserInfoDialog extends JDialog {
	private static final int WIDTH = 16;

	private final EmulightSystem sys;

	private final LaserDiscoveryListener laserDiscoveryListener;
	private final LaserConnectionListener laserConnectionListener;

	private final InterfaceId id;

	private final CopyableLabel statusLabel;
	private final CopyableLabel addressLabel;
	private final CopyableLabel bootloaderLabel;
	private final CopyableLabel firmwareLabel;
	private final CopyableLabel hardwareId;
	private final CopyableLabel interfaceId;
	private final CopyableLabel macAddress;
	private final CopyableLabel generationInfo;
	private final CopyableLabel colorInfo;
	private final CopyableLabel timeInterval;
	private final CopyableLabel configAddress;
	private final CopyableLabel mode;
	private final CopyableLabel dongleStatus;
	private final CopyableLabel[] licenseInfo;

	private final JEditorPane rawConfig;
	private final JScrollPane rawConfigScroller;
	private int oldAddr = -1;
	private byte[] oldData = null;

	public LaserInfoDialog(JDialog parent, EmulightSystem sys, InterfaceId id) {
		super(parent, "Laser Information", ModalityType.APPLICATION_MODAL);

		this.sys = sys;
		this.id = id;

		statusLabel = new CopyableLabel();
		addressLabel = new CopyableLabel();
		bootloaderLabel = new CopyableLabel();
		firmwareLabel = new CopyableLabel();
		hardwareId = new CopyableLabel();
		interfaceId = new CopyableLabel();
		macAddress = new CopyableLabel();
		generationInfo = new CopyableLabel();
		colorInfo = new CopyableLabel();
		timeInterval = new CopyableLabel();
		configAddress = new CopyableLabel();
		mode = new CopyableLabel();
		dongleStatus = new CopyableLabel();

		licenseInfo = new CopyableLabel[8];
		for(int i = 0; i < licenseInfo.length; i++) {
			licenseInfo[i] = new CopyableLabel();
		}

		Color background = UIManager.getColor("Panel.background");
		String bgcolor = "#" + HexFormatter.tohex(background.getRGB() & 0xFFFFFF, 6);
		ExtendedHTMLEditorKit kit = new ExtendedHTMLEditorKit();
		StyleSheet defaultStyle = kit.getStyleSheet();
		StyleSheet style = new StyleSheet();
		style.addStyleSheet(defaultStyle);
		style.addRule("body { background-color: " + bgcolor + "; }");
		style.addRule("pre { white-space: nowrap; font-family: monospace; font-size: 11pt; }");
		kit.setStyleSheet(style);

		rawConfig = new JEditorPane();
		rawConfig.setEditorKit(kit);
		rawConfig.setContentType("text/html");
		rawConfig.setEditable(false);
		rawConfig.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
		rawConfig.setBackground(statusLabel.getBackground());
		rawConfig.setForeground(statusLabel.getForeground());
		Dimension scrollerSize = rawConfig.getPreferredSize();
		rawConfigScroller = new JScrollPane(rawConfig);
		rawConfigScroller.setPreferredSize(scrollerSize);

		JPanel infoPanel = new JPanel(new LabeledPairLayout());
		infoPanel.setBorder(UIUtils.padding());
		infoPanel.add(LabeledPairLayout.LABEL, new JLabel("Status:"));
		infoPanel.add(LabeledPairLayout.COMPONENT, statusLabel);
		infoPanel.add(LabeledPairLayout.LABEL, new JLabel("Address:"));
		infoPanel.add(LabeledPairLayout.COMPONENT, addressLabel);
		infoPanel.add(LabeledPairLayout.LABEL, new JLabel("Bootloader:"));
		infoPanel.add(LabeledPairLayout.COMPONENT, bootloaderLabel);
		infoPanel.add(LabeledPairLayout.LABEL, new JLabel("Firmware:"));
		infoPanel.add(LabeledPairLayout.COMPONENT, firmwareLabel);
		infoPanel.add(LabeledPairLayout.LABEL, new JLabel("Hardware ID:"));
		infoPanel.add(LabeledPairLayout.COMPONENT, hardwareId);
		infoPanel.add(LabeledPairLayout.LABEL, new JLabel("Interface ID:"));
		infoPanel.add(LabeledPairLayout.COMPONENT, interfaceId);
		infoPanel.add(LabeledPairLayout.LABEL, new JLabel("MAC Address:"));
		infoPanel.add(LabeledPairLayout.COMPONENT, macAddress);
		infoPanel.add(LabeledPairLayout.LABEL, new JLabel("Generation:"));
		infoPanel.add(LabeledPairLayout.COMPONENT, generationInfo);
		infoPanel.add(LabeledPairLayout.LABEL, new JLabel("Colors:"));
		infoPanel.add(LabeledPairLayout.COMPONENT, colorInfo);
		infoPanel.add(LabeledPairLayout.LABEL, new JLabel("Time Base:"));
		infoPanel.add(LabeledPairLayout.COMPONENT, timeInterval);
		infoPanel.add(LabeledPairLayout.LABEL, new JLabel("Config Address:"));
		infoPanel.add(LabeledPairLayout.COMPONENT, configAddress);
		infoPanel.add(LabeledPairLayout.LABEL, new JLabel("Operating Mode:"));
		infoPanel.add(LabeledPairLayout.COMPONENT, mode);
		infoPanel.add(LabeledPairLayout.LABEL, new JLabel("Dongle Status:"));
		infoPanel.add(LabeledPairLayout.COMPONENT, dongleStatus);
		infoPanel.add(LabeledPairLayout.LABEL, new JLabel("License:"));
		infoPanel.add(LabeledPairLayout.COMPONENT, licenseInfo[0]);
		for(int i = 1; i < licenseInfo.length; i++) {
			infoPanel.add(LabeledPairLayout.LABEL, new JPanel());
			infoPanel.add(LabeledPairLayout.COMPONENT, licenseInfo[i]);
		}

		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("General", infoPanel);
		tabs.addTab("Raw Config", rawConfigScroller);

		JButton close = new JButton("Close");
		close.addActionListener(e -> {
			dispose();
			close();
		});

		JPanel buttons = new JPanel(new FlowLayout());
		buttons.add(close);

		add(BorderLayout.CENTER, tabs);
		add(BorderLayout.SOUTH, buttons);

		update();

		laserDiscoveryListener = new LaserDiscoveryListener() {
			@Override
			public void laserDiscovered(LaserInfo info) {
				SwingUtilities.invokeLater(() -> update());
			}

			@Override
			public void laserLost(LaserInfo info) {
				SwingUtilities.invokeLater(() -> update());
			}
		};
		laserConnectionListener = new LaserConnectionListener() {
			@Override
			public void laserConnected(Laser l) {
				SwingUtilities.invokeLater(() -> update());
			}

			public void laserDisconnected(Laser l) {
				SwingUtilities.invokeLater(() -> update());
			}
		};
		LaserProcessor processor = sys.getLaserProcessor();
		processor.addLaserDiscoveryListener(laserDiscoveryListener);
		processor.addLaserConnectionListener(laserConnectionListener);

		pack();

		close.requestFocusInWindow();

		KeyStroke escKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		Object quit = new Object();
		JRootPane root = getRootPane();
		root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escKey, quit);
		root.getActionMap().put(quit, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
				close();
			}
		});

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				close();
			}
		});

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}

	private void close() {
		LaserProcessor processor = sys.getLaserProcessor();
		processor.removeLaserDiscoveryListener(laserDiscoveryListener);
		processor.removeLaserConnectionListener(laserConnectionListener);
	}

	// This function intentionally keeps old data in the fields. After all, there should only be one physical laser
	// with a specific interface ID in existence.
	private void update() {
		LaserProcessor processor = sys.getLaserProcessor();
		Optional<LaserInfo> result = processor.getAvailableLasers().stream()
				.filter(info -> id.equals(info.getInterfaceId())).findAny();
		if(result.isPresent()) {
			LaserInfo info = result.get();

			addressLabel.setText(info.getAddress().getHostAddress());
			bootloaderLabel.setText(info.getBootloaderString());
			firmwareLabel.setText(info.getFirmwareString());
			hardwareId.setText(Integer.toString(info.getHardwareId()));
			interfaceId.setText(info.getInterfaceId().toString());

			Laser laser = processor.getLaser(info.getAddress());
			statusLabel.setText(getState(laser));
			if(laser != null) {
				if(laser.isConnected()) {
					bootloaderLabel.setText(Integer.toUnsignedString(laser.getBootloader()));
					firmwareLabel.setText(Integer.toUnsignedString(laser.getFirmware()));
					hardwareId.setText(Integer.toString(laser.getHardwareId()));
				}

				macAddress.setText(laser.getMACAddressString());
				generationInfo.setText(Integer.toString(laser.getGeneration()));
				colorInfo.setText(laser.getColorCount() + " (" + laser.getBitPerChannel() +
						" bit per channel)");
				timeInterval.setText(Integer.toString(laser.getTimeInterval()));
				configAddress.setText(String.format("0x%08X", laser.getConfigAddress()));
				mode.setText(laser.getModeString());
				dongleStatus.setText(laser.getDongleStatusString());
				for(int i = 0; i < 8; i++) {
					licenseInfo[i].setText(laser.getLicenseString(i));
				}

				setRawData(laser);
			} else {
				for(int i = 0; i < 8; i++) {
					licenseInfo[i].setText(info.getLicenseString(i));
				}
			}
		} else {
			statusLabel.setText("Unavailable");
			interfaceId.setText(id.toString());
		}
	}

	private static String getState(Laser laser) {
		if(laser == null) {
			return "Available";
		}
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
		default:
			throw new AssertionError("unknown state");
		}
	}

	private void setRawData(Laser laser) {
		if(!laser.isConnected()) {
			return;
		}

		byte[] data = laser.getConfigMemory();
		int addr = laser.getConfigAddress();

		if(oldAddr != addr || oldData == null || !Arrays.equals(oldData, data)) {
			oldAddr = addr;
			oldData = data;

			StringBuilder buf = new StringBuilder();
			buf.append("<html><body><pre>");
			for(int i = 0; i < data.length; i++) {
				if((i % WIDTH) == 0) {
					buf.append(HexFormatter.tohex(addr + i, 8));
					buf.append(":");
				}
				buf.append(' ');
				buf.append(HexFormatter.tohex(Byte.toUnsignedInt(data[i]), 2));
				if((i % WIDTH) == WIDTH - 1) {
					// add the decoded text
					buf.append("   ");
					for(int j = 0; j < WIDTH; j++) {
						int c = Byte.toUnsignedInt(data[i - WIDTH + 1 + j]);
						if(c >= 0x20 && c < 0x7F) {
							buf.append(escape((char) c));
						} else {
							buf.append('.');
						}
					}
					buf.append("<br/>");
				}
			}

			if((data.length % WIDTH) != 0) {
				for(int i = (data.length % WIDTH); i < WIDTH; i++) {
					buf.append("   ");
				}

				buf.append("   ");

				for(int i = data.length - (data.length % WIDTH); i < data.length; i++) {
					int c = Byte.toUnsignedInt(data[i]);
					if(c >= 0x20 && c < 0x7F) {
						buf.append((char) c);
					} else {
						buf.append('.');
					}
				}
			}
			buf.append("</pre></body></html>");

			int pos = rawConfig.getCaretPosition();
			Point viewpos = rawConfigScroller.getViewport().getViewPosition();
			rawConfig.setText(buf.toString().trim());
			if(pos < rawConfig.getText().length()) {
				rawConfig.setCaretPosition(pos);
			}
			rawConfigScroller.getViewport().setViewPosition(viewpos);
		}
	}

	private static String escape(char c) {
		switch(c) {
		case '&':
			return "&amp;";
		case '<':
			return "&lt;";
		case '>':
			return "&gt;";
		default:
			return Character.toString(c);
		}
	}
}
