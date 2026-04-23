package com.unknown.emulight.lcp.ui.live;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractListModel;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;

import com.unknown.emulight.lcp.event.ConfigChangeListener;
import com.unknown.emulight.lcp.event.ProjectListener;
import com.unknown.emulight.lcp.io.midi.MidiInPort;
import com.unknown.emulight.lcp.laser.LaserCue;
import com.unknown.emulight.lcp.laser.LaserPart;
import com.unknown.emulight.lcp.laser.LaserReference;
import com.unknown.emulight.lcp.laser.LaserTrack;
import com.unknown.emulight.lcp.live.Cue;
import com.unknown.emulight.lcp.live.CuePool;
import com.unknown.emulight.lcp.live.Target;
import com.unknown.emulight.lcp.live.Trigger;
import com.unknown.emulight.lcp.project.AbstractPart;
import com.unknown.emulight.lcp.project.PartContainer;
import com.unknown.emulight.lcp.project.Project;
import com.unknown.emulight.lcp.project.SystemConfiguration.DMXPortConfig;
import com.unknown.emulight.lcp.project.SystemConfiguration.LaserConfig;
import com.unknown.emulight.lcp.project.SystemConfiguration.MidiPortConfig;
import com.unknown.emulight.lcp.project.Track;
import com.unknown.emulight.lcp.ui.UIUtils;

@SuppressWarnings("serial")
public class LiveEditor extends JPanel implements ConfigChangeListener {
	private final static int SPINNER_WIDTH = 75;

	private final Project project;
	private final CueList cueList;

	private MidiInPort[] ports;

	private JSpinner bpm;
	private JComboBox<String> controllerPort;
	private JComboBox<String> triggerPort;

	private List<Callback> updaters = new ArrayList<>();

	public LiveEditor(Project project) {
		this.project = project;

		CuePool cues = project.getCuePool();

		setLayout(new BorderLayout());

		bpm = new JSpinner(new SpinnerNumberModel(120.0, 1.0, 600.0, 1.0));
		Dimension bpmSize = bpm.getMinimumSize();
		Dimension bpmMinsz = new Dimension(SPINNER_WIDTH, bpmSize.height);
		bpm.setMinimumSize(bpmMinsz);
		bpm.setPreferredSize(bpmMinsz);

		cueList = new CueList(project.getCuePool());
		add(BorderLayout.CENTER, cueList);

		bpm.addChangeListener(e -> {
			double d = (double) bpm.getValue();
			cueList.setBPM(d);
			project.getSystem().getLaserProcessor().setBPM(d);
		});

		ports = new MidiInPort[0];

		controllerPort = new JComboBox<>(new PortModel());
		controllerPort.addItemListener(e -> {
			int idx = controllerPort.getSelectedIndex();
			if(idx == -1) {
				return;
			}
			cues.setControllerPort(ports[idx]);
		});

		triggerPort = new JComboBox<>(new PortModel());
		triggerPort.addItemListener(e -> {
			int idx = triggerPort.getSelectedIndex();
			if(idx == -1) {
				return;
			}
			cues.setTriggerPort(ports[idx]);
		});

		refreshMidiPorts();

		JButton transferButton = new JButton("Transfer from Tracks");
		transferButton.addActionListener(e -> transferFromTracks());

		JButton deleteAll = new JButton("Delete All");
		deleteAll.addActionListener(e -> deleteAll());

		JButton stopAll = new JButton("Stop All");
		Trigger stopTrigger = cues.getTrigger(CuePool.TRIGGER_ALL_STOP);
		stopAll.addActionListener(e -> stopTrigger.trigger());
		stopAll.addMouseListener(createToggle("Stop all cues", CuePool.TRIGGER_ALL_STOP));

		JToggleButton strobo = new JToggleButton("Strobo");
		strobo.setSelected(false);
		Trigger stroboTrigger = cues.getTrigger(CuePool.TRIGGER_STROBO);
		stroboTrigger.addTriggerListener(e -> {
			strobo.setSelected(stroboTrigger.getState());
		});
		strobo.addActionListener(e -> {
			stroboTrigger.setState(strobo.isSelected());
		});
		strobo.addMouseListener(createToggle("Strobo enable", CuePool.TRIGGER_STROBO));

		JPanel options = new JPanel(new FlowLayout(FlowLayout.CENTER));
		options.add(transferButton);
		options.add(deleteAll);
		options.add(new JLabel("Controller Port:"));
		options.add(controllerPort);
		options.add(new JLabel("Trigger Port:"));
		options.add(triggerPort);
		options.add(new JLabel("BPM:"));
		options.add(bpm);
		options.add(strobo);
		options.add(stopAll);
		add(BorderLayout.NORTH, options);

		// faders
		JPanel controls = new JPanel(new FlowLayout());
		controls.add(createFader("Bright", "Brightness", CuePool.TARGET_BRIGHTNESS));
		controls.add(createFader("Red", CuePool.TARGET_RED));
		controls.add(createFader("Green", CuePool.TARGET_GREEN));
		controls.add(createFader("Blue", CuePool.TARGET_BLUE));
		controls.add(createFader("Size", CuePool.TARGET_SIZE));
		controls.add(createFader("Rot", "Rotation", CuePool.TARGET_ROTATION));
		controls.add(createFader("X", "Translation X", CuePool.TARGET_TRANSLATION_X));
		controls.add(createFader("Y", "Translation Y", CuePool.TARGET_TRANSLATION_Y));
		controls.add(createFader("Strobo", "Strobo Speed", CuePool.TARGET_STROBO_SPEED));

		add(BorderLayout.SOUTH, controls);

		project.getSystem().getConfig().addConfigChangeListener(this);

		ProjectListener projectListener = new ProjectListener() {
			@Override
			public void propertyChanged(String key) {
				// nothing
			}

			@Override
			public void trackAdded(Track<?> track) {
				// nothing
			}

			@Override
			public void trackRemoved(Track<?> track) {
				// nothing
			}

			@Override
			public void projectLoaded() {
				reload();
			}
		};
		project.addProjectListener(projectListener);
	}

