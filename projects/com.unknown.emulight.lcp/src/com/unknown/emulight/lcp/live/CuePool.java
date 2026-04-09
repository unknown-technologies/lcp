package com.unknown.emulight.lcp.live;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.unknown.xml.dom.Element;

public class CuePool implements MidiReceiver {
	private final Project project;
	private final List<Cue<?>> cues = new ArrayList<>();
	private final CueMap map = new CueMap();

	private MidiInPort controllerPort;
	private final MidiInPort allBusPort;

	private MidiLearner midiLearner;

	private double bpm = 120.0;

	private TriggerKey globalStroboKey;
	private TriggerKey stopAllKey;

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

	public MidiInPort getControllerPort() {
		return controllerPort;
	}

	public void setControllerPort(MidiInPort in) {
		if(controllerPort == allBusPort) {
			project.removeAllBusReceiver(this);
		} else if(controllerPort != null) {
			project.getSystem().getMidiRouter().removeReceiver(controllerPort, this);
		}

		if(in == allBusPort) {
			project.addAllBusReceiver(this);
		} else if(in != null) {
			project.getSystem().getMidiRouter().addReceiver(controllerPort, this);
		}

		controllerPort = in;
	}

	public void setMidiLearn(MidiLearner learner) {
		midiLearner = learner;
	}

	public void clearMidiLearn() {
		midiLearner = null;
	}

	@Override
	public void receive(int status, int data1, int data2) {
		int channel = status & 0x0F;

		MidiLearner learner = midiLearner;
		if(learner != null) {
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
			return;
		}

		switch((byte) (status & 0xF0)) {
		case MIDIEvent.NOTE_ON:
			noteOn(channel, data1);
			break;
		case MIDIEvent.NOTE_OFF:
			noteOff(channel, data1);
			break;
		case MIDIEvent.CTRL_CHANGE:
			controller(channel, data1, data2);
			break;
		}
	}

	public double getBPM() {
		return bpm;
	}

	public void setBPM(double bpm) {
		this.bpm = bpm;
	}

	private void noteOn(int channel, int key) {
		TriggerKey triggerKey = new TriggerKey(channel, key);
		Cue<?> cue = map.getCue(triggerKey);
		if(cue != null) {
			if(cue.isToggleTrigger() && cue.isPlaying()) {
				cue.stop();
			} else {
				cue.play(bpm);
			}
		}

		if(triggerKey.equals(globalStroboKey)) {
			setStroboState(true);
		}

		if(triggerKey.equals(stopAllKey)) {
			stopAll();
		}
	}

	private void noteOff(int channel, int key) {
		TriggerKey triggerKey = new TriggerKey(channel, key);

		if(triggerKey.equals(globalStroboKey)) {
			setStroboState(true);
		}
	}

	private void controller(int channel, int controller, int value) {
		// TODO
	}

	private void setStroboState(boolean on) {
		LaserProcessor processor = project.getSystem().getLaserProcessor();
		processor.setStroboState(on);
	}

	public void stopAll() {
		LaserProcessor processor = project.getSystem().getLaserProcessor();
		processor.clearAllCurrentClips();
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

		clear();

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

		bpm = Double.parseDouble(xml.getAttribute("bpm", "120"));

		Map<Integer, Cue<?>> ids = new HashMap<>();
		for(Element e : xml.getChildren()) {
			if(e.name.equals("cue")) {
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
			} else if(e.name.equals("cue-map")) {
				map.clear();
				map.read(e, ids);
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

		xml.addAttribute("bpm", Double.toString(bpm));

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
