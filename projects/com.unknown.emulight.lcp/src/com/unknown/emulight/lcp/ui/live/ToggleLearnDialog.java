package com.unknown.emulight.lcp.ui.live;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import com.unknown.audio.analysis.MIDINames;
import com.unknown.emulight.lcp.live.CuePool;
import com.unknown.emulight.lcp.live.MidiLearner;
import com.unknown.emulight.lcp.live.Trigger;
import com.unknown.emulight.lcp.live.TriggerKey;
import com.unknown.emulight.lcp.ui.UIUtils;
import com.unknown.util.ui.LabeledPairLayout;

@SuppressWarnings("serial")
public class ToggleLearnDialog extends JDialog {
	private final CuePool pool;
	private final Trigger target;

	private final JLabel controllerChannelInfo;
	private final JLabel controllerKeyInfo;
	private final JLabel triggerChannelInfo;
	private final JLabel triggerKeyInfo;

	public ToggleLearnDialog(CuePool pool, Trigger target, String name) {
		super(pool.getProject().getSystem().getMainWindow(), "Toggle Learn (" + name + ")", true);

		this.pool = pool;
		this.target = target;

		controllerChannelInfo = new JLabel();
		controllerKeyInfo = new JLabel();
		triggerChannelInfo = new JLabel();
		triggerKeyInfo = new JLabel();

		TriggerKey controllerKey = pool.getControllerToggle(target);
		TriggerKey triggerKey = pool.getTriggerToggle(target);
		showControllerKeyInfo(controllerKey);
		showTriggerKeyInfo(triggerKey);

		JPanel info = new JPanel(new LabeledPairLayout());
		info.setBorder(UIUtils.border("Trigger Key"));
		info.add(LabeledPairLayout.LABEL, new JLabel("Controller Channel:"));
		info.add(LabeledPairLayout.COMPONENT, controllerChannelInfo);
		info.add(LabeledPairLayout.LABEL, new JLabel("Controller Key:"));
		info.add(LabeledPairLayout.COMPONENT, controllerKeyInfo);
		info.add(LabeledPairLayout.LABEL, new JLabel("Trigger Channel:"));
		info.add(LabeledPairLayout.COMPONENT, triggerChannelInfo);
		info.add(LabeledPairLayout.LABEL, new JLabel("Trigger Key:"));
		info.add(LabeledPairLayout.COMPONENT, triggerKeyInfo);

		JButton close = new JButton("Close");
		close.addActionListener(e -> close());

		JButton clearController = new JButton("Clear Controller");
		clearController.addActionListener(e -> clearController());

		JButton clearTrigger = new JButton("Clear Trigger");
		clearTrigger.addActionListener(e -> clearTrigger());

		JPanel buttons = new JPanel(new FlowLayout());
		buttons.add(clearController);
		buttons.add(clearTrigger);
		buttons.add(close);

		setLayout(new BorderLayout());
		add(BorderLayout.CENTER, info);
		add(BorderLayout.SOUTH, buttons);

		pool.setMidiControllerLearn(new MidiLearner() {
			@Override
			public void noteOn(int channel, int key) {
				setControllerKey(channel, key);
			}

			@Override
			public void noteOff(int channel, int key) {
				setControllerKey(channel, key);
			}

			@Override
			public void controller(int channel, int controller) {
				setControllerCC(channel, controller);
			}

			@Override
			public void program(int channel, int program) {
				// empty
			}

			@Override
			public void bend(int channel) {
				// empty
			}
		});

		pool.setMidiTriggerLearn(new MidiLearner() {
			@Override
			public void noteOn(int channel, int key) {
				setTriggerKey(channel, key);
			}

			@Override
			public void noteOff(int channel, int key) {
				setTriggerKey(channel, key);
			}

			@Override
			public void controller(int channel, int controller) {
				setTriggerCC(channel, controller);
			}

			@Override
			public void program(int channel, int program) {
				// empty
			}

			@Override
			public void bend(int channel) {
				// empty
			}
		});

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
		pool.clearMidiControllerLearn();
		pool.clearMidiTriggerLearn();
	}

	private void showControllerKeyInfo(TriggerKey controllerKey) {
		if(controllerKey == null) {
			controllerChannelInfo.setText("-");
			controllerKeyInfo.setText("-");
		} else {
			controllerChannelInfo.setText(Integer.toString(controllerKey.getChannel() + 1));
			if(controllerKey.getType() == TriggerKey.TYPE_NOTE) {
				controllerKeyInfo.setText(MIDINames.getNoteName(controllerKey.getKey()));
			} else {
				controllerKeyInfo.setText(Integer.toString(controllerKey.getKey()));
			}
		}
	}

	private void showTriggerKeyInfo(TriggerKey triggerKey) {
		if(triggerKey == null) {
			triggerChannelInfo.setText("-");
			triggerKeyInfo.setText("-");
		} else {
			triggerChannelInfo.setText(Integer.toString(triggerKey.getChannel() + 1));
			if(triggerKey.getType() == TriggerKey.TYPE_NOTE) {
				triggerKeyInfo.setText(MIDINames.getNoteName(triggerKey.getKey()));
			} else {
				triggerKeyInfo.setText(Integer.toString(triggerKey.getKey()));
			}
		}
	}

	private void setControllerKey(int channel, int key) {
		TriggerKey trigger = new TriggerKey(TriggerKey.TYPE_NOTE, channel, key);
		pool.unmapControllerToggle(target);
		if(pool.isControllerToggleAssigned(trigger)) {
			controllerChannelInfo.setText("- (conflict)");
			controllerKeyInfo.setText("- (conflict)");
		} else {
			pool.mapControllerToggle(trigger, target);
			showControllerKeyInfo(trigger);
		}
	}

	private void setControllerCC(int channel, int cc) {
		TriggerKey trigger = new TriggerKey(TriggerKey.TYPE_CONTROLLER, channel, cc);
		pool.unmapControllerToggle(target);
		if(pool.isControllerToggleAssigned(trigger)) {
			controllerChannelInfo.setText("- (conflict)");
			controllerKeyInfo.setText("- (conflict)");
		} else {
			pool.mapControllerToggle(trigger, target);
			showControllerKeyInfo(trigger);
		}
	}

	private void setTriggerKey(int channel, int key) {
		TriggerKey trigger = new TriggerKey(TriggerKey.TYPE_NOTE, channel, key);
		pool.unmapTriggerToggle(target);
		if(pool.isTriggerToggleAssigned(trigger)) {
			triggerChannelInfo.setText("- (conflict)");
			triggerKeyInfo.setText("- (conflict)");
		} else {
			pool.mapTriggerToggle(trigger, target);
			showTriggerKeyInfo(trigger);
		}
	}

	private void setTriggerCC(int channel, int cc) {
		TriggerKey trigger = new TriggerKey(TriggerKey.TYPE_CONTROLLER, channel, cc);
		pool.unmapTriggerToggle(target);
		if(pool.isTriggerToggleAssigned(trigger)) {
			triggerChannelInfo.setText("- (conflict)");
			triggerKeyInfo.setText("- (conflict)");
		} else {
			pool.mapTriggerToggle(trigger, target);
			showTriggerKeyInfo(trigger);
		}
	}

	private void clearController() {
		pool.unmapControllerToggle(target);
		showControllerKeyInfo(null);
	}

	private void clearTrigger() {
		pool.unmapTriggerToggle(target);
		showTriggerKeyInfo(null);
	}
}
