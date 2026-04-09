package com.unknown.emulight.lcp.ui.live;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import com.unknown.audio.analysis.MIDINames;
import com.unknown.emulight.lcp.live.Cue;
import com.unknown.emulight.lcp.live.CuePool;
import com.unknown.emulight.lcp.live.MidiLearner;
import com.unknown.emulight.lcp.live.TriggerKey;
import com.unknown.emulight.lcp.ui.UIUtils;
import com.unknown.util.ui.LabeledPairLayout;

@SuppressWarnings("serial")
public class CueTriggerLearnDialog extends JDialog implements MidiLearner {
	private final CuePool pool;
	private final Cue<?> cue;

	private final JLabel channelInfo;
	private final JLabel keyInfo;

	public CueTriggerLearnDialog(CuePool pool, Cue<?> cue) {
		super(cue.getProject().getSystem().getMainWindow(), "Trigger Learn (Cue: " + cue.getName() + ")", true);

		this.pool = pool;
		this.cue = cue;

		channelInfo = new JLabel();
		keyInfo = new JLabel();

		TriggerKey key = pool.getTriggerKey(cue);
		showKeyInfo(key);

		JCheckBox toggleTrigger = new JCheckBox();
		toggleTrigger.setSelected(cue.isToggleTrigger());
		toggleTrigger.addChangeListener(e -> cue.setToggleTrigger(toggleTrigger.isSelected()));
		toggleTrigger.setToolTipText("When enabled, the MIDI trigger key toggles playback. When disabled, " +
				"the MIDI trigger key always starts playback.");

		JPanel info = new JPanel(new LabeledPairLayout());
		info.setBorder(UIUtils.border("Trigger Key"));
		info.add(LabeledPairLayout.LABEL, new JLabel("Channel:"));
		info.add(LabeledPairLayout.COMPONENT, channelInfo);
		info.add(LabeledPairLayout.LABEL, new JLabel("Key:"));
		info.add(LabeledPairLayout.COMPONENT, keyInfo);
		info.add(LabeledPairLayout.LABEL, new JLabel("Toggle Trigger:"));
		info.add(LabeledPairLayout.COMPONENT, toggleTrigger);

		JButton close = new JButton("Close");
		close.addActionListener(e -> close());

		JButton clear = new JButton("Clear");
		clear.addActionListener(e -> clearKey());

		JPanel buttons = new JPanel(new FlowLayout());
		buttons.add(clear);
		buttons.add(close);

		setLayout(new BorderLayout());
		add(BorderLayout.CENTER, info);
		add(BorderLayout.SOUTH, buttons);

		pool.setMidiLearn(this);

		JComponent root = getRootPane();
		KeyStroke escKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		Object quit = new Object();
		root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escKey, quit);
		root.getActionMap().put(quit, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				close();
			}
		});

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				close();
			}
		});

		pack();

		close.requestFocusInWindow();
	}

	private void close() {
		dispose();
		destroy();
	}

	private void destroy() {
		pool.clearMidiLearn();
	}

	private void showKeyInfo(TriggerKey key) {
		if(key == null) {
			channelInfo.setText("-");
			keyInfo.setText("-");
		} else {
			channelInfo.setText(Integer.toString(key.getChannel() + 1));
			keyInfo.setText(MIDINames.getNoteName(key.getKey()));
		}
	}

	private void setKey(int channel, int key) {
		TriggerKey trigger = new TriggerKey(channel, key);
		pool.clearTriggerKey(cue);
		if(pool.isTriggerKeyAssigned(trigger)) {
			channelInfo.setText("- (conflict)");
			keyInfo.setText("- (conflict)");
		} else {
			pool.setTriggerKey(cue, trigger);
			showKeyInfo(trigger);
		}
	}

	private void clearKey() {
		pool.clearTriggerKey(cue);
		showKeyInfo(null);
	}

	@Override
	public void noteOn(int channel, int key) {
		setKey(channel, key);
	}

	@Override
	public void noteOff(int channel, int key) {
		setKey(channel, key);
	}

	@Override
	public void controller(int channel, int controller) {
		// nothing
	}

	@Override
	public void program(int channel, int program) {
		// nothing
	}

	@Override
	public void bend(int channel) {
		// nothing
	}
}
