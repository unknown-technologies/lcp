package com.unknown.emulight.lcp.ui.live;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
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
import com.unknown.emulight.lcp.project.AbstractPart;
import com.unknown.emulight.lcp.project.PartContainer;
import com.unknown.emulight.lcp.project.Project;
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
	private JComboBox<String> port;

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
		});

		ports = new MidiInPort[0];
		port = new JComboBox<>(new PortModel());
		port.addItemListener(e -> {
			int idx = port.getSelectedIndex();
			if(idx == -1) {
				return;
			}
			cues.setControllerPort(ports[idx]);
		});

		refreshMidiPorts();

		JButton transferButton = new JButton("Transfer from Tracks");
		transferButton.addActionListener(e -> transferFromTracks());

		JButton deleteAll = new JButton("Delete All");
		deleteAll.addActionListener(e -> deleteAll());

		JButton stopAll = new JButton("Stop All");
		stopAll.addActionListener(e -> project.getCuePool().stopAll());

		JPanel options = new JPanel(new FlowLayout(FlowLayout.CENTER));
		options.add(transferButton);
		options.add(deleteAll);
		options.add(new JLabel("Controller Port:"));
		options.add(port);
		options.add(new JLabel("BPM:"));
		options.add(bpm);
		options.add(stopAll);
		add(BorderLayout.NORTH, options);

		// faders
		CuePool pool = project.getCuePool();

		JPanel controls = new JPanel(new FlowLayout());
		controls.add(createFader("Bright", "Brightness", () -> pool.getGlobalBrightness(),
				x -> pool.setGlobalBrightness(x), () -> 1.0));
		controls.add(createFader("Red", () -> pool.getGlobalRed(),
				x -> pool.setGlobalRed(x), () -> 1.0));
		controls.add(createFader("Green", () -> pool.getGlobalGreen(),
				x -> pool.setGlobalGreen(x), () -> 1.0));
		controls.add(createFader("Blue", () -> pool.getGlobalBlue(),
				x -> pool.setGlobalBlue(x), () -> 1.0));
		controls.add(createFader("Size", () -> pool.getGlobalSize(),
				x -> pool.setGlobalSize(x), () -> 1.0));
		controls.add(createFader("Rot", "Rotation", () -> pool.getGlobalRotation() / 360.0,
				x -> pool.setGlobalRotation(x * 360.0), () -> 0.0));
		controls.add(createFader("X", "Translation X", () -> (pool.getGlobalTranslationX() + 1.0) / 2.0,
				x -> pool.setGlobalTranslationX(x * 2.0 - 1.0), () -> 0.5));
		controls.add(createFader("Y", "Translation Y", () -> (pool.getGlobalTranslationY() + 1.0) / 2.0,
				x -> pool.setGlobalTranslationY(x * 2.0 - 1.0), () -> 0.5));

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

	@FunctionalInterface
	private interface GetValue {
		double get();
	}

	@FunctionalInterface
	private interface SetValue {
		void set(double value);
	}

	private JPanel createFader(String name, GetValue get, SetValue set, GetValue def) {
		return createFader(name, name, get, set, def);
	}

	private JPanel createFader(String name, String tooltip, GetValue get, SetValue set, GetValue def) {
		JSlider fader = new JSlider(JSlider.VERTICAL, 0, 127,
				(int) Math.round(get.get() * 127.0));
		JSpinner numericFader = new JSpinner(new SpinnerNumberModel(fader.getValue(), 0, 127, 1));

		fader.addChangeListener(e -> {
			int value = fader.getValue();
			int spinner = ((SpinnerNumberModel) numericFader.getModel()).getNumber().intValue();
			if(spinner != value) {
				numericFader.setValue(value);
			}
			set.set(value / 127.0);
		});

		fader.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
					int value = (int) Math.round(def.get() * 127.0);
					numericFader.setValue(value);
					e.consume();
				}
			}
		});

		numericFader.addChangeListener(e -> {
			int value = ((SpinnerNumberModel) numericFader.getModel()).getNumber().intValue();
			int slider = fader.getValue();
			if(slider != value) {
				fader.setValue(value);
			}
		});

		JPanel faderControls = new JPanel();
		faderControls.setBorder(UIUtils.border(name));
		faderControls.setToolTipText(tooltip);
		faderControls.setLayout(new BoxLayout(faderControls, BoxLayout.Y_AXIS));
		faderControls.add(fader);

		JPanel numericFaderPanel = new JPanel(new FlowLayout());
		numericFaderPanel.add(numericFader);
		numericFader.setMaximumSize(numericFader.getPreferredSize());
		faderControls.add(numericFaderPanel);

		updaters.add(() -> {
			int value = (int) Math.round(get.get() * 127.0);
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

		int selectedPort = -1;
		for(int i = 0; i < ports.length; i++) {
			if(ports[i] == cues.getControllerPort()) {
				selectedPort = i;
				break;
			}
		}

		port.setSelectedIndex(selectedPort);
	}

	private void reload() {
		CuePool cues = project.getCuePool();

		int selectedPort = -1;
		for(int i = 0; i < ports.length; i++) {
			if(ports[i] == cues.getControllerPort()) {
				selectedPort = i;
				break;
			}
		}

		port.setSelectedIndex(selectedPort);

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
