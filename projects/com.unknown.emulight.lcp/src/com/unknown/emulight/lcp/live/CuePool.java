package com.unknown.emulight.lcp.live;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.sound.midi.MidiDevice.Info;
import javax.sound.midi.MidiUnavailableException;

import com.unknown.audio.midi.smf.MIDIEvent;
import com.unknown.emulight.lcp.io.midi.MidiInPort;
import com.unknown.emulight.lcp.io.midi.MidiReceiver;
import com.unknown.emulight.lcp.laser.LaserCue;
import com.unknown.emulight.lcp.laser.LaserPart;
import com.unknown.emulight.lcp.laser.LaserProcessor;
import com.unknown.emulight.lcp.project.AbstractPart;
import com.unknown.emulight.lcp.project.PartPool;
import com.unknown.emulight.lcp.project.Project;
import com.unknown.emulight.lcp.project.Track;
import com.unknown.math.g3d.Mtx44;
import com.unknown.xml.dom.Element;

public class CuePool {
	private final Project project;
	private final List<Cue<?>> cues = new ArrayList<>();
	private final CueMap map = new CueMap();

	private MidiInPort controllerPort;
	private MidiInPort triggerPort;
	private final MidiInPort allBusPort;

	private MidiLearner controllerLearner;
	private MidiLearner triggerLearner;

	private final MidiReceiver controllerReceiver = this::receiveController;
	private final MidiReceiver triggerReceiver = this::receiveTrigger;

	private final Map<Controller, Target> controllers = new ConcurrentHashMap<>();
	private final Map<Target, Controller> controllersLookup = new ConcurrentHashMap<>();

	private final Map<TriggerKey, Trigger> controllerToggles = new ConcurrentHashMap<>();
	private final Map<Trigger, TriggerKey> controllerTogglesLookup = new ConcurrentHashMap<>();

	private final Map<TriggerKey, Trigger> triggerToggles = new ConcurrentHashMap<>();
	private final Map<Trigger, TriggerKey> triggerTogglesLookup = new ConcurrentHashMap<>();

	private double bpm = 120.0;

	private double globalBrightness = 1.0;
	private double globalRed = 1.0;
	private double globalGreen = 1.0;
	private double globalBlue = 1.0;
	private double globalSize = 1.0;
	private double globalRotation = 0.0;
	private double globalTranslationX = 0.0;
	private double globalTranslationY = 0.0;
	private double globalStroboSpeed = 1.0;

	private final Map<String, Target> targets = new HashMap<>();
	private final Map<String, Trigger> triggers = new HashMap<>();

	public static final String TARGET_BRIGHTNESS = "brightness";
	public static final String TARGET_RED = "red";
	public static final String TARGET_GREEN = "green";
	public static final String TARGET_BLUE = "blue";
	public static final String TARGET_SIZE = "size";
	public static final String TARGET_ROTATION = "rotation";
	public static final String TARGET_TRANSLATION_X = "translation-x";
	public static final String TARGET_TRANSLATION_Y = "translation-y";
	public static final String TARGET_STROBO_SPEED = "strobo-speed";

	public static final String TRIGGER_ALL_STOP = "all-stop";
	public static final String TRIGGER_STROBO = "strobo-active";

