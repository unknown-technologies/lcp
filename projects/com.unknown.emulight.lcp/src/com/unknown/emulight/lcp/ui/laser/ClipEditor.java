package com.unknown.emulight.lcp.ui.laser;

import java.awt.BorderLayout;
import java.util.function.Consumer;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;

import com.unknown.emulight.lcp.laser.LaserPart;
import com.unknown.emulight.lcp.project.PartContainer;
import com.unknown.emulight.lcp.project.Project;
import com.unknown.util.ui.LabeledPairLayout;

@SuppressWarnings("serial")
public class ClipEditor extends JPanel {
	private Project project;

	private PartContainer<LaserPart> clip;

	private ClipTreeEditor treeEditor;
	private ClipPropertyEditor propertyEditor;
	private ClipNodeEditor nodeEditor;

	private ClipPropertyAutomationEditor automationEditor;

	private JSpinner frameSpeed;
	private JSpinner clipDuration;
	private JCheckBox loop;

	private long globalTime;

	private final Consumer<Integer> positionCallback;

	public ClipEditor(JFrame parent, PartContainer<LaserPart> container, Consumer<Integer> positionCallback) {
		super(new BorderLayout());

		this.project = container.getTrack().getProject();
		this.positionCallback = positionCallback;

		automationEditor = new ClipPropertyAutomationEditor(parent, project, this::update);
		automationEditor.setStartTime(container.getTime());

		clip = container;

		propertyEditor = new ClipPropertyEditor(project, this::updateProperty, automationEditor);

		treeEditor = new ClipTreeEditor(this::update);
		treeEditor.setSelectionListener(n -> {
			nodeEditor.setNode(n);
			propertyEditor.setNode(n);
		});

		nodeEditor = new ClipNodeEditor(this::update, treeEditor);

		treeEditor.setClip(clip.getPart());

		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		split.setResizeWeight(0.2);
		split.setLeftComponent(treeEditor);
		split.setRightComponent(nodeEditor);

		boolean isLoop = clip.getPart().isLoop();
		int length = (int) (isLoop ? clip.getPart().getLength() : clip.getLength());
		frameSpeed = new JSpinner(new SpinnerNumberModel(clip.getPart().getSpeed(), 440, 2000000, 1));
		clipDuration = new JSpinner(
				new SpinnerNumberModel(length, 1, Integer.MAX_VALUE, 1));
		loop = new JCheckBox();
		loop.setSelected(isLoop);

		JPanel settings = new JPanel(new LabeledPairLayout());
		settings.add(LabeledPairLayout.LABEL, new JLabel("Speed:"));
		settings.add(LabeledPairLayout.COMPONENT, frameSpeed);
		settings.add(LabeledPairLayout.LABEL, new JLabel("Clip duration:"));
		settings.add(LabeledPairLayout.COMPONENT, clipDuration);
		settings.add(LabeledPairLayout.LABEL, new JLabel("Loop:"));
		settings.add(LabeledPairLayout.COMPONENT, loop);

		frameSpeed.addChangeListener(e -> {
			int speed = (int) frameSpeed.getValue();
			clip.getPart().setSpeed(speed);
		});

		clipDuration.addChangeListener(e -> {
			int duration = (int) clipDuration.getValue();
			clip.getPart().setLength(duration);
			updateTime();
		});

		loop.addChangeListener(e -> {
			clip.getPart().setLoop(loop.isSelected());
			updateTime();
		});

		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("Properties", propertyEditor);
		tabs.addTab("Settings", settings);
		add(BorderLayout.CENTER, split);
		add(BorderLayout.EAST, tabs);
	}

	public void setClip(PartContainer<LaserPart> clip) {
		this.clip = clip;
		LaserPart part = clip.getPart();
		nodeEditor.setNode(part.getRoot());
		propertyEditor.setNode(part.getRoot());
		treeEditor.setClip(part);
		frameSpeed.setValue(part.getSpeed());
		clipDuration.setValue((int) part.getLength());
		loop.setSelected(part.isLoop());
		nodeEditor.setTime(0);
		propertyEditor.setTime(0);
		updateTime();
	}

	private void update() {
		nodeEditor.repaint();
		propertyEditor.update();
	}

	public void setTime(int time) {
		nodeEditor.setTime(time);
		propertyEditor.setTime(time);
		positionCallback.accept(time);
	}

	public void setPosition(long time) {
		globalTime = time;
		long localTime = clip.getLocalTime(time);
		long clipLength = clip.getLength();
		long partLength = clip.getPart().getLength();
		if(localTime < 0) {
			localTime = 0;
		}
		if(clip.getPart().isLoop()) {
			localTime %= partLength;
		} else if(localTime > clipLength) {
			localTime = clipLength;
		}
		setTime((int) localTime);
	}

	void updateTime() {
		setPosition(globalTime);
	}

	private void updateProperty() {
		treeEditor.nodeChanged(propertyEditor.getNode());
	}
}
