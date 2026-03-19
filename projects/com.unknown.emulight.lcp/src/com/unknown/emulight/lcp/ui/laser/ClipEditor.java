package com.unknown.emulight.lcp.ui.laser;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javax.swing.AbstractListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;

import com.unknown.emulight.lcp.laser.LaserPart;
import com.unknown.emulight.lcp.project.PartContainer;
import com.unknown.emulight.lcp.project.Project;
import com.unknown.math.g3d.Mtx44;
import com.unknown.math.g3d.Vec3;
import com.unknown.net.shownet.InterfaceId;
import com.unknown.net.shownet.Laser;
import com.unknown.net.shownet.LaserDiscoveryListener;
import com.unknown.net.shownet.LaserInfo;
import com.unknown.net.shownet.Point;
import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;
import com.unknown.util.ui.LabeledPairLayout;

@SuppressWarnings("serial")
public class ClipEditor extends JPanel {
	private static final Logger log = Trace.create(ClipEditor.class);

	private Project project;

	private PartContainer<LaserPart> clip;
	private LaserListModel lasers;

	private ClipTreeEditor treeEditor;
	private ClipPropertyEditor propertyEditor;
	private ClipNodeEditor nodeEditor;

	private ClipPropertyAutomationEditor automationEditor;

	private Laser previewLaser;

	private Vec3 colorScale = new Vec3(0, 0, 0);
	private double rotation = 0;
	private Vec3 scale = new Vec3(1, 1, 1);

	private JSpinner frameSpeed;
	private JSpinner clipDuration;
	private JCheckBox loop;
	private JSlider timeSlider;

	private int time;
	private long globalTime;

	private final Consumer<Integer> positionCallback;