	@FunctionalInterface
	private interface Callback {
		void callback();
	}

	private JPanel createFader(String name, String targetName) {
		return createFader(name, name, targetName);
	}

	private MouseListener createToggle(String name, String targetName) {
		CuePool pool = project.getCuePool();
		Trigger target = pool.getTrigger(targetName);

		return new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(e.getButton() == MouseEvent.BUTTON3) {
					JMenuItem mapToggle = new JMenuItem("Map toggle...");
					mapToggle.setMnemonic('M');
					mapToggle.addActionListener(ev -> {
						ToggleLearnDialog dlg = new ToggleLearnDialog(pool, target, name);
						dlg.setLocationRelativeTo(LiveEditor.this);
						dlg.setVisible(true);
						repaint();
					});

					JMenuItem unmapController = new JMenuItem("Unmap controller toggle...");
					unmapController.setMnemonic('c');
					unmapController.addActionListener(ev -> {
						pool.unmapControllerToggle(target);
					});
					unmapController.setEnabled(pool.getControllerToggle(target) != null);

					JMenuItem unmapTrigger = new JMenuItem("Unmap trigger toggle...");
					unmapTrigger.setMnemonic('t');
					unmapTrigger.addActionListener(ev -> {
						pool.unmapTriggerToggle(target);
					});
					unmapTrigger.setEnabled(pool.getTriggerToggle(target) != null);

					JPopupMenu menu = new JPopupMenu();
					menu.add(mapToggle);
					menu.addSeparator();
					menu.add(unmapController);
					menu.add(unmapTrigger);

					menu.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		};
	}

