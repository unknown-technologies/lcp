package com.unknown.emulight.lcp.live;

import java.io.IOException;

import com.unknown.emulight.lcp.project.AbstractPart;
import com.unknown.emulight.lcp.project.PartPool;
import com.unknown.emulight.lcp.project.Project;
import com.unknown.emulight.lcp.project.Track;
import com.unknown.xml.dom.Element;

public abstract class Cue<T extends AbstractPart> {
	public static final int MIDI = Track.MIDI;
	public static final int LASER = Track.LASER;
	public static final int DMX = Track.DMX;

	private final int type;
	protected final Project project;
	protected final T part;
	protected int length;

	private String name;
	private int color;

	protected Cue(int type, Project project, T part) {
		this.type = type;
		this.project = project;
		this.part = part;

		name = null;
		color = 0;
		length = 0;
	}

	public int getType() {
		return type;
	}

	public String getTypeName() {
		switch(type) {
		case MIDI:
			return "MIDI";
		case LASER:
			return "Laser";
		case DMX:
			return "DMX";
		default:
			return Track.TRACK_TYPES[type];
		}
	}

	public Project getProject() {
		return project;
	}

	public T getPart() {
		return part;
	}

	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;
	}

	public String getName() {
		if(name == null) {
			return part.getName();
		} else {
			return name;
		}
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public abstract void play(double bpm);

	public abstract void stop();

	public void read(Element xml) throws IOException {
		if(!xml.name.equals("cue")) {
			throw new IOException("not a cue");
		}

		length = Integer.parseInt(xml.getAttribute("length"));
		color = Integer.parseInt(xml.getAttribute("color"));
		name = xml.getAttribute("name");
	}

	public Element write(PartPool pool) {
		Element xml = new Element("cue");
		int id = pool.getId(part);
		xml.addAttribute("type", Track.TRACK_TYPES[type]);
		xml.addAttribute("part", Integer.toString(id));
		xml.addAttribute("length", Integer.toString(length));
		xml.addAttribute("color", Integer.toString(color));
		if(name != null) {
			xml.addAttribute("name", name);
		}
		return xml;
	}
}