	public ClipEditor(JFrame parent, PartContainer<LaserPart> container, Consumer<Integer> positionCallback) {
		super(new BorderLayout());

		this.project = container.getTrack().getProject();
		this.positionCallback = positionCallback;

		automationEditor = new ClipPropertyAutomationEditor(parent, project, this::update);
		automationEditor.setStartTime(container.getTime());

		clip = container;

		lasers = new LaserListModel();
		JList<String> laserList = new JList<>(lasers);

		propertyEditor = new ClipPropertyEditor(this::updateProperty, automationEditor);

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

		JSlider sliderRed = new JSlider(0, 0xFF, 0);
		JSlider sliderGreen = new JSlider(0, 0xFF, 0);
		JSlider sliderBlue = new JSlider(0, 0xFF, 0);
		JSlider sliderRotate = new JSlider(0, 65536, 0);
		JSlider sliderScaleX = new JSlider(0, 65536, 65536);
		JSlider sliderScaleY = new JSlider(0, 65536, 65536);

		boolean isLoop = clip.getPart().isLoop();
		int length = (int) (isLoop ? clip.getPart().getLength() : clip.getLength());
		frameSpeed = new JSpinner(new SpinnerNumberModel(clip.getPart().getSpeed(), 440, 2000000, 1));
		clipDuration = new JSpinner(
				new SpinnerNumberModel(length, 1, Integer.MAX_VALUE, 1));
		loop = new JCheckBox();
		loop.setSelected(isLoop);

		JPanel settings = new JPanel(new LabeledPairLayout());
		settings.add(LabeledPairLayout.LABEL, new JLabel("Red:"));
		settings.add(LabeledPairLayout.COMPONENT, sliderRed);
		settings.add(LabeledPairLayout.LABEL, new JLabel("Green:"));
		settings.add(LabeledPairLayout.COMPONENT, sliderGreen);
		settings.add(LabeledPairLayout.LABEL, new JLabel("Blue:"));
		settings.add(LabeledPairLayout.COMPONENT, sliderBlue);
		settings.add(LabeledPairLayout.LABEL, new JLabel("Rotate:"));
		settings.add(LabeledPairLayout.COMPONENT, sliderRotate);
		settings.add(LabeledPairLayout.LABEL, new JLabel("Scale X:"));
		settings.add(LabeledPairLayout.COMPONENT, sliderScaleX);
		settings.add(LabeledPairLayout.LABEL, new JLabel("Scale Y:"));
		settings.add(LabeledPairLayout.COMPONENT, sliderScaleY);
		settings.add(LabeledPairLayout.LABEL, new JLabel("Speed:"));
		settings.add(LabeledPairLayout.COMPONENT, frameSpeed);
		settings.add(LabeledPairLayout.LABEL, new JLabel("Clip duration:"));
		settings.add(LabeledPairLayout.COMPONENT, clipDuration);
		settings.add(LabeledPairLayout.LABEL, new JLabel("Loop:"));
		settings.add(LabeledPairLayout.COMPONENT, loop);

		ChangeListener colorSliderListener = e -> {
			int red = sliderRed.getValue();
			int green = sliderGreen.getValue();
			int blue = sliderBlue.getValue();

			colorScale = new Vec3(red / 255.0, green / 255.0, blue / 255.0);
			send();
		};

		ChangeListener scaleSliderListener = e -> {
			double scaleX = sliderScaleX.getValue() / 65536.0;
			double scaleY = sliderScaleY.getValue() / 65536.0;
			scale = new Vec3(scaleX, scaleY, 1.0);
			send();
		};

		sliderRed.addChangeListener(colorSliderListener);
		sliderGreen.addChangeListener(colorSliderListener);
		sliderBlue.addChangeListener(colorSliderListener);
		sliderRotate.addChangeListener(e -> {
			rotation = sliderRotate.getValue() / 65536.0;
			send();
		});
		sliderScaleX.addChangeListener(scaleSliderListener);
		sliderScaleY.addChangeListener(scaleSliderListener);

		timeSlider = new JSlider(0, length, 0);

		frameSpeed.addChangeListener(e -> {
			int speed = (int) frameSpeed.getValue();
			clip.getPart().setSpeed(speed);
			send();
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

		timeSlider.addChangeListener(e -> {
			setTime(timeSlider.getValue());
		});

		JButton play = new JButton("Play");
		play.addActionListener(e -> playClip());
		JButton stop = new JButton("Stop");
		stop.addActionListener(e -> stopClip());
		JPanel buttons = new JPanel(new FlowLayout());
		buttons.add(play);
		buttons.add(stop);

		JPanel laserControl = new JPanel(new BorderLayout());
		laserControl.add(BorderLayout.CENTER, new JScrollPane(laserList));
		laserControl.add(BorderLayout.SOUTH, buttons);

		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("Properties", propertyEditor);
		tabs.addTab("Lasers", laserControl);
		tabs.addTab("Settings", settings);
		add(BorderLayout.CENTER, split);
		add(BorderLayout.EAST, tabs);
		add(BorderLayout.SOUTH, timeSlider);

		project.getProcessor().addLaserDiscoveryListener(new LaserDiscoveryListener() {
			@Override
			public void laserDiscovered(LaserInfo info) {
				SwingUtilities.invokeLater(() -> lasers.update());
			}

			@Override
			public void laserLost(LaserInfo info) {
				SwingUtilities.invokeLater(() -> lasers.update());
			}
		});

		laserList.addListSelectionListener(e -> {
			if(previewLaser != null) {
				stop();
				previewLaser = null;
			}

			int idx = laserList.getSelectedIndex();
			if(idx != -1) {
				previewLaser = lasers.getLaser(idx);
				send();
			}
		});
	}

	private class LaserListModel extends AbstractListModel<String> {
		private List<Laser> lasers;

		public LaserListModel() {
			lasers = getLasers();
		}

		private static String getName(Laser laser) {
			InterfaceId id = laser.getInterfaceId();
			if(id != null) {
				return laser.getAddress().getHostAddress() + " [" + laser.getInterfaceId() + "]";
			} else {
				return laser.getAddress().getHostAddress();
			}
		}

		private List<Laser> getLasers() {
			return project.getProcessor().getLasers().stream()
					.sorted((a, b) -> getName(a).compareTo(getName(b))).toList();
		}

		public Laser getLaser(int index) {
			return lasers.get(index);
		}

		@Override
		public String getElementAt(int index) {
			return getName(lasers.get(index));
		}

		@Override
		public int getSize() {
			return lasers.size();
		}

		public void update() {
			lasers = getLasers();
			fireContentsChanged(this, 0, -1);
		}
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
		timeSlider.setValue(0);
		timeSlider.setMaximum((int) part.getLength());
		nodeEditor.setTime(0);
		propertyEditor.setTime(0);
		updateTime();
		send();
	}

	private void update() {
		nodeEditor.repaint();
		propertyEditor.update();
		send();
	}

	public void setTime(int time) {
		this.time = time;
		nodeEditor.setTime(time);
		propertyEditor.setTime(time);
		positionCallback.accept(time);
		send();
	}

	public void setPosition(long time) {
		globalTime = time;
		long localTime = clip.getLocalTime(time);
		long clipLength = clip.getLength();
		long partLength = clip.getPart().getLength();
		if(clip.getPart().isLoop()) {
			localTime %= partLength;
		} else if(localTime > clipLength) {
			localTime = clipLength;
		}
		timeSlider.setValue((int) localTime);
		setTime((int) localTime);
	}

	void updateTime() {
		boolean isloop = clip.getPart().isLoop();
		int value = timeSlider.getValue();
		int duration = isloop ? (int) clip.getPart().getLength() : (int) clip.getLength();
		if(value > duration) {
			timeSlider.setValue(0);
		}
		timeSlider.setMaximum(duration);
		if(value > duration) {
			timeSlider.setValue(duration);
		}
		setPosition(globalTime);
		send();
	}

	private void updateProperty() {
		treeEditor.nodeChanged(propertyEditor.getNode());
	}

	private void send() {
		if(previewLaser != null) {
			if(project.getProcessor().hasCurrentClip(previewLaser)) {
				return;
			}
			Mtx44 proj = Mtx44.rotDegZ(rotation * 360.0).concat(Mtx44.scale(scale.x, scale.y, scale.z));
			Mtx44 color = Mtx44.scale(colorScale.x, colorScale.y, colorScale.z);
			List<Point> points = clip.getPart().render(time, proj, color);
			try {
				previewLaser.sendFrame(points, clip.getPart().getSpeed());
			} catch(IOException e) {
				log.log(Levels.WARNING, "Failed to send frame to laser: " + e.getMessage(), e);
			}
		}
	}

	private void stop() {
		if(previewLaser != null) {
			if(project.getProcessor().hasCurrentClip(previewLaser)) {
				return;
			}
			List<Point> points = List.of(new Point());
			try {
				previewLaser.sendFrame(points, 1000);
			} catch(IOException e) {
				log.log(Levels.WARNING, "Failed to send frame to laser: " + e.getMessage(), e);
			}
		}
	}

	private void playClip() {
		if(previewLaser != null) {
			project.getProcessor().setCurrentClip(previewLaser, clip.getPart());
		}
	}

	private void stopClip() {
		if(previewLaser != null) {
			project.getProcessor().clearCurrentClip(previewLaser);
			send();
		}
	}
}
