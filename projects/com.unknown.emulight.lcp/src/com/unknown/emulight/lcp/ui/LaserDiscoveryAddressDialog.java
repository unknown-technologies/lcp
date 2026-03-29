package com.unknown.emulight.lcp.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

import com.unknown.emulight.lcp.laser.LaserProcessor;
import com.unknown.emulight.lcp.project.EmulightSystem;
import com.unknown.emulight.lcp.project.SystemConfiguration.LaserAddress;

@SuppressWarnings("serial")
public class LaserDiscoveryAddressDialog extends JDialog {
	private final AddressModel addressModel;
	private final EmulightSystem sys;

	public LaserDiscoveryAddressDialog(JDialog parent, EmulightSystem sys) {
		super(parent, "Laser Discovery", ModalityType.APPLICATION_MODAL);

		this.sys = sys;

		LaserProcessor processor = sys.getLaserProcessor();

		setLayout(new BorderLayout());

		addressModel = new AddressModel();
		JList<String> discoveryList = new JList<>(addressModel);
		JScrollPane scroller = new JScrollPane(discoveryList);
		scroller.setBorder(UIUtils.border("Laser Discovery Addresses"));
		add(BorderLayout.CENTER, scroller);

		JPanel buttons = new JPanel(new FlowLayout());
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
						sys.getConfig().addLaserAddress(hostname, addr);
						addressModel.update();
					} catch(UnknownHostException e1) {
						JOptionPane.showMessageDialog(LaserDiscoveryAddressDialog.this,
								"Host not found", "Error",
								JOptionPane.OK_OPTION | JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		});

		JButton removeAddress = new JButton("-");

		JButton close = new JButton("Close");
		close.addActionListener(e -> dispose());

		buttons.add(addAddress);
		buttons.add(removeAddress);
		buttons.add(close);

		add(BorderLayout.SOUTH, buttons);

		removeAddress.setEnabled(false);
		removeAddress.addActionListener(e -> {
			int index = discoveryList.getSelectedIndex();
			if(index >= 0 && index < addressModel.getSize()) {
				String hostname = addressModel.getElementAt(index);
				InetAddress addr = addressModel.getAddress(index);
				sys.getConfig().removeLaserAddress(hostname);
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

		KeyStroke escKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		Object quit = new Object();
		JRootPane root = getRootPane();
		root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escKey, quit);
		root.getActionMap().put(quit, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});

		setSize(320, 240);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}

	private class AddressModel extends AbstractListModel<String> {
		private List<LaserAddress> addresses;

		public AddressModel() {
			update();
		}

		@Override
		public String getElementAt(int index) {
			return addresses.get(index).getHostname();
		}

		@Override
		public int getSize() {
			return addresses.size();
		}

		public InetAddress getAddress(int index) {
			return addresses.get(index).getAddress();
		}

		public void update() {
			Set<LaserAddress> result = sys.getConfig().getLaserAddresses();
			addresses = result.stream().sorted((a, b) -> a.getHostname().compareTo(b.getHostname()))
					.toList();
			fireContentsChanged(this, 0, -1);
		}
	}
}
