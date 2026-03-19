package com.unknown.emulight.lcp.ui.laser;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionListener;

import com.unknown.emulight.lcp.laser.LaserProcessor;
import com.unknown.net.shownet.InterfaceId;
import com.unknown.net.shownet.Laser;
import com.unknown.net.shownet.LaserDiscoveryListener;
import com.unknown.net.shownet.LaserInfo;
import com.unknown.net.shownet.Point;
import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;
import com.unknown.util.ui.CopyableLabel;
import com.unknown.util.ui.LabeledPairLayout;

@SuppressWarnings("serial")
public class LaserDiscovery extends JDialog implements LaserDiscoveryListener {
	private static final Logger log = Trace.create(LaserDiscovery.class);

	private final LaserProcessor processor;

	private final DiscoveryModel discoveryModel;
	private final AddressModel addressModel;
	private final LaserModel laserModel;

	public LaserDiscovery(JFrame parent, LaserProcessor processor) {
		super(parent, "Laser Discovery", ModalityType.APPLICATION_MODAL);

		this.processor = processor;

		setLayout(new BorderLayout());

		JSplitPane discovery = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		discovery.setResizeWeight(0.4);

		discoveryModel = new DiscoveryModel();
		JList<String> addressList = new JList<>(discoveryModel);
		discovery.setLeftComponent(new JScrollPane(addressList));

		CopyableLabel addressLabel = new CopyableLabel();
		CopyableLabel bootloaderLabel = new CopyableLabel();
		CopyableLabel firmwareLabel = new CopyableLabel();
		CopyableLabel hardwareId = new CopyableLabel();
		CopyableLabel interfaceId = new CopyableLabel();
		CopyableLabel macAddress = new CopyableLabel();
		CopyableLabel generationInfo = new CopyableLabel();
		CopyableLabel colorInfo = new CopyableLabel();
		CopyableLabel timeInterval = new CopyableLabel();
		CopyableLabel configAddress = new CopyableLabel();

		JPanel infoPanel = new JPanel(new LabeledPairLayout());
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
		discovery.setRightComponent(infoPanel);

		ListSelectionListener addressListListener = e -> {
			int index = addressList.getSelectedIndex();
			if(index >= 0 && index < discoveryModel.getSize()) {
				LaserInfo info = discoveryModel.getLaser(index);
				addressLabel.setText(info.getAddress().getHostAddress());
				bootloaderLabel.setText(info.getBootloaderString());
				firmwareLabel.setText(info.getFirmwareString());
				hardwareId.setText(Integer.toString(info.getHardwareId()));
				interfaceId.setText(info.getInterfaceId().toString());

				Laser laser = processor.getLaser(info.getAddress());
				if(laser != null) {
					macAddress.setText(laser.getMACAddressString());
					generationInfo.setText(Integer.toString(laser.getGeneration()));
					colorInfo.setText(laser.getColorCount() + " (" + laser.getBitPerChannel() +
							" bit per channel)");
					timeInterval.setText(Integer.toString(laser.getTimeInterval()));
					configAddress.setText(String.format("0x%08X", laser.getConfigAddress()));
				} else {
					macAddress.setText("");
					generationInfo.setText("");
					colorInfo.setText("");
					timeInterval.setText("");
					configAddress.setText("");
				}
			} else {
				addressLabel.setText("");
				bootloaderLabel.setText("");
				firmwareLabel.setText("");
				hardwareId.setText("");
				interfaceId.setText("");
				macAddress.setText("");
				generationInfo.setText("");
				colorInfo.setText("");
				timeInterval.setText("");
				configAddress.setText("");
			}
		};
		addressList.addListSelectionListener(addressListListener);

		JPanel additionalLasers = new JPanel(new BorderLayout());
		addressModel = new AddressModel();
		JList<String> discoveryList = new JList<>(addressModel);
		additionalLasers.add(BorderLayout.CENTER, new JScrollPane(discoveryList));

		JPanel additionalLaserButtons = new JPanel(new FlowLayout());
		JButton addAddress = new JButton("+");
		addAddress.addActionListener(e -> {
			String hostname = JOptionPane.showInputDialog(this, "Laser address:", "Add Laser...",
					JOptionPane.OK_CANCEL_OPTION | JOptionPane.PLAIN_MESSAGE);
			if(hostname != null) {
				hostname = hostname.trim();
				if(hostname.length() > 0) {
					try {
						InetAddress addr = InetAddress.getByName(hostname);
						processor.addDiscoveryAddress(addr);
						addressModel.update();
					} catch(UnknownHostException e1) {
						JOptionPane.showMessageDialog(LaserDiscovery.this, "Host not found",
								"Error",
								JOptionPane.OK_OPTION | JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		});

		JButton removeAddress = new JButton("-");
		additionalLaserButtons.add(addAddress);
		additionalLaserButtons.add(removeAddress);
		additionalLasers.add(BorderLayout.SOUTH, additionalLaserButtons);

		removeAddress.setEnabled(false);
		removeAddress.addActionListener(e -> {
			int index = discoveryList.getSelectedIndex();
			if(index >= 0 && index < addressModel.getSize()) {
				InetAddress addr = addressModel.getAddress(index);
				processor.removeDiscoveryAddress(addr);
				addressModel.update();

				index = discoveryList.getSelectedIndex();
				removeAddress.setEnabled(index >= 0 && index < addressModel.getSize());
			} else {
				removeAddress.setEnabled(false);
			}
		});

		discoveryList.addListSelectionListener(e -> {
			int index = discoveryList.getSelectedIndex();
			if(index >= 0 && index < addressModel.getSize()) {
				removeAddress.setEnabled(true);
			} else {
				removeAddress.setEnabled(false);
			}
		});

		JPanel connected = new JPanel(new BorderLayout());
		laserModel = new LaserModel();

		JPanel connectedCenter = new JPanel(new GridLayout(1, 2));
		JList<String> availableLasers = new JList<>(discoveryModel);
		JList<String> connectedLasers = new JList<>(laserModel);

		JScrollPane availableLasersScroller = new JScrollPane(availableLasers);
		JScrollPane connectedLasersScroller = new JScrollPane(connectedLasers);

		availableLasersScroller.setBorder(BorderFactory.createTitledBorder("Available Lasers"));
		connectedLasersScroller.setBorder(BorderFactory.createTitledBorder("Connected Lasers"));

		connectedCenter.add(availableLasersScroller);
		connectedCenter.add(connectedLasersScroller);

		availableLasers.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(e.getClickCount() == 2) {
					int idx = availableLasers.getSelectedIndex();
					if(idx != -1) {
						LaserInfo laser = discoveryModel.getLaser(idx);
						try {
							processor.connect(laser.getAddress());
							laserModel.update();
							addressListListener.valueChanged(null);
						} catch(IOException ex) {
							log.log(Levels.ERROR, "Failed to connect to laser " +
									getName(laser) + ": " + ex.getMessage(), ex);
							JOptionPane.showMessageDialog(LaserDiscovery.this,
									"Failed to connect to laser " + getName(laser) +
											": " + ex.getMessage(),
									"Error", JOptionPane.OK_OPTION |
											JOptionPane.ERROR_MESSAGE);
						}
					}
				}
			}
		});

		connectedLasers.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(e.getClickCount() == 2) {
					int idx = connectedLasers.getSelectedIndex();
					if(idx != -1) {
						Laser laser = laserModel.getLaser(idx);
						processor.disconnect(laser);
						laserModel.update();
					}
				}
			}
		});

		connected.add(BorderLayout.CENTER, connectedCenter);

		JSplitPane manualControl = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		manualControl.setResizeWeight(0.4);
		JList<String> laserList = new JList<>(laserModel);
		manualControl.setLeftComponent(new JScrollPane(laserList));

		JPanel manualControls = new JPanel(new LabeledPairLayout());
		JSlider sliderX = new JSlider(0, 0xFFFF, 0x8000);
		JSlider sliderY = new JSlider(0, 0xFFFF, 0x8000);
		JSlider sliderRed = new JSlider(0, 0xFF, 0);
		JSlider sliderGreen = new JSlider(0, 0xFF, 0);
		JSlider sliderBlue = new JSlider(0, 0xFF, 0);
		manualControls.add(LabeledPairLayout.LABEL, new JLabel("Scanner X:"));
		manualControls.add(LabeledPairLayout.COMPONENT, sliderX);
		manualControls.add(LabeledPairLayout.LABEL, new JLabel("Scanner Y:"));
		manualControls.add(LabeledPairLayout.COMPONENT, sliderY);
		manualControls.add(LabeledPairLayout.LABEL, new JLabel("Red:"));
		manualControls.add(LabeledPairLayout.COMPONENT, sliderRed);
		manualControls.add(LabeledPairLayout.LABEL, new JLabel("Green:"));
		manualControls.add(LabeledPairLayout.COMPONENT, sliderGreen);
		manualControls.add(LabeledPairLayout.LABEL, new JLabel("Blue:"));
		manualControls.add(LabeledPairLayout.COMPONENT, sliderBlue);
		manualControl.setRightComponent(manualControls);

		ChangeListener manualChange = e -> {
			int idx = laserList.getSelectedIndex();
			if(idx >= 0 && idx <= laserModel.getSize()) {
				Laser laser = laserModel.getLaser(idx);

				int x = sliderX.getValue();
				int y = sliderY.getValue();
				int red = sliderRed.getValue();
				int green = sliderGreen.getValue();
				int blue = sliderBlue.getValue();

				Point p = new Point();
				p.x = (short) x;
				p.y = (short) y;
				p.red = (short) (red << 8);
				p.green = (short) (green << 8);
				p.blue = (short) (blue << 8);

				try {
					laser.sendFrame(List.of(p), 1000);
				} catch(IOException ex) {
					log.log(Levels.WARNING, "Failed to send frame: " + ex.getMessage(), ex);
				}
			}
		};

		sliderX.addChangeListener(manualChange);
		sliderY.addChangeListener(manualChange);
		sliderRed.addChangeListener(manualChange);
		sliderGreen.addChangeListener(manualChange);
		sliderBlue.addChangeListener(manualChange);

		JSplitPane diagnostics = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		diagnostics.setResizeWeight(0.4);
		JList<String> diagnosticLaserList = new JList<>(laserModel);
		diagnostics.setLeftComponent(new JScrollPane(diagnosticLaserList));
		LaserDiagnostics diagnosticControls = new LaserDiagnostics();
		diagnostics.setRightComponent(diagnosticControls);

		diagnosticLaserList.addListSelectionListener(e -> {
			int idx = diagnosticLaserList.getSelectedIndex();
			if(idx >= 0 && idx <= laserModel.getSize()) {
				Laser laser = laserModel.getLaser(idx);
				diagnosticControls.setLaser(laser);
			} else {
				diagnosticControls.setLaser(null);
			}
		});

		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("Discovery", discovery);
		tabs.addTab("Additional Lasers", additionalLasers);
		tabs.addTab("Connected", connected);
		tabs.addTab("Manual Control", manualControl);
		tabs.addTab("Diagnostics", diagnostics);

		add(BorderLayout.CENTER, tabs);

		processor.addLaserDiscoveryListener(this);

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				processor.removeLaserDiscoveryListener(LaserDiscovery.this);
			}
		});

		setSize(640, 480);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}

	private static String getName(LaserInfo laser) {
		String address = laser.getAddress().getHostAddress();
		InterfaceId id = laser.getInterfaceId();
		if(id == null) {
			return address;
		} else {
			return address + " [" + id + "]";
		}
	}

	private static String getName(Laser laser) {
		String address = laser.getAddress().getHostAddress();
		InterfaceId id = laser.getInterfaceId();
		if(id == null) {
			return address;
		} else {
			return address + " [" + id + "]";
		}
	}

	private class DiscoveryModel extends AbstractListModel<String> {
		private List<LaserInfo> lasers;

		public DiscoveryModel() {
			update();
		}

		@Override
		public String getElementAt(int index) {
			return getName(lasers.get(index));
		}

		@Override
		public int getSize() {
			return lasers.size();
		}

		public LaserInfo getLaser(int index) {
			return lasers.get(index);
		}

		public void update() {
			Set<LaserInfo> info = processor.getAvailableLasers();
			lasers = info.stream().sorted((a, b) -> getName(a).compareTo(getName(b))).toList();
			fireContentsChanged(this, 0, -1);
		}
	}

	private class AddressModel extends AbstractListModel<String> {
		private List<InetAddress> addresses;

		public AddressModel() {
			update();
		}

		@Override
		public String getElementAt(int index) {
			return addresses.get(index).getHostAddress();
		}

		@Override
		public int getSize() {
			return addresses.size();
		}

		public InetAddress getAddress(int index) {
			return addresses.get(index);
		}

		public void update() {
			Set<InetAddress> result = processor.getDiscoveryAddresses();
			addresses = result.stream().sorted((a, b) -> a.getHostAddress().compareTo(b.getHostAddress()))
					.toList();
			fireContentsChanged(this, 0, -1);
		}
	}

	private class LaserModel extends AbstractListModel<String> {
		private List<Laser> lasers;

		public LaserModel() {
			update();
		}

		@Override
		public String getElementAt(int index) {
			return getName(lasers.get(index));
		}

		@Override
		public int getSize() {
			return lasers.size();
		}

		public Laser getLaser(int index) {
			return lasers.get(index);
		}

		public void update() {
			Set<Laser> result = processor.getLasers();
			lasers = result.stream().sorted((a, b) -> getName(a).compareTo(getName(b))).toList();
			fireContentsChanged(this, 0, -1);
		}
	}

	@Override
	public void laserDiscovered(LaserInfo info) {
		SwingUtilities.invokeLater(() -> discoveryModel.update());
	}

	@Override
	public void laserLost(LaserInfo info) {
		SwingUtilities.invokeLater(() -> discoveryModel.update());
	}
}
