package com.unknown.emulight.lcp.project;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;

import com.unknown.emulight.lcp.audio.AudioData;
import com.unknown.emulight.lcp.audio.AudioPart;
import com.unknown.emulight.lcp.audio.AudioTrack;
import com.unknown.emulight.lcp.event.ProjectListener;
import com.unknown.emulight.lcp.laser.LaserPart;
import com.unknown.emulight.lcp.laser.LaserProcessor;
import com.unknown.emulight.lcp.laser.LaserRenderer;
import com.unknown.emulight.lcp.laser.LaserTrack;
import com.unknown.emulight.lcp.sequencer.MidiTrack;
import com.unknown.emulight.lcp.sequencer.Sequencer;
import com.unknown.emulight.lcp.ui.MainWindow;
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

	private final AudioTrack systemSounds;

	private long lastStartPosition = 0;

	private final Map<KeyStroke, Object> keyboardShortcuts = new HashMap<>();
	private final Map<Object, Action> keyboardActions = new HashMap<>();

	public Project(EmulightSystem system) {
		this.system = system;

		sequencer.addListener(system.getAudioProcessor());

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

		systemSounds = new AudioTrack(this, "System");

		addTrack(tempoTrack = new TempoTrack(this, "Tempo"));
		addTrack(signatureTrack = new SignatureTrack(this, "Signature"));

		sequencer.setTempoTrack(tempoTrack);

		clearLaserRenderer();
	}

	public void addKeyboardShortcut(KeyStroke key, ActionListener action) {
		addKeyboardShortcut(Set.of(key), new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				action.actionPerformed(e);
			}
		});
	}

	public void addKeyboardShortcut(Set<KeyStroke> keys, Action action) {
		Object link = new Object();
		for(KeyStroke key : keys) {
			keyboardShortcuts.put(key, link);
		}
		keyboardActions.put(link, action);
	}

	public void registerKeyboardShortcuts(JComponent c) {
		InputMap inputMap = c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		for(Entry<KeyStroke, Object> entry : keyboardShortcuts.entrySet()) {
			inputMap.put(entry.getKey(), entry.getValue());
		}

		ActionMap actionMap = c.getActionMap();
		for(Entry<Object, Action> entry : keyboardActions.entrySet()) {
			actionMap.put(entry.getKey(), entry.getValue());
		}
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
		List<AudioTrack> audioTracks = new ArrayList<>();
		for(Track<?> track : tracks) {
			if(track instanceof MidiTrack) {
				midiTracks.add((MidiTrack) track);
			} else if(track instanceof AudioTrack) {
				audioTracks.add((AudioTrack) track);
			}
		}

		sequencer.setTracks(midiTracks);
		sequencer.setAudioTracks(audioTracks);
		sequencer.generateEvents();
		sequencer.play();
	}

	public void stop() {
		sequencer.stop();
	}

	public void playSystemSound(AudioData data) {
		AudioPart part = new AudioPart(data);
		system.getAudioProcessor().playSystem(systemSounds, part, 0, data.getSampleCount());
	}

	public void setTick(long tick) {
		boolean playing = sequencer.isPlaying();
		if(playing) {
			stop();
		}
		sequencer.setTick(tick);
		if(playing) {
			play();
		}
	}

	public void playbackToggle() {
		JFrame mainWindow = system.getMainWindow();
		MainWindow main = mainWindow instanceof MainWindow ? (MainWindow) mainWindow : null;

		if(sequencer.isPlaying()) {
			long tick = sequencer.getTick();
			stop();
			sequencer.setTick(tick);
			if(main != null) {
				main.setStatus("Playback stopped");
			}
		} else {
			lastStartPosition = sequencer.getTick();
			play();
			if(main != null) {
				main.setStatus("Playback started");
			}
		}
	}

	public void playbackStop() {
		if(sequencer.isPlaying()) {
			long tick = sequencer.getTick();
			stop();
			sequencer.setTick(tick);
			JFrame mainWindow = system.getMainWindow();
			MainWindow main = mainWindow instanceof MainWindow ? (MainWindow) mainWindow : null;
			if(main != null) {
				main.setStatus("Playback stopped");
			}
		} else {
			sequencer.setTick(lastStartPosition);
		}
	}

	public void playbackPositionReset() {
		// TODO: once a loop region is implemented, this should go to the start of the loop
		sequencer.setTick(0);
	}

	public void setLaserRenderer(LaserRenderer renderer) {
		if(renderer == null) {
			clearLaserRenderer();
		} else {
			system.getLaserProcessor().setRenderer(renderer);
		}
	}

	public void clearLaserRenderer() {
		system.getLaserProcessor().setRenderer(this::renderLaser);
	}

	private void renderLaser() {
		long time;
		if(sequencer.isPlaying()) {
			long nanotime = sequencer.getTime();
			nanotime -= system.getConfig().getOutputDelay();
			time = tempoTrack.getTick(nanotime);
		} else {
			time = sequencer.getTick();
		}

		Set<InterfaceId> alreadyDone = new HashSet<>();
		for(LaserTrack track : laserTracks) {
			if(track.isMuted()) {
				continue;
			}
			Laser laser = track.getLaser();
			if(laser == null || alreadyDone.contains(laser.getInterfaceId())) {
				continue;
			}
			if(!laser.isConnected()) {
				continue;
			}

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
				case "audio":
					track = new AudioTrack(this, child.getAttribute("name"));
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

		getTempoTrack().recompute();
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