	public CuePool(Project project) {
		this.project = project;

		// dummy All Bus port
		allBusPort = new MidiInPort(project.getSystem().getMidiRouter()) {
			@Override
			public Info getInfo() {
				return null;
			}

			@Override
			public void openDevice() throws MidiUnavailableException {
				// empty
			}

			@Override
			public void closeDevice() {
				// empty
			}

			@Override
			public String getName() {
				return "All-Bus";
			}
		};

		targets.put(TARGET_BRIGHTNESS, new Target(TARGET_BRIGHTNESS) {
			@Override
			public double getDefault() {
				return 1.0;
			}

			@Override
			protected double get() {
				return getGlobalBrightness();
			}

			@Override
			protected void set(double value) {
				setGlobalBrightness(value);
			}
		});
		targets.put(TARGET_RED, new Target(TARGET_RED) {
			@Override
			public double getDefault() {
				return 1.0;
			}

			@Override
			protected double get() {
				return getGlobalRed();
			}

			@Override
			protected void set(double value) {
				setGlobalRed(value);
			}
		});
		targets.put(TARGET_GREEN, new Target(TARGET_GREEN) {
			@Override
			public double getDefault() {
				return 1.0;
			}

			@Override
			protected double get() {
				return getGlobalGreen();
			}

			@Override
			protected void set(double value) {
				setGlobalGreen(value);
			}
		});
		targets.put(TARGET_BLUE, new Target(TARGET_BLUE) {
			@Override
			public double getDefault() {
				return 1.0;
			}

			@Override
			protected double get() {
				return getGlobalBlue();
			}

			@Override
			protected void set(double value) {
				setGlobalBlue(value);
			}
		});
		targets.put(TARGET_SIZE, new Target(TARGET_SIZE) {
			@Override
			public double getDefault() {
				return 1.0;
			}

			@Override
			protected double get() {
				return getGlobalSize();
			}

			@Override
			protected void set(double value) {
				setGlobalSize(value);
			}
		});
		targets.put(TARGET_ROTATION, new Target(TARGET_ROTATION) {
			@Override
			public double getDefault() {
				return 0.0;
			}

			@Override
			protected double get() {
				return getGlobalRotation() / 360.0;
			}

			@Override
			protected void set(double value) {
				setGlobalRotation(value * 360.0);
			}
		});
		targets.put(TARGET_TRANSLATION_X, new Target(TARGET_TRANSLATION_X) {
			@Override
			public double getDefault() {
				return 0.5;
			}

			@Override
			protected double get() {
				return (getGlobalTranslationX() + 1.0) / 2.0;
			}

			@Override
			protected void set(double value) {
				setGlobalTranslationX(value * 2.0 - 1.0);
			}
		});
		targets.put(TARGET_TRANSLATION_Y, new Target(TARGET_TRANSLATION_Y) {
			@Override
			public double getDefault() {
				return 0.5;
			}

			@Override
			protected double get() {
				return (getGlobalTranslationY() + 1.0) / 2.0;
			}

			@Override
			protected void set(double value) {
				setGlobalTranslationY(value * 2.0 - 1.0);
			}
		});
		targets.put(TARGET_STROBO_SPEED, new Target(TARGET_STROBO_SPEED) {
			@Override
			public double getDefault() {
				return 1.0 / 10.0;
			}

			@Override
			protected double get() {
				return getGlobalStroboSpeed() / 10.0;
			}

			@Override
			protected void set(double value) {
				setGlobalStroboSpeed(value * 10.0);
			}
		});

		triggers.put(TRIGGER_ALL_STOP, new Trigger(TRIGGER_ALL_STOP) {
			@Override
			public void set(boolean state) {
				if(state) {
					stopAll();
				}
			}
		});
		triggers.put(TRIGGER_STROBO, new Trigger(TRIGGER_STROBO) {
			@Override
			public void set(boolean state) {
				setStroboState(state);
			}
		});
	}

	public Project getProject() {
		return project;
	}

	public MidiInPort getAllBusPort() {
		return allBusPort;
	}

	public List<Cue<?>> getCues() {
		return Collections.unmodifiableList(cues);
	}

	public void addCue(Cue<?> cue) {
		cues.add(cue);
	}

	public void removeCue(Cue<?> cue) {
		cues.remove(cue);
		map.removeCue(cue);
	}

	public Cue<?> getCue(int id) {
		return cues.get(id);
	}

	public int size() {
		return cues.size();
	}

	public TriggerKey getTriggerKey(Cue<?> cue) {
		return map.getTriggerKey(cue);
	}

	public void setTriggerKey(Cue<?> cue, TriggerKey key) {
		map.setTrigger(key, cue);
	}

	public void clearTriggerKey(Cue<?> cue) {
		map.removeCue(cue);
	}

	public boolean isTriggerKeyAssigned(TriggerKey key) {
		return map.getCue(key) != null;
	}

	public void clear() {
		cues.clear();
		map.clear();
	}

	private void clearAll() {
		clear();
		controllers.clear();
		controllersLookup.clear();
		controllerToggles.clear();
		controllerTogglesLookup.clear();
		triggerToggles.clear();
		triggerTogglesLookup.clear();
	}

	public MidiInPort getControllerPort() {
		return controllerPort;
	}

	public void setControllerPort(MidiInPort in) {
		if(controllerPort == allBusPort) {
			project.removeAllBusReceiver(controllerReceiver);
		} else if(controllerPort != null) {
			project.getSystem().getMidiRouter().removeReceiver(controllerPort, controllerReceiver);
		}

		if(in == allBusPort) {
			project.addAllBusReceiver(controllerReceiver);
		} else if(in != null) {
			project.getSystem().getMidiRouter().addReceiver(in, controllerReceiver);
		}

		controllerPort = in;
	}

