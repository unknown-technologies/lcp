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

import com.unknown.emulight.lcp.live.Controller;
import com.unknown.emulight.lcp.live.CuePool;
import com.unknown.emulight.lcp.live.MidiLearner;
import com.unknown.emulight.lcp.live.Target;
import com.unknown.emulight.lcp.ui.UIUtils;
import com.unknown.util.ui.LabeledPairLayout;

@SuppressWarnings("serial")
public class ControllerLearnDialog extends JDialog implements MidiLearner {
	private final CuePool pool;
	private final Target target;

	private final JLabel channelInfo;
	private final JLabel controllerInfo;

	public ControllerLearnDialog(CuePool pool, Target target, String name) {
		super(pool.getProject().getSystem().getMainWindow(), "Controller Learn (" + name + ")", true);

		this.pool = pool;
		this.target = target;

		channelInfo = new JLabel();
		controllerInfo = new JLabel();

		Controller controller = pool.getController(target);
		showControllerInfo(controller);

		JPanel info = new JPanel(new LabeledPairLayout());
		info.setBorder(UIUtils.border("Trigger Key"));
		info.add(LabeledPairLayout.LABEL, new JLabel("Channel:"));
		info.add(LabeledPairLayout.COMPONENT, channelInfo);
		info.add(LabeledPairLayout.LABEL, new JLabel("Controller:"));
		info.add(LabeledPairLayout.COMPONENT, controllerInfo);

		JButton close = new JButton("Close");
		close.addActionListener(e -> close());

		JButton clear = new JButton("Clear");
		clear.addActionListener(e -> clearController());

		JPanel buttons = new JPanel(new FlowLayout());
		buttons.add(clear);
		buttons.add(close);

		setLayout(new BorderLayout());
		add(BorderLayout.CENTER, info);
		add(BorderLayout.SOUTH, buttons);

		pool.setMidiControllerLearn(this);

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
	}

	private void showControllerInfo(Controller controller) {
		if(controller == null) {
			channelInfo.setText("-");
			controllerInfo.setText("-");
		} else {
			channelInfo.setText(Integer.toString(controller.getChannel() + 1));
			controllerInfo.setText(Integer.toString(controller.getController()));
		}
	}

	private void setController(int channel, int cc) {
		Controller controller = new Controller(channel, cc);
		pool.unmapController(target);
		if(pool.isControllerAssigned(controller)) {
			channelInfo.setText("- (conflict)");
			controllerInfo.setText("- (conflict)");
		} else {
			pool.mapController(controller, target);
			showControllerInfo(controller);
		}
	}

	private void clearController() {
		pool.unmapController(target);
		showControllerInfo(null);
	}

	@Override
	public void noteOn(int channel, int key) {
		// nothing
	}

	@Override
	public void noteOff(int channel, int key) {
		// nothing
	}

	@Override
	public void controller(int channel, int controller) {
		setController(channel, controller);
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
