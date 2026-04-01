package com.unknown.emulight.lcp.project;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.logging.Logger;

import com.unknown.emulight.lcp.event.TrackListener;
import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;
import com.unknown.xml.dom.Element;

public abstract class Track<T extends AbstractPart> {
	public static final int AUDIO = 0;
	public static final int INSTRUMENT = 1;
	public static final int MIDI = 2;
	public static final int SAMPLER = 3;
	public static final int ARRANGER = 4;
	public static final int CHORD = 5;
	public static final int FX = 6;
	public static final int FOLDER = 7;
	public static final int GROUP = 8;
	public static final int MARKER = 9;
	public static final int RULER = 10;
	public static final int SIGNATURE = 11;
	public static final int TEMPO = 12;
	public static final int TRANSPOSE = 13;
	public static final int VCA = 14;
	public static final int VIDEO = 15;
	public static final int LASER = 16;
	public static final int DMX = 17;

	public static final String[] TRACK_TYPES = {
			/* 000 */ "audio",
			/* 001 */ "instrument",
			/* 002 */ "midi",
			/* 003 */ "sampler",
			/* 004 */ "arranger",
			/* 005 */ "chord",
			/* 006 */ "fx",
			/* 007 */ "folder",
			/* 008 */ "group",
			/* 009 */ "marker",
			/* 010 */ "ruler",
			/* 011 */ "signature",
			/* 012 */ "tempo",
			/* 013 */ "transpose",
			/* 014 */ "vca",
			/* 015 */ "video",
			/* 016 */ "laser",
			/* 017 */ "dmx"
	};

	private static final Logger log = Trace.create(Track.class);

	private final int type;
	private String name;
	private int color;

	private boolean muted;
	private boolean solo;
	private boolean recording;
	private boolean monitoring;
	private boolean enabled;
	private boolean locked;

	private double volume;

	private final Project project;

	private final NavigableMap<Long, PartContainer<T>> parts = new TreeMap<>();

	private final List<TrackListener> listeners = new ArrayList<>();

	public Track(int type, Project project, String name) {
		this.type = type;
		this.name = name;
		this.project = project;

		enabled = true;
		volume = 1.0;
	}

	protected void copy(Track<T> other) {
		other.name = name;
		other.color = color;
		other.muted = muted;
		other.solo = solo;
		other.recording = recording;
		other.monitoring = monitoring;
		other.enabled = enabled;
		other.locked = locked;
		other.volume = volume;

		for(PartContainer<T> container : parts.sequencedValues()) {
			// NOTE: this creates a shared copy of the parts
			PartContainer<T> c = other.addPart(container.getTime(), container.getPart());
			c.setTrimStart(container.getTrimStart());
			c.setLength(container.getLength());
		}
	}

