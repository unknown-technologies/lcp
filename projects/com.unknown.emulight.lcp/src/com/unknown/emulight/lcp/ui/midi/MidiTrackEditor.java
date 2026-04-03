package com.unknown.emulight.lcp.ui.midi;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import com.unknown.emulight.lcp.event.ConfigChangeListener;
import com.unknown.emulight.lcp.event.TrackListener;
import com.unknown.emulight.lcp.io.midi.MidiOutPort;
import com.unknown.emulight.lcp.project.EmulightSystem;
import com.unknown.emulight.lcp.project.SystemConfiguration.LaserConfig;
import com.unknown.emulight.lcp.project.SystemConfiguration.MidiPortConfig;
import com.unknown.emulight.lcp.sequencer.MidiTrack;
import com.unknown.emulight.lcp.ui.SpinnerProgramEditor;
import com.unknown.emulight.lcp.ui.SpinnerProgramModel;
import com.unknown.emulight.lcp.ui.UIUtils;
import com.unknown.emulight.lcp.ui.project.TrackEditor;
import com.unknown.util.ui.LabeledPairLayout;

@SuppressWarnings("serial")
public class MidiTrackEditor extends TrackEditor implements TrackListener, ConfigChangeListener {
	private static final String[] CHANNEL_NAMES;

	private final MidiTrack track;
	private final JTextField name;
	private final JComboBox<String> channel;
	private final JComboBox<String> port;
	private final JSlider volume;
	private final JSpinner numericVolume;
	private final JSpinner program;

	private MidiOutPort[] ports;

	private boolean bypassEvents = false;

	static {
		CHANNEL_NAMES = new String[17];
		CHANNEL_NAMES[0] = "Any";
		for(int i = 0; i < 16; i++) {
			CHANNEL_NAMES[i + 1] = Integer.toString(i + 1);
		}
	}

	public MidiTrackEditor(EmulightSystem sys, MidiTrack track) {
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

		channel = new JComboBox<>(CHANNEL_NAMES);
		channel.addItemListener(e -> {
			int idx = channel.getSelectedIndex();
			if(idx == -1) {
				return;
			}
			int ch = idx == 0 ? MidiTrack.ANY : (idx - 1);
			track.setChannel(ch);
		});
		channel.setSelectedIndex(track.getChannel() == MidiTrack.ANY ? 0 : track.getChannel() + 1);

		ports = new MidiOutPort[0];
		port = new JComboBox<>(new PortModel());
		port.addItemListener(e -> {
			int idx = port.getSelectedIndex();
			if(idx == -1) {
				return;
			}
			track.setPort(ports[idx]);
		});

		refreshMidiPorts();

		volume = new JSlider(JSlider.VERTICAL, 0, 127, (int) Math.round(track.getVolume()));
		numericVolume = new JSpinner(new SpinnerNumberModel(volume.getValue(), 0, 127, 1));

		volume.addChangeListener(e -> {
			int value = volume.getValue();
			int spinner = ((SpinnerNumberModel) numericVolume.getModel()).getNumber().intValue();
			if(spinner != value) {
				numericVolume.setValue(value);
			}
			track.setVolume(value);
		});

		numericVolume.addChangeListener(e -> {
			int value = ((SpinnerNumberModel) numericVolume.getModel()).getNumber().intValue();
			int slider = volume.getValue();
			if(slider != value) {
				volume.setValue(value);
			}
		});

		program = new JSpinner(new SpinnerProgramModel(track.getProgram()));
		program.setEditor(new SpinnerProgramEditor(program));
		program.addChangeListener(e -> {
			int prog = ((SpinnerProgramModel) program.getModel()).getProgram();
			track.setProgram(prog);
		});

		JPanel controls = new JPanel(new LabeledPairLayout());
		controls.setBorder(UIUtils.border("Track Properties"));
		controls.add(LabeledPairLayout.LABEL, new JLabel("Name:"));
		controls.add(LabeledPairLayout.COMPONENT, name);
		controls.add(LabeledPairLayout.LABEL, new JLabel("Channel:"));
		controls.add(LabeledPairLayout.COMPONENT, channel);
		controls.add(LabeledPairLayout.LABEL, new JLabel("Port:"));
		controls.add(LabeledPairLayout.COMPONENT, port);
		controls.add(LabeledPairLayout.LABEL, new JLabel("Program:"));
		controls.add(LabeledPairLayout.COMPONENT, program);
		controls.add(LabeledPairLayout.LABEL, new JLabel("Color:"));
		controls.add(LabeledPairLayout.COMPONENT, createColorBox());

		JPanel volumeControls = new JPanel();
		volumeControls.setBorder(UIUtils.border("Volume"));
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

	private void refreshMidiPorts() {
		MidiOutPort[] midiPorts = track.getProject().getSystem().getMidiRouter().getOutputPorts();
		List<MidiOutPort> activePorts = new ArrayList<>();
		for(MidiOutPort p : midiPorts) {
			if(p.isActive()) {
				activePorts.add(p);
			}
		}
		midiPorts = activePorts.toArray(new MidiOutPort[activePorts.size()]);
		Arrays.sort(midiPorts, (a, b) -> a.getDisplayName().compareTo(b.getDisplayName()));
		ports = new MidiOutPort[midiPorts.length + 1];
		ports[0] = null;
		System.arraycopy(midiPorts, 0, ports, 1, midiPorts.length);

		int selectedPort = -1;
		for(int i = 0; i < ports.length; i++) {
			if(ports[i] == track.getPort()) {
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
	public void configChanged(String key, String value) {
		// nothing
	}

	@Override
	public void laserChanged(LaserConfig laser) {
		// nothing
	}

	@Override
	public void midiPortChanged(MidiPortConfig p) {
		refreshMidiPorts();
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
		case TrackListener.CHANNEL:
			channel.setSelectedIndex(track.getChannel() == MidiTrack.ANY ? 0 : track.getChannel() + 1);
			break;
		}
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
				return ports[index].getDisplayName();
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
