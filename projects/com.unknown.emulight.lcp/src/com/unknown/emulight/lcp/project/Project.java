package com.unknown.emulight.lcp.project;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import com.unknown.emulight.lcp.event.ProjectListener;
import com.unknown.emulight.lcp.laser.LaserPart;
import com.unknown.emulight.lcp.laser.LaserProcessor;
import com.unknown.emulight.lcp.laser.LaserTrack;
import com.unknown.emulight.lcp.sequencer.MidiTrack;
import com.unknown.emulight.lcp.sequencer.Sequencer;
import com.unknown.math.g3d.Mtx44;
import com.unknown.net.shownet.InterfaceId;
import com.unknown.net.shownet.Laser;
import com.unknown.net.shownet.Point;
import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;
import com.unknown.xml.dom.Element;

public class Project {
	private static final Logger log = Trace.create(Project.class);

	private final EmulightSystem system;

	private String name;

	private int ppq;

	private final Sequencer sequencer = new Sequencer();

	private final List<Track<?>> tracks = new ArrayList<>();
	private final List<LaserTrack> laserTracks = new ArrayList<>();

	private final List<ProjectListener> listeners = new ArrayList<>();

	private final List<MidiTrack> allBusTracks = new ArrayList<>();

	private final TempoTrack tempoTrack;
	private final SignatureTrack signatureTrack;

	private final List<Color> colors = new ArrayList<>();

	public Project(EmulightSystem system) {
		this.system = system;

		// default colors
		colors.add(new Color(0x8EA0B2));
		colors.add(new Color(0xE53636));
		colors.add(new Color(0xE57636));
		colors.add(new Color(0xE5BA3B));
		colors.add(new Color(0xD5E84C));
		colors.add(new Color(0x8DE536));
		colors.add(new Color(0x51D83C));
		colors.add(new Color(0x35DD5F));
		colors.add(new Color(0x33D697));
		colors.add(new Color(0x30CCCC));
		colors.add(new Color(0x40AAE8));
		colors.add(new Color(0x5D80EA));
		colors.add(new Color(0x796AED));
		colors.add(new Color(0xA056EA));
		colors.add(new Color(0xCF44E5));
		colors.add(new Color(0xE536B9));
		colors.add(new Color(0xE53679));
		colors.add(new Color(0xFE0000));
		colors.add(new Color(0x00FEFE));
		colors.add(new Color(0x0000FE));
		colors.add(new Color(0xE47535));
		colors.add(new Color(0xAF6929));
		colors.add(new Color(0x885732));

		setPPQ(1920);

		setName("Untitled Project");

		addTrack(tempoTrack = new TempoTrack(this, "Tempo"));
		addTrack(signatureTrack = new SignatureTrack(this, "Signature"));
	}

	public EmulightSystem getSystem() {
		return system;
	}

	public LaserProcessor getProcessor() {
		return system.getLaserProcessor();
	}