	public MidiInPort getTriggerPort() {
		return triggerPort;
	}

	public void setTriggerPort(MidiInPort in) {
		if(triggerPort == allBusPort) {
			project.removeAllBusReceiver(triggerReceiver);
		} else if(triggerPort != null) {
			project.getSystem().getMidiRouter().removeReceiver(triggerPort, triggerReceiver);
		}

		if(in == allBusPort) {
			project.addAllBusReceiver(triggerReceiver);
		} else if(in != null) {
			project.getSystem().getMidiRouter().addReceiver(in, triggerReceiver);
		}

		triggerPort = in;
	}

	public void setMidiTriggerLearn(MidiLearner learner) {
		triggerLearner = learner;
	}

	public void clearMidiTriggerLearn() {
		triggerLearner = null;
	}

	public void setMidiControllerLearn(MidiLearner learner) {
		controllerLearner = learner;
	}

	public void clearMidiControllerLearn() {
		controllerLearner = null;
	}

	private static void learn(MidiLearner learner, int status, int data1) {
		int channel = status & 0x0F;

		switch((byte) (status & 0xF0)) {
		case MIDIEvent.NOTE_ON:
			learner.noteOn(channel, data1);
			break;
		case MIDIEvent.NOTE_OFF:
			learner.noteOff(channel, data1);
			break;
		case MIDIEvent.CTRL_CHANGE:
			learner.controller(channel, data1);
			break;
		case MIDIEvent.PROG_CHANGE:
			learner.program(channel, data1);
			break;
		case MIDIEvent.PITCH_BEND:
			learner.bend(channel);
			break;
		}
	}

	public void receiveTrigger(int status, int data1, @SuppressWarnings("unused") int data2) {
		int channel = status & 0x0F;

		MidiLearner learner = triggerLearner;
		if(learner != null) {
			learn(learner, status, data1);
			return;
		}

		switch((byte) (status & 0xF0)) {
		case MIDIEvent.NOTE_ON:
			triggerNoteOn(channel, data1);
			break;
		case MIDIEvent.NOTE_OFF:
			triggerNoteOff(channel, data1);
			break;
		case MIDIEvent.CTRL_CHANGE:
			triggerCC(channel, data1, data2);
			break;
		}
	}

	public void receiveController(int status, int data1, int data2) {
		int channel = status & 0x0F;

		MidiLearner learner = controllerLearner;
		if(learner != null) {
			learn(learner, status, data1);
			return;
		}

		switch((byte) (status & 0xF0)) {
		case MIDIEvent.NOTE_ON:
			controllerNoteOn(channel, data1);
			break;
		case MIDIEvent.NOTE_OFF:
			controllerNoteOff(channel, data1);
			break;
		case MIDIEvent.CTRL_CHANGE:
			controllerCC(channel, data1, data2);
			break;
		}
	}

	public double getBPM() {
		return bpm;
	}

	public void setBPM(double bpm) {
		this.bpm = bpm;
	}

	private void triggerNoteOn(int channel, int key) {
		TriggerKey triggerKey = new TriggerKey(TriggerKey.TYPE_NOTE, channel, key);
		Cue<?> cue = map.getCue(triggerKey);
		if(cue != null) {
			if(cue.isToggleTrigger() && cue.isPlaying()) {
				cue.stop();
			} else {
				cue.play(bpm);
			}
		}

		Trigger trigger = triggerToggles.get(triggerKey);
		if(trigger != null) {
			trigger.setState(true);
		}
	}

	private void triggerNoteOff(int channel, int key) {
		TriggerKey triggerKey = new TriggerKey(TriggerKey.TYPE_NOTE, channel, key);

		Trigger trigger = triggerToggles.get(triggerKey);
		if(trigger != null) {
			trigger.setState(false);
		}
	}

	private void triggerCC(int channel, int controller, int value) {
		TriggerKey triggerKey = new TriggerKey(TriggerKey.TYPE_CONTROLLER, channel, controller);

		Trigger trigger = triggerToggles.get(triggerKey);
		if(trigger != null) {
			trigger.setState(value > 64);
		}
	}

	private void controllerNoteOn(int channel, int key) {
		TriggerKey triggerKey = new TriggerKey(TriggerKey.TYPE_NOTE, channel, key);

		Trigger trigger = controllerToggles.get(triggerKey);
		if(trigger != null) {
			trigger.setState(true);
		}
	}

