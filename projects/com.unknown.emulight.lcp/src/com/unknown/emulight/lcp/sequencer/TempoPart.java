package com.unknown.emulight.lcp.sequencer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import com.unknown.emulight.lcp.project.AbstractPart;
import com.unknown.xml.dom.Element;

public class TempoPart extends AbstractPart {
	private NavigableMap<Long, Double> tempo = new TreeMap<>();

	public TempoPart() {
		setTempo(0, 120);
	}

	public double getTempo(long time) {
		Entry<Long, Double> entry = tempo.floorEntry(time);
		if(entry == null) {
			return 120;
		} else {
			return entry.getValue();
		}
	}

	public List<TempoChange> getTempoChanges() {
		List<TempoChange> result = new ArrayList<>();

		for(Entry<Long, Double> change : tempo.entrySet()) {
			result.add(new TempoChange(change.getKey(), change.getValue()));
		}

		return result;
	}

	public NavigableMap<Long, Double> getTempoMap() {
		return Collections.unmodifiableNavigableMap(tempo);
	}

	public double getFirstTempo() {
		Entry<Long, Double> first = tempo.firstEntry();
		if(first == null) {
			return 0;
		} else {
			return first.getValue();
		}
	}

	public double getLastTempo() {
		Entry<Long, Double> last = tempo.lastEntry();
		if(last == null) {
			return 0;
		} else {
			return last.getValue();
		}
	}

	public void setTempo(long time, double tempo) {
		this.tempo.put(time, tempo);
	}

	public void deleteTempo(long time) {
		tempo.remove(time);
	}

	@Override
	public long getLength() {
		return tempo.lastKey() + 1;
	}

	@Override
	public void read(Element xml) throws IOException {
		tempo.clear();
		for(Element child : xml.getChildren()) {
			if(child.name.equals("tempo")) {
				long tick = Long.parseLong(child.getAttribute("time"));
				double bpm = Double.parseDouble(child.getAttribute("tempo"));
				setTempo(tick, bpm);
			}
		}
	}

	@Override
	public void write(Element xml) {
		for(Entry<Long, Double> entry : tempo.entrySet()) {
			Element xmlEntry = new Element("tempo");
			xmlEntry.addAttribute("time", Long.toString(entry.getKey()));
			xmlEntry.addAttribute("tempo", Double.toString(entry.getValue()));
			xml.addChild(xmlEntry);
		}
	}

	@Override
	public TempoPart clone() {
		TempoPart part = new TempoPart();
		copy(part);
		part.tempo.putAll(tempo);
		return part;
	}
}
