package com.unknown.emulight.lcp.live;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.unknown.emulight.lcp.laser.LaserCue;
import com.unknown.emulight.lcp.laser.LaserPart;
import com.unknown.emulight.lcp.project.AbstractPart;
import com.unknown.emulight.lcp.project.PartPool;
import com.unknown.emulight.lcp.project.Project;
import com.unknown.emulight.lcp.project.Track;
import com.unknown.xml.dom.Element;

public class CuePool {
	private final Project project;
	private final List<Cue<?>> cues = new ArrayList<>();
	private final CueMap map = new CueMap();

	public CuePool(Project project) {
		this.project = project;
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

	public void clear() {
		cues.clear();
		map.clear();
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
					cue.read(e);
					addCue(cue);
				}
			}
		}
	}

	public Element write(PartPool pool) {
		Element xml = new Element("cues");
		for(Cue<?> cue : cues) {
			xml.addChild(cue.write(pool));
		}
		return xml;
	}
}