	private void controllerNoteOff(int channel, int key) {
		TriggerKey triggerKey = new TriggerKey(TriggerKey.TYPE_NOTE, channel, key);

		Trigger trigger = controllerToggles.get(triggerKey);
		if(trigger != null) {
			trigger.setState(false);
		}
	}

	private void controllerCC(int channel, int controller, int value) {
		Target target = controllers.get(new Controller(channel, controller));
		if(target != null) {
			target.setValue(value / 127.0);
		}

		TriggerKey triggerKey = new TriggerKey(TriggerKey.TYPE_CONTROLLER, channel, controller);

		Trigger trigger = controllerToggles.get(triggerKey);
		if(trigger != null) {
			trigger.setState(value > 64);
		}
	}

	private void setStroboState(boolean on) {
		LaserProcessor processor = project.getSystem().getLaserProcessor();
		processor.setStroboState(on);
	}

	public void stopAll() {
		LaserProcessor processor = project.getSystem().getLaserProcessor();
		processor.clearAllCurrentClips();
	}

	public Target getTarget(String name) {
		return targets.get(name);
	}

	public Trigger getTrigger(String name) {
		return triggers.get(name);
	}

	public Controller getController(Target target) {
		return controllersLookup.get(target);
	}

	public TriggerKey getControllerToggle(Trigger target) {
		return controllerTogglesLookup.get(target);
	}

	public TriggerKey getTriggerToggle(Trigger target) {
		return triggerTogglesLookup.get(target);
	}

	public void mapController(Controller controller, Target target) {
		// remove old mapping
		Controller oldController = controllersLookup.remove(target);
		if(oldController != null) {
			controllers.remove(oldController);
		}

		// add new mapping
		controllers.put(controller, target);
		controllersLookup.put(target, controller);
	}

	public void unmapController(Target target) {
		Controller controller = controllersLookup.remove(target);
		if(controller != null) {
			controllers.remove(controller);
		}
	}

	public boolean isControllerAssigned(Controller controller) {
		return controllers.containsKey(controller);
	}

	public void mapControllerToggle(TriggerKey key, Trigger trigger) {
		// remove old mapping
		TriggerKey oldKey = controllerTogglesLookup.remove(trigger);
		if(oldKey != null) {
			controllerToggles.remove(oldKey);
		}

		// add new mapping
		controllerToggles.put(key, trigger);
		controllerTogglesLookup.put(trigger, key);
	}

	public void unmapControllerToggle(Trigger trigger) {
		TriggerKey key = controllerTogglesLookup.remove(trigger);
		if(key != null) {
			controllerToggles.remove(key);
		}
	}

	public boolean isControllerToggleAssigned(TriggerKey key) {
		return controllerToggles.containsKey(key);
	}

	public void mapTriggerToggle(TriggerKey key, Trigger trigger) {
		// remove old mapping
		TriggerKey oldKey = triggerTogglesLookup.remove(trigger);
		if(oldKey != null) {
			triggerToggles.remove(oldKey);
		}

		// add new mapping
		triggerToggles.put(key, trigger);
		triggerTogglesLookup.put(trigger, key);
	}

	public void unmapTriggerToggle(Trigger trigger) {
		TriggerKey key = triggerTogglesLookup.remove(trigger);
		if(key != null) {
			triggerToggles.remove(key);
		}
	}

	public boolean isTriggerToggleAssigned(TriggerKey key) {
		return triggerToggles.containsKey(key);
	}

	public double getGlobalBrightness() {
		return globalBrightness;
	}

	public void setGlobalBrightness(double brightness) {
		globalBrightness = brightness;
		recomputeColor();
	}

	public double getGlobalRed() {
		return globalRed;
	}

	public double getGlobalGreen() {
		return globalGreen;
	}

	public double getGlobalBlue() {
		return globalBlue;
	}

	public void setGlobalRed(double red) {
		globalRed = red;
		recomputeColor();
	}

	public void setGlobalGreen(double green) {
		globalGreen = green;
		recomputeColor();
	}

	public void setGlobalBlue(double blue) {
		globalBlue = blue;
		recomputeColor();
	}

	public double getGlobalSize() {
		return globalSize;
	}

	public void setGlobalSize(double size) {
		globalSize = size;
		recomputeTransform();
	}

