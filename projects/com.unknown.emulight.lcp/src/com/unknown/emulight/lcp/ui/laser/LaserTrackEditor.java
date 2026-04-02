package com.unknown.emulight.lcp.ui.laser;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import com.unknown.emulight.lcp.event.ConfigChangeListener;
import com.unknown.emulight.lcp.event.TrackListener;
import com.unknown.emulight.lcp.laser.LaserReference;
import com.unknown.emulight.lcp.laser.LaserTrack;
import com.unknown.emulight.lcp.project.EmulightSystem;
import com.unknown.emulight.lcp.project.SystemConfiguration.LaserConfig;
import com.unknown.emulight.lcp.project.SystemConfiguration.MidiPortConfig;
import com.unknown.emulight.lcp.ui.UIUtils;
import com.unknown.emulight.lcp.ui.project.TrackEditor;
import com.unknown.util.ui.LabeledPairLayout;

@SuppressWarnings("serial")
public class LaserTrackEditor extends TrackEditor implements TrackListener, ConfigChangeListener {
	private final LaserTrack track;
	private final JTextField name;
	private final JComboBox<String> port;
	private final JSlider volume;
	private final JSpinner numericVolume;

	private LaserConfig[] ports;

	private boolean bypassEvents = false;

	public LaserTrackEditor(EmulightSystem sys, LaserTrack track) {
		super(sys, track);
		this.track = track;

		track.addTrackListener(this);
		sys.getConfig().addConfigChangeListener(this);

		name = new JTextField(track.getName());
		name.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				try {
					bypassEvents = true;
					track.setName(name.getText().trim());
				} finally {
					bypassEvents = false;
				}
			}
		});

		ports = new LaserConfig[0];
		port = new JComboBox<>(new PortModel());
		port.addItemListener(e -> {
			int idx = port.getSelectedIndex();
			if(idx == -1) {
				return;
			}
			if(ports[idx] == null) {
				track.setLaserReference(null);
			} else {
				LaserReference ref = sys.getLaser(ports[idx].getName());
				track.setLaserReference(ref);
			}
		});

		refreshLasers();

		volume = new JSlider(JSlider.VERTICAL, 0, 255, (int) Math.round(track.getVolume() * 255.0));
		numericVolume = new JSpinner(new SpinnerNumberModel(volume.getValue(), 0, 255, 1));

		volume.addChangeListener(e -> {
			int value = volume.getValue();
			int spinner = ((SpinnerNumberModel) numericVolume.getModel()).getNumber().intValue();
			if(spinner != value) {
				numericVolume.setValue(value);
			}
			track.setVolume(value / 255.0);
		});

		numericVolume.addChangeListener(e -> {
			int value = ((SpinnerNumberModel) numericVolume.getModel()).getNumber().intValue();
			int slider = volume.getValue();
			if(slider != value) {
				volume.setValue(value);
			}
		});

		JCheckBox mirrorX = new JCheckBox();
		mirrorX.setSelected(track.isMirrorX());
		mirrorX.addItemListener(e -> track.setMirrorX(mirrorX.isSelected()));

		JCheckBox mirrorY = new JCheckBox();
		mirrorY.setSelected(track.isMirrorY());
		mirrorY.addItemListener(e -> track.setMirrorY(mirrorY.isSelected()));

		JPanel controls = new JPanel(new LabeledPairLayout());
		controls.setBorder(UIUtils.border("Track Properties"));
		controls.add(LabeledPairLayout.LABEL, new JLabel("Name:"));
		controls.add(LabeledPairLayout.COMPONENT, name);
		controls.add(LabeledPairLayout.LABEL, new JLabel("Laser:"));
		controls.add(LabeledPairLayout.COMPONENT, port);
		controls.add(LabeledPairLayout.LABEL, new JLabel("Mirror X:"));
		controls.add(LabeledPairLayout.COMPONENT, mirrorX);
		controls.add(LabeledPairLayout.LABEL, new JLabel("Mirror Y:"));
		controls.add(LabeledPairLayout.COMPONENT, mirrorY);

		JPanel volumeControls = new JPanel();
		volumeControls.setBorder(UIUtils.border("Brightness"));
		volumeControls.setLayout(new BoxLayout(volumeControls, BoxLayout.Y_AXIS));
		volumeControls.add(volume);

		JPanel numericVolumePanel = new JPanel(new FlowLayout());
		numericVolumePanel.add(numericVolume);
		numericVolume.setMaximumSize(numericVolume.getPreferredSize());
		volumeControls.add(numericVolumePanel);

		JPanel buttons = new JPanel(new FlowLayout());
		JButton close = new JButton("Close");
		close.addActionListener(e -> dispose());
		buttons.add(close);

		JPanel content = new JPanel(new BorderLayout());
		content.add(BorderLayout.NORTH, controls);
		content.add(BorderLayout.CENTER, volumeControls);
		content.add(BorderLayout.SOUTH, buttons);
		setContentPane(content);

		pack();
	}

	private void refreshLasers() {
		Collection<LaserConfig> configuredLasers = track.getProject().getSystem().getConfig().getLasers();
		List<LaserConfig> activeLasers = new ArrayList<>();
		for(LaserConfig p : configuredLasers) {
			if(p.isActive()) {
				activeLasers.add(p);
			}
		}
		LaserConfig[] lasers = activeLasers.toArray(new LaserConfig[activeLasers.size()]);
		Arrays.sort(lasers, (a, b) -> a.getName().compareTo(b.getName()));
		ports = new LaserConfig[lasers.length + 1];
		ports[0] = null;
		System.arraycopy(lasers, 0, ports, 1, lasers.length);

		int selectedPort = -1;
		for(int i = 0; i < ports.length; i++) {
			LaserReference ref = track.getLaserReference();
			if(ports[i] == null) {
				if(ref == null) {
					selectedPort = i;
					break;
				}
			} else if(ports[i].getId() != null && ports[i].getId().equals(ref.getInterfaceId())) {
				selectedPort = i;
				break;
			}
		}

		port.setSelectedIndex(selectedPort);
	}

	@Override
	public void destroy() {
		// remove listeners
		track.removeTrackListener(this);
		sys.getConfig().removeConfigChangeListener(this);
	}

	@Override
	public void propertyChanged(String key) {
		switch(key) {
		case TrackListener.NAME:
			if(!bypassEvents) {
				name.setText(track.getName());
			}
			setTitle("Track: " + track.getName());
			break;
		}
	}

	@Override
	public void configChanged(String key, String value) {
		// nothing
	}

	@Override
	public void laserChanged(LaserConfig laser) {
		refreshLasers();
	}

	@Override
	public void midiPortChanged(MidiPortConfig p) {
		// nothing
	}

	private class PortModel extends AbstractListModel<String> implements ComboBoxModel<String> {
		private Object item;

		public int getSize() {
			return ports.length;
		}

		public String getElementAt(int index) {
			if(ports[index] == null) {
				return "<none>";
			} else {
				return ports[index].getName();
			}
		}

		public void setSelectedItem(Object item) {
			this.item = item;
		}

		public Object getSelectedItem() {
			return item;
		}
	}
}
