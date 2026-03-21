package com.unknown.emulight.lcp.ui.audio;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import com.unknown.emulight.lcp.audio.AudioTrack;
import com.unknown.emulight.lcp.event.TrackListener;
import com.unknown.emulight.lcp.project.EmulightSystem;
import com.unknown.emulight.lcp.ui.UIUtils;
import com.unknown.emulight.lcp.ui.project.TrackEditor;
import com.unknown.util.ui.LabeledPairLayout;

@SuppressWarnings("serial")
public class AudioTrackEditor extends TrackEditor implements TrackListener {
	private final AudioTrack track;
	private final JTextField name;
	private final JSlider volume;
	private final JSpinner numericVolume;

	public AudioTrackEditor(EmulightSystem sys, AudioTrack track) {
		super(sys, track);
		this.track = track;

		track.addTrackListener(this);

		name = new JTextField(track.getName());
		name.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				track.setName(name.getText().trim());
			}
		});

		double trackVolumeLog = track.getVolume() == 0 ? -70 : 20 * Math.log(track.getVolume()) / Math.log(10);
		if(trackVolumeLog < -70) {
			trackVolumeLog = -70;
		} else if(trackVolumeLog > 6.02) {
			trackVolumeLog = 6.02;
		}
		volume = new JSlider(JSlider.VERTICAL, -7000, 602, (int) Math.round(trackVolumeLog * 100));
		numericVolume = new JSpinner(new SpinnerNumberModel(volume.getValue(), -70.0, 6.02, 0.1));

		volume.addChangeListener(e -> {
			int value = volume.getValue();
			double db = value / 100.0;
			int spinner = ((SpinnerNumberModel) numericVolume.getModel()).getNumber().intValue();
			if(spinner != value) {
				numericVolume.setValue(db);
			}
			double linear = Math.pow(10, db / 20);
			track.setVolume(linear);
		});

		numericVolume.addChangeListener(e -> {
			double value = ((SpinnerNumberModel) numericVolume.getModel()).getNumber().doubleValue();
			int slider = volume.getValue();
			if(slider != value) {
				volume.setValue((int) Math.round(value * 100));
			}
		});

		JPanel controls = new JPanel(new LabeledPairLayout());
		controls.setBorder(UIUtils.border("Track Properties"));
		controls.add(LabeledPairLayout.LABEL, new JLabel("Name:"));
		controls.add(LabeledPairLayout.COMPONENT, name);

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

	@Override
	public void destroy() {
		// remove listeners
		track.removeTrackListener(this);
	}

	@Override
	public void propertyChanged(String key) {
		switch(key) {
		case TrackListener.NAME:
			name.setText(track.getName());
			setTitle("Track: " + track.getName());
			break;
		}
	}
}