	public double getGlobalRotation() {
		return globalRotation;
	}

	public void setGlobalRotation(double rotation) {
		globalRotation = rotation;
		recomputeTransform();
	}

	public double getGlobalTranslationX() {
		return globalTranslationX;
	}

	public void setGlobalTranslationX(double translationX) {
		globalTranslationX = translationX;
		recomputeTransform();
	}

	public double getGlobalTranslationY() {
		return globalTranslationY;
	}

	public void setGlobalTranslationY(double translationY) {
		globalTranslationY = translationY;
		recomputeTransform();
	}

	public double getGlobalStroboSpeed() {
		return globalStroboSpeed;
	}

	public void setGlobalStroboSpeed(double speed) {
		globalStroboSpeed = speed;
		LaserProcessor laser = project.getSystem().getLaserProcessor();
		laser.setStroboSpeed(speed);
	}

	private void recomputeColor() {
		Mtx44 mtx = Mtx44.scale(globalBrightness * globalRed, globalBrightness * globalGreen,
				globalBrightness * globalBlue);
		LaserProcessor laser = project.getSystem().getLaserProcessor();
		laser.setColorTransform(mtx);
	}

	private void recomputeTransform() {
		Mtx44 mtx = Mtx44.rotDegZ(globalRotation).transApply(globalTranslationX, globalTranslationY, 0)
				.applyScale(globalSize, globalSize, globalSize);
		LaserProcessor laser = project.getSystem().getLaserProcessor();
		laser.setPositionTransform(mtx);
	}

	public void addPartsToPool(PartPool pool) {
		for(Cue<?> cue : cues) {
			pool.add(cue.getPart(), cue.getType());
		}
	}

	public void read(Element xml, Map<Integer, AbstractPart> parts) throws IOException {
		if(!xml.name.equals("cues")) {
			throw new IOException("not a cue pool");
		}

		clearAll();

		if(xml.getAttribute("controller-port") != null) {
			String portName = xml.getAttribute("controller-port");
			if(portName.equals("<all-bus>")) {
				setControllerPort(allBusPort);
			} else {
				MidiInPort[] ports = project.getSystem().getMidiRouter().getInputPorts();
				for(MidiInPort in : ports) {
					if(in.getDisplayName() != null && in.getDisplayName().equals(portName)) {
						setControllerPort(in);
						break;
					}
				}
			}
		}

		if(xml.getAttribute("trigger-port") != null) {
			String portName = xml.getAttribute("trigger-port");
			if(portName.equals("<all-bus>")) {
				setTriggerPort(allBusPort);
			} else {
				MidiInPort[] ports = project.getSystem().getMidiRouter().getInputPorts();
				for(MidiInPort in : ports) {
					if(in.getDisplayName() != null && in.getDisplayName().equals(portName)) {
						setTriggerPort(in);
						break;
					}
				}
			}
		}

		bpm = Double.parseDouble(xml.getAttribute("bpm", "120"));

		Map<Integer, Cue<?>> ids = new HashMap<>();
		for(Element e : xml.getChildren()) {
			switch(e.name) {
			case "cue": {
				Cue<?> cue = null;
				AbstractPart part = parts.get(Integer.parseInt(e.getAttribute("part")));
				switch(e.getAttribute("type")) {
				case Track.NAME_LASER:
					cue = new LaserCue(project, (LaserPart) part);
					break;
				}

				if(cue != null) {
					int id = Integer.parseInt(e.getAttribute("id"));
					cue.read(e);
					addCue(cue);
					ids.put(id, cue);
				}
				break;
			}
			case "cue-map":
				map.clear();
				map.read(e, ids);
				break;
			case "color-transform":
				globalBrightness = Double.parseDouble(e.getAttribute("brightness", "1.0"));
				globalRed = Double.parseDouble(e.getAttribute("red", "1.0"));
				globalGreen = Double.parseDouble(e.getAttribute("green", "1.0"));
				globalBlue = Double.parseDouble(e.getAttribute("blue", "1.0"));
				recomputeColor();
				break;
			case "position-transform":
				globalRotation = Double.parseDouble(e.getAttribute("rotation", "0.0"));
				globalSize = Double.parseDouble(e.getAttribute("size", "1.0"));
				globalTranslationX = Double.parseDouble(e.getAttribute("translation-x", "0.0"));
				globalTranslationY = Double.parseDouble(e.getAttribute("translation-y", "0.0"));
				recomputeTransform();
				break;
			case "controller": {
				int channel = Integer.parseInt(e.getAttribute("channel", "0"));
				int cc = Integer.parseInt(e.getAttribute("cc", "1"));
				Target target = getTarget(e.getAttribute("target"));
				if(target == null) {
					throw new IOException("unknown target: " + e.getAttribute("target"));
				}
				mapController(new Controller(channel, cc), target);
				break;
			}
			case "toggle": {
				int type = TriggerKey.getType(e.getAttribute("event", "note"));
				int channel = Integer.parseInt(e.getAttribute("channel", "0"));
				int key = Integer.parseInt(e.getAttribute("key", "0"));
				Trigger target = getTrigger(e.getAttribute("target"));
				if(target == null) {
					throw new IOException("unknown target: " + e.getAttribute("target"));
				}
				switch(e.getAttribute("type", "controller")) {
				case "controller":
					mapControllerToggle(new TriggerKey(type, channel, key), target);
					break;
				case "trigger":
					mapTriggerToggle(new TriggerKey(type, channel, key), target);
					break;
				default:
					throw new IOException("unknown type: " + e.getAttribute("type"));
				}
				break;
			}
			}
		}
	}