	public Color getColor(int index) {
		if(index < 0 || index >= colors.size()) {
			return colors.get(0);
		} else {
			return colors.get(index);
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		fireEvent(ProjectListener.NAME);
	}

	public int getPPQ() {
		return ppq;
	}

	public void setPPQ(int ppq) {
		this.ppq = ppq;
		sequencer.setTempo(ppq, 120);
	}

	public void addTrack(Track<?> track) {
		tracks.add(track);
		if(track instanceof LaserTrack) {
			laserTracks.add((LaserTrack) track);
		}
		fireTrackAdded(track);
	}

	public void removeTrack(Track<?> track) {
		if(!track.isPermanent()) {
			if(track.isRecordingArmed() && track instanceof MidiTrack) {
				removeAllBusTrack((MidiTrack) track);
			}

			tracks.remove(track);
			if(track instanceof LaserTrack) {
				laserTracks.remove(track);
			}
			fireTrackRemoved(track);
		}
	}

	public Track<?> duplicateTrack(Track<?> track) {
		if(track.isPermanent()) {
			return null;
		}

		Track<?> newTrack = track.clone();
		addTrack(newTrack);

		return track;
	}

	public Track<?> getTrack(int index) {
		return tracks.get(index);
	}

	public List<Track<?>> getTracks() {
		return Collections.unmodifiableList(tracks);
	}

	public TempoTrack getTempoTrack() {
		return tempoTrack;
	}

	public SignatureTrack getSignatureTrack() {
		return signatureTrack;
	}

	public void addAllBusTrack(MidiTrack track) {
		allBusTracks.add(track);
	}

	public void removeAllBusTrack(MidiTrack track) {
		allBusTracks.remove(track);
	}

	public Sequencer getSequencer() {
		return sequencer;
	}

	public void play() {
		sequencer.stop();

		List<MidiTrack> midiTracks = new ArrayList<>();
		for(Track<?> track : tracks) {
			if(track instanceof MidiTrack) {
				midiTracks.add((MidiTrack) track);
			}
		}

		system.getLaserProcessor().setRenderer(this::renderLaser);

		sequencer.setTempoTrack(tempoTrack);
		sequencer.setTracks(midiTracks);
		sequencer.play();
	}

	public void stop() {
		sequencer.stop();
		system.getLaserProcessor().setRenderer(null);
		system.getLaserProcessor().resetAll();
	}

	private void renderLaser() {
		Set<InterfaceId> alreadyDone = new HashSet<>();
		for(LaserTrack track : laserTracks) {
			if(track.isMuted()) {
				continue;
			}
			Laser laser = track.getLaser();
			if(laser == null || alreadyDone.contains(laser.getInterfaceId())) {
				continue;
			}

			long time = sequencer.getTick();
			PartContainer<LaserPart> container = track.getFloorPart(time);
			if(container == null) {
				continue;
			}
			if(container.getEnd() < time) {
				continue;
			}
			alreadyDone.add(laser.getInterfaceId());
			LaserPart part = container.getPart();
			long localTime = container.getLocalTime(time);
			double vol = track.getVolume();
			if(part.isLoop()) {
				localTime %= part.getLength();
			}
			List<Point> points = part.render((int) localTime, new Mtx44(), Mtx44.scale(vol, vol, vol));
			try {
				laser.sendFrame(points, part.getSpeed());
			} catch(IOException e) {
				log.log(Levels.WARNING, "Failed to send frame: " + e.getMessage(), e);
			}
		}

		// send empty frames to all other lasers
		for(Laser laser : system.getLaserProcessor().getLasers()) {
			if(!alreadyDone.contains(laser.getInterfaceId())) {
				try {
					laser.sendFrame(List.of(new Point()), 1000);
				} catch(IOException e) {
					log.log(Levels.WARNING, "Failed to send frame continuation: " + e.getMessage(),
							e);
				}
			}
		}
	}

	public void inputAllBus(int status, int data1, int data2) {
		for(MidiTrack track : allBusTracks) {
			track.inputAllBus(status, data1, data2);
		}
	}

	public void addProjectListener(ProjectListener listener) {
		listeners.add(listener);
	}

	public void removeProjectListener(ProjectListener listener) {
		listeners.remove(listener);
	}

	protected void fireEvent(String key) {
		for(ProjectListener listener : listeners) {
			try {
				listener.propertyChanged(key);
			} catch(Throwable t) {
				log.log(Levels.ERROR, "Failed to execute project listener", t);
			}
		}
	}

	protected void fireTrackAdded(Track<?> track) {
		for(ProjectListener listener : listeners) {
			try {
				listener.trackAdded(track);
			} catch(Throwable t) {
				log.log(Levels.ERROR, "Failed to execute project listener", t);
			}
		}
	}

	protected void fireTrackRemoved(Track<?> track) {
		for(ProjectListener listener : listeners) {
			try {
				listener.trackRemoved(track);
			} catch(Throwable t) {
				log.log(Levels.ERROR, "Failed to execute project listener", t);
			}
		}
	}

	public void load(Element xml) throws IOException {
		if(!xml.name.equals("project")) {
			throw new IOException("not a project file");
		}

		List<Track<?>> tracksToRemove = new ArrayList<>();
		for(Track<?> track : tracks) {
			if(track != tempoTrack && track != signatureTrack) {
				tracksToRemove.add(track);
			}
		}

		for(Track<?> track : tracksToRemove) {
			removeTrack(track);
		}

		tracks.clear();

		setName(xml.getAttribute("name"));
		setPPQ(Integer.parseInt(xml.getAttribute("ppq")));

		for(Element child : xml.getChildren()) {
			if(child.name.equals("track")) {
				Track<?> track;

				switch(child.getAttribute("type")) {
				case "tempo":
					track = tempoTrack;
					break;
				case "signature":
					track = signatureTrack;
					break;
				case "midi":
					track = new MidiTrack(this, child.getAttribute("name"));
					break;
				case "laser":
					track = new LaserTrack(this, child.getAttribute("name"),
							child.getAttribute("laser"));
					break;
				default:
					throw new IOException("unknown track type: " + child.getAttribute("type"));
				}

				addTrack(track);
				track.read(child);
			}
		}
	}

	public Element write() {
		Element xml = new Element("project");

		xml.addAttribute("name", name);
		xml.addAttribute("ppq", Integer.toString(ppq));

		for(Track<?> track : tracks) {
			xml.addChild(track.write());
		}

		return xml;
	}
}