	public Project getProject() {
		return project;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		String oldName = this.name;
		this.name = name;
		fireEvent(TrackListener.NAME, oldName, name);
	}

	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		int oldColor = this.color;
		this.color = color;
		fireEvent(TrackListener.NAME, oldColor, color);
	}

	public int getType() {
		return type;
	}

	public boolean isPermanent() {
		return type == TEMPO || type == SIGNATURE;
	}

	public boolean isMuted() {
		return muted;
	}

	public void setMuted(boolean muted) {
		boolean oldMuted = this.muted;
		this.muted = muted;
		if(fireEvent(TrackListener.MUTE, oldMuted, muted)) {
			if(muted) {
				onMute();
			} else {
				onUnmute();
			}
		}
	}

	protected void onMute() {
		// override in subclasses
	}

	protected void onUnmute() {
		// override in subclasses
	}

	public boolean isSolo() {
		return solo;
	}

	public void setSolo(boolean solo) {
		boolean oldSolo = this.solo;
		this.solo = solo;
		fireEvent(TrackListener.SOLO, oldSolo, solo);
	}

	public boolean isRecordingArmed() {
		return recording;
	}

	public void setRecordingArmed(boolean recording) {
		boolean oldRecording = this.recording;
		this.recording = recording;
		if(fireEvent(TrackListener.RECORD, oldRecording, recording)) {
			if(recording) {
				onRecordingArmed();
			} else {
				onRecordingDisarmed();
			}
		}
	}

	protected void onRecordingArmed() {
		// override in subclasses
	}

	protected void onRecordingDisarmed() {
		// override in subclasses
	}

	public boolean isMonitoring() {
		return monitoring;
	}

	public void setMonitoring(boolean monitoring) {
		boolean oldMonitoring = this.monitoring;
		this.monitoring = monitoring;
		fireEvent(TrackListener.MONITOR, oldMonitoring, monitoring);
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		boolean oldEnabled = this.enabled;
		this.enabled = enabled;
		fireEvent(TrackListener.ENABLE, oldEnabled, enabled);
	}

	public boolean isLocked() {
		return locked;
	}

	public void setLocked(boolean locked) {
		boolean oldLocked = this.locked;
		this.locked = locked;
		fireEvent(TrackListener.LOCK, oldLocked, locked);
	}

	public double getVolume() {
		return volume;
	}

	public void setVolume(double volume) {
		double oldVolume = this.volume;
		this.volume = volume;
		if(fireEvent(TrackListener.VOLUME, oldVolume, volume)) {
			onVolumeChanged();
		}
	}

	protected void onVolumeChanged() {
		// override in subclasses
	}

	public PartContainer<T> addPart(long time, T part) {
		PartContainer<T> container = new PartContainer<>(this, part, time);
		container.getPart().addReference(container);
		parts.put(time, container);
		fireEvent(TrackListener.PART);
		return container;
	}

	public void removePart(PartContainer<T> part) {
		parts.remove(part.getTime());
		part.getPart().removeReference(part);
		fireEvent(TrackListener.PART);
	}

	public PartContainer<T> movePart(long time, PartContainer<T> container) {
		if(parts.get(container.getTime()) != container) {
			// the part was never on this track
			return null;
		}
		if(container.getTime() == time) {
			return container;
		}
		PartContainer<T> newContainer = container.moveInternal(time, this);
		newContainer.getPart().removeReference(container);
		newContainer.getPart().addReference(newContainer);
		parts.remove(container.getTime());
		parts.put(time, newContainer);
		fireEvent(TrackListener.PART);
		return newContainer;
	}

	public PartContainer<T> clonePart(long time, PartContainer<T> container) {
		if(parts.get(container.getTime()) == container && container.getTime() == time) {
			// the part was on this track already, therefore no change
			return container;
		}
		PartContainer<T> newContainer = container.cloneAt(time, this);
		newContainer.getPart().addReference(newContainer);
		parts.put(time, newContainer);
		fireEvent(TrackListener.PART);
		return newContainer;
	}

	public PartContainer<T> linkPart(long time, PartContainer<T> container) {
		if(parts.get(container.getTime()) == container && container.getTime() == time) {
			// the part was on this track already, therefore no change
			return container;
		}
		PartContainer<T> newContainer = container.moveInternal(time, this);
		newContainer.getPart().addReference(newContainer);
		parts.put(time, newContainer);
		fireEvent(TrackListener.PART);
		return newContainer;
	}

	public List<PartContainer<T>> getParts() {
		return new ArrayList<>(parts.sequencedValues());
	}

	public PartContainer<T> getFloorPart(long time) {
		Entry<Long, PartContainer<T>> entry = parts.floorEntry(time);
		if(entry != null) {
			return entry.getValue();
		} else {
			return null;
		}
	}

	public PartContainer<T> getCeilingPart(long time) {
		Entry<Long, PartContainer<T>> entry = parts.ceilingEntry(time);
		if(entry != null) {
			return entry.getValue();
		} else {
			return null;
		}
	}

	public PartContainer<T> createPart(long time, long length) {
		T part = createPart();
		if(part == null) {
			return null;
		}

		part.setName(getName());

		PartContainer<T> container = addPart(time, part);
		container.setLength(length);
		return container;
	}

	public void addTrackListener(TrackListener listener) {
		listeners.add(listener);
	}

	public void removeTrackListener(TrackListener listener) {
		listeners.remove(listener);
	}

	protected boolean fireEvent(String key, boolean oldValue, boolean newValue) {
		if(oldValue != newValue) {
			fireEvent(key);
			return true;
		} else {
			return false;
		}
	}

	protected boolean fireEvent(String key, int oldValue, int newValue) {
		if(oldValue != newValue) {
			fireEvent(key);
			return true;
		} else {
			return false;
		}
	}

	protected boolean fireEvent(String key, double oldValue, double newValue) {
		// careful here: the exact "!=" is intentional!
		if(oldValue != newValue) {
			fireEvent(key);
			return true;
		} else {
			return false;
		}
	}

	protected boolean fireEvent(String key, Object oldValue, Object newValue) {
		if((oldValue != null && !oldValue.equals(newValue)) || (oldValue == null && newValue != null) ||
				(oldValue != null && newValue == null)) {
			fireEvent(key);
			return true;
		} else {
			return false;
		}
	}

	protected void fireEvent(String key) {
		for(TrackListener listener : listeners) {
			try {
				listener.propertyChanged(key);
			} catch(Throwable t) {
				log.log(Levels.ERROR, "Failed to execute track listener", t);
			}
		}
	}

	protected abstract void readTrack(Element xml) throws IOException;

	protected abstract void writeTrack(Element xml);

	protected abstract T createPart();

	@SuppressWarnings("unchecked")
	private void readParts(Element xml, Map<Integer, AbstractPart> pool) throws IOException {
		Map<Integer, T> uniqueParts = new HashMap<>();

		parts.clear();

		T dummy = createPart();
		Class<?> clazz = dummy != null ? dummy.getClass() : null;

		for(Element child : xml.getChildren()) {
			switch(child.name) {
			case "part": {
				T part = createPart();
				String partName = child.getAttribute("name");
				if(partName != null) {
					part.setName(partName);
				}
				part.read(child);
				int id = Integer.parseInt(child.getAttribute("id"));
				uniqueParts.put(id, part);
				break;
			}
			case "container": {
				int id = Integer.parseInt(child.getAttribute("part"));
				long time = Long.parseLong(child.getAttribute("time"));
				T part = uniqueParts.get(id);
				if(part == null) {
					// try to get this part from the global pool
					AbstractPart p = pool.get(id);
					if(p == null) {
						throw new IOException("part not found: " + id);
					}
					if(p.getClass().equals(clazz)) {
						part = (T) p;
					} else {
						throw new IOException("invalid part reference");
					}
				}
				PartContainer<T> container = new PartContainer<>(this, part, time);
				part.addReference(container);
				container.read(child);
				parts.put(time, container);
				break;
			}
			}
		}

		fireEvent(TrackListener.PART);
	}

	public void addPartsToPool(PartPool pool) {
		for(PartContainer<T> container : parts.sequencedValues()) {
			T part = container.getPart();
			pool.add(part, type);
		}
	}

	private void writeParts(Element xml, PartPool pool) {
		Map<T, Integer> uniqueParts = new HashMap<>();

		int id = pool.size();
		for(PartContainer<T> container : parts.sequencedValues()) {
			T part = container.getPart();
			if(!pool.contains(part) && !uniqueParts.containsKey(part)) {
				uniqueParts.put(part, id++);
			}
		}

		for(Entry<T, Integer> entry : uniqueParts.entrySet()) {
			T part = entry.getKey();
			Element xmlPart = new Element("part");
			xmlPart.addAttribute("id", Integer.toString(entry.getValue()));
			if(part.getName() != null) {
				xmlPart.addAttribute("name", part.getName());
			}
			part.write(xmlPart);
			xml.addChild(xmlPart);
		}

		for(PartContainer<T> container : parts.sequencedValues()) {
			T part = container.getPart();
			int partId;
			if(pool.contains(part)) {
				partId = pool.getId(part);
			} else {
				partId = uniqueParts.get(part);
			}

			Element xmlContainer = new Element("container");
			xmlContainer.addAttribute("part", Integer.toString(partId));
			xmlContainer.addAttribute("time", Long.toString(container.getTime()));
			container.write(xmlContainer);
			xml.addChild(xmlContainer);
		}
	}

	public void read(Element xml, Map<Integer, AbstractPart> pool) throws IOException {
		if(!xml.name.equals("track")) {
			throw new IOException("not a track");
		}

		setName(xml.getAttribute("name"));
		setColor(Integer.parseInt(xml.getAttribute("color")));
		setMuted(Boolean.parseBoolean(xml.getAttribute("muted")));
		setSolo(Boolean.parseBoolean(xml.getAttribute("solo")));
		setRecordingArmed(Boolean.parseBoolean(xml.getAttribute("recording")));
		setMonitoring(Boolean.parseBoolean(xml.getAttribute("monitoring")));
		setEnabled(Boolean.parseBoolean(xml.getAttribute("enabled")));
		setLocked(Boolean.parseBoolean(xml.getAttribute("locked")));
		setVolume(Double.parseDouble(xml.getAttribute("volume")));
		readTrack(xml);
		readParts(xml, pool);
	}

	public Element write(PartPool pool) {
		Element xml = new Element("track");
		xml.addAttribute("type", Track.TRACK_TYPES[getType()]);
		xml.addAttribute("name", getName());
		xml.addAttribute("color", Integer.toString(getColor()));
		xml.addAttribute("muted", Boolean.toString(muted));
		xml.addAttribute("solo", Boolean.toString(solo));
		xml.addAttribute("recording", Boolean.toString(recording));
		xml.addAttribute("monitoring", Boolean.toString(monitoring));
		xml.addAttribute("enabled", Boolean.toString(enabled));
		xml.addAttribute("locked", Boolean.toString(locked));
		xml.addAttribute("volume", Double.toString(volume));
		writeTrack(xml);
		writeParts(xml, pool);
		return xml;
	}

	@Override
	public abstract Track<T> clone();
}