	public Element write(PartPool pool) {
		Element xml = new Element("cues");

		if(controllerPort == allBusPort) {
			xml.addAttribute("controller-port", "<all-bus>");
		} else if(controllerPort != null) {
			xml.addAttribute("controller-port", controllerPort.getDisplayName());
		}

		if(triggerPort == allBusPort) {
			xml.addAttribute("trigger-port", "<all-bus>");
		} else if(triggerPort != null) {
			xml.addAttribute("trigger-port", triggerPort.getDisplayName());
		}

		xml.addAttribute("bpm", Double.toString(bpm));

		Element postransform = new Element("position-transform");
		postransform.addAttribute("rotation", Double.toString(globalRotation));
		postransform.addAttribute("size", Double.toString(globalSize));
		postransform.addAttribute("translation-x", Double.toString(globalTranslationX));
		postransform.addAttribute("translation-y", Double.toString(globalTranslationY));
		xml.addChild(postransform);

		Element colortransform = new Element("color-transform");
		colortransform.addAttribute("brightness", Double.toString(globalBrightness));
		colortransform.addAttribute("red", Double.toString(globalRed));
		colortransform.addAttribute("green", Double.toString(globalGreen));
		colortransform.addAttribute("blue", Double.toString(globalBlue));
		xml.addChild(colortransform);

		for(Entry<Controller, Target> entry : controllers.entrySet()) {
			Controller ctl = entry.getKey();
			Target target = entry.getValue();
			Element controller = new Element("controller");
			controller.addAttribute("channel", Integer.toString(ctl.getChannel()));
			controller.addAttribute("cc", Integer.toString(ctl.getController()));
			controller.addAttribute("target", target.getName());
			xml.addChild(controller);
		}

		for(Entry<TriggerKey, Trigger> entry : controllerToggles.entrySet()) {
			TriggerKey key = entry.getKey();
			Trigger trigger = entry.getValue();

			Element toggle = new Element("toggle");
			toggle.addAttribute("event", key.getTypeName());
			toggle.addAttribute("channel", Integer.toString(key.getChannel()));
			toggle.addAttribute("key", Integer.toString(key.getKey()));
			toggle.addAttribute("target", trigger.getName());
			toggle.addAttribute("type", "controller");
			xml.addChild(toggle);
		}

		for(Entry<TriggerKey, Trigger> entry : triggerToggles.entrySet()) {
			TriggerKey key = entry.getKey();
			Trigger trigger = entry.getValue();

			Element toggle = new Element("toggle");
			toggle.addAttribute("event", key.getTypeName());
			toggle.addAttribute("channel", Integer.toString(key.getChannel()));
			toggle.addAttribute("key", Integer.toString(key.getKey()));
			toggle.addAttribute("target", trigger.getName());
			toggle.addAttribute("type", "trigger");
			xml.addChild(toggle);
		}

		int id = 0;
		Map<Cue<?>, Integer> ids = new HashMap<>();
		for(Cue<?> cue : cues) {
			Element cuexml = cue.write(pool);
			cuexml.addAttribute("id", Integer.toString(id));
			xml.addChild(cuexml);
			ids.put(cue, id);
			id++;
		}

		xml.addChild(map.write(ids));

		return xml;
	}
}
