package com.unknown.emulight.lcp.ui.dmx;

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

import com.unknown.emulight.lcp.dmx.DMXTrack;
import com.unknown.emulight.lcp.event.TrackListener;
import com.unknown.emulight.lcp.project.EmulightSystem;
import com.unknown.emulight.lcp.ui.UIUtils;
import com.unknown.emulight.lcp.ui.project.TrackEditor;
import com.unknown.util.ui.LabeledPairLayout;

@SuppressWarnings("serial")
public class DMXTrackEditor extends TrackEditor implements TrackListener {
	private final DMXTrack track;
	private final JTextField name;
	private final JSlider volume;
	private final JSpinner numericVolume;

	private boolean bypassEvents = false;

	public DMXTrackEditor(EmulightSystem sys, DMXTrack track) {
		super(sys, track);
		this.track = track;

		track.addTrackListener(this);

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

		JPanel controls = new JPanel(new LabeledPairLayout());
		controls.setBorder(UIUtils.border("Track Properties"));
		controls.add(LabeledPairLayout.LABEL, new JLabel("Name:"));
		controls.add(LabeledPairLayout.COMPONENT, name);
		controls.add(LabeledPairLayout.LABEL, new JLabel("Color:"));
		controls.add(LabeledPairLayout.COMPONENT, getTrackColorBox());

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

	@Override
	public void destroy() {
		// remove listeners
		track.removeTrackListener(this);
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
		case TrackListener.COLOR:
			getTrackColorBox().repaint();
			break;
		}
	}
}