	private JPanel createFader(String shortName, String fullName, String targetName) {
		CuePool pool = project.getCuePool();
		Target target = pool.getTarget(targetName);

		MouseListener mouse = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(e.getButton() == MouseEvent.BUTTON3) {
					JMenuItem mapController = new JMenuItem("Map controller...");
					mapController.setMnemonic('M');
					mapController.addActionListener(ev -> {
						ControllerLearnDialog dlg = new ControllerLearnDialog(pool, target,
								fullName);
						dlg.setLocationRelativeTo(LiveEditor.this);
						dlg.setVisible(true);
						repaint();
					});

					JMenuItem unmapController = new JMenuItem("Unmap controller...");
					unmapController.setMnemonic('U');
					unmapController.addActionListener(ev -> {
						pool.unmapController(target);
					});
					unmapController.setEnabled(pool.getController(target) != null);

					JPopupMenu menu = new JPopupMenu();
					menu.add(mapController);
					menu.add(unmapController);

					menu.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		};

		JSlider fader = new JSlider(JSlider.VERTICAL, 0, 127,
				(int) Math.round(target.getValue() * 127.0));
		JSpinner numericFader = new JSpinner(new SpinnerNumberModel(fader.getValue(), 0, 127, 1));

		fader.addChangeListener(e -> {
			int value = fader.getValue();
			int spinner = ((SpinnerNumberModel) numericFader.getModel()).getNumber().intValue();
			if(spinner != value) {
				numericFader.setValue(value);
			}
			target.setValue(value / 127.0);
		});

		fader.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
					target.setValue(target.getDefault());
					e.consume();
				}
			}
		});
		fader.addMouseListener(mouse);

		numericFader.addChangeListener(e -> {
			int value = ((SpinnerNumberModel) numericFader.getModel()).getNumber().intValue();
			int slider = fader.getValue();
			if(slider != value) {
				fader.setValue(value);
			}
		});

		target.addTargetListener(e -> {
			int value = (int) Math.round(target.getValue() * 127.0);
			numericFader.setValue(value);
		});

		JPanel faderControls = new JPanel();
		faderControls.setBorder(UIUtils.border(shortName));
		faderControls.setToolTipText(fullName);
		faderControls.setLayout(new BoxLayout(faderControls, BoxLayout.Y_AXIS));
		faderControls.add(fader);

		JPanel numericFaderPanel = new JPanel(new FlowLayout());
		numericFaderPanel.add(numericFader);
		numericFader.setMaximumSize(numericFader.getPreferredSize());
		faderControls.add(numericFaderPanel);

		faderControls.addMouseListener(mouse);

		updaters.add(() -> {
			int value = (int) Math.round(target.getValue() * 127.0);
			numericFader.setValue(value);
		});

		return faderControls;
	}

	public void transferFromTracks() {
		Set<AbstractPart> uniqueParts = new HashSet<>();
		Map<AbstractPart, List<PartContainer<?>>> map = new HashMap<>();
		for(Track<?> track : project.getTracks()) {
			for(PartContainer<?> container : track.getParts()) {
				uniqueParts.add(container.getPart());
				List<PartContainer<?>> containers = map.get(container.getPart());
				if(containers == null) {
					containers = new ArrayList<>();
					map.put(container.getPart(), containers);
				}
				containers.add(container);
			}
		}

		CuePool pool = project.getCuePool();
		Set<AbstractPart> existingParts = new HashSet<>();
		for(Cue<?> cue : pool.getCues()) {
			existingParts.add(cue.getPart());
		}

		// TODO: update this later, once more part types become available
		uniqueParts.stream().filter(part -> !existingParts.contains(part)).forEach(part -> {
			if(part instanceof LaserPart) {
				LaserPart laserPart = (LaserPart) part;
				List<PartContainer<?>> containers = map.get(laserPart);
				int color = containers.get(0).getTrack().getColor();

				LaserCue cue = new LaserCue(project, laserPart);
				cue.setColor(color);
				long maxlen = 0;
				for(PartContainer<?> container : containers) {
					long len = container.getLength();
					if(len > maxlen) {
						maxlen = len;
					}

					LaserReference laser = ((LaserTrack) container.getTrack()).getLaserReference();
					if(laser != null) {
						cue.addLaser(laser);
					}
				}
				cue.setLength((int) maxlen);

				pool.addCue(cue);
			}
		});

		cueList.repaint();
	}

	public void deleteAll() {
		project.getCuePool().clear();
		cueList.repaint();
	}

	private void refreshMidiPorts() {
		CuePool cues = project.getCuePool();

		MidiInPort[] midiPorts = project.getSystem().getMidiRouter().getInputPorts();
		List<MidiInPort> activePorts = new ArrayList<>();
		for(MidiInPort p : midiPorts) {
			if(p.isActive()) {
				activePorts.add(p);
			}
		}
		midiPorts = activePorts.toArray(new MidiInPort[activePorts.size()]);
		Arrays.sort(midiPorts, (a, b) -> a.getDisplayName().compareTo(b.getDisplayName()));
		ports = new MidiInPort[midiPorts.length + 2];
		ports[0] = null;
		ports[1] = cues.getAllBusPort();
		System.arraycopy(midiPorts, 0, ports, 2, midiPorts.length);

		// controller port
		int selectedPort = -1;
		for(int i = 0; i < ports.length; i++) {
			if(ports[i] == cues.getControllerPort()) {
				selectedPort = i;
				break;
			}
		}

		controllerPort.setSelectedIndex(selectedPort);

		// trigger port
		selectedPort = -1;
		for(int i = 0; i < ports.length; i++) {
			if(ports[i] == cues.getTriggerPort()) {
				selectedPort = i;
				break;
			}
		}

		triggerPort.setSelectedIndex(selectedPort);
	}

	private void reload() {
		CuePool cues = project.getCuePool();

		// controller port
		int selectedPort = -1;
		for(int i = 0; i < ports.length; i++) {
			if(ports[i] == cues.getControllerPort()) {
				selectedPort = i;
				break;
			}
		}

		controllerPort.setSelectedIndex(selectedPort);

		// trigger port
		selectedPort = -1;
		for(int i = 0; i < ports.length; i++) {
			if(ports[i] == cues.getTriggerPort()) {
				selectedPort = i;
				break;
			}
		}

		triggerPort.setSelectedIndex(selectedPort);

		bpm.setValue(cues.getBPM());

		for(Callback updater : updaters) {
			updater.callback();
		}

		cueList.repaint();
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
	public void dmxPortChanged(DMXPortConfig p) {
		// nothing
	}

	private class PortModel extends AbstractListModel<String> implements ComboBoxModel<String> {
		private Object item;

		@Override
		public int getSize() {
			return ports.length;
		}

		@Override
		public String getElementAt(int index) {
			if(ports[index] == null) {
				return "<none>";
			} else {
				return ports[index].getDisplayName();
			}
		}

		@Override
		public void setSelectedItem(Object item) {
			this.item = item;
		}

		@Override
		public Object getSelectedItem() {
			return item;
		}
	}
}
