package com.unknown.emulight.lcp.project;

import java.io.IOException;
import java.util.List;

import com.unknown.emulight.lcp.sequencer.TempoPart;
import com.unknown.xml.dom.Element;

public class TempoTrack extends Track<TempoPart> {
	public TempoTrack(Project project, String name) {
		super(TEMPO, project, name);

		TempoPart part = new TempoPart();
		addPart(0, part);
	}

	public double getTempo(long time) {
		List<PartContainer<TempoPart>> parts = getParts();

		if(parts.isEmpty()) {
			return 120;
		} else if(parts.get(0).getStart() > time) {
			return parts.get(0).getPart().getFirstTempo();
		}

		double lastTempo = 120;

		for(PartContainer<TempoPart> part : getParts()) {
			TempoPart tempoPart = part.getPart();
			if(part.contains(time)) {
				double tempo = tempoPart.getTempo(time - part.getTime());
				if(tempo == 0) {
					return lastTempo;
				} else {
					return tempo;
				}
			} else {
				lastTempo = tempoPart.getLastTempo();
			}
		}

		return lastTempo;
	}

	public void setTempo(long time, double tempo) {
		List<PartContainer<TempoPart>> parts = getParts();

		for(PartContainer<TempoPart> part : parts) {
			TempoPart tempoPart = part.getPart();
			if(part.contains(time)) {
				tempoPart.setTempo(time - part.getTime(), tempo);
				return;
			}
		}

		if(parts.isEmpty()) {
			// no tempo part at all, add a new one
			TempoPart part = new TempoPart();
			part.setTempo(time, tempo);
			addPart(0, part);
		} else if(parts.get(parts.size() - 1).getEnd() <= time) {
			// after the last part
			PartContainer<TempoPart> lastPart = parts.get(parts.size() - 1);
			TempoPart part = lastPart.getPart();

			long oldLength = lastPart.getLength();
			long oldPartLength = part.getLength();

			part.setTempo(time - lastPart.getTime(), tempo);

			// extend the part's size
			long newPartLength = part.getLength();
			long delta = newPartLength - oldPartLength;
			long newLength = oldLength + delta;
			lastPart.setLength(newLength);
		}
	}

	@Override
	protected TempoPart createPart() {
		return new TempoPart();
	}

	@Override
	protected void readTrack(Element xml) throws IOException {
		// TODO
	}

	@Override
	protected void writeTrack(Element xml) {
		// TODO
	}

	@Override
	public TempoTrack clone() {
		TempoTrack track = new TempoTrack(getProject(), getName());
		copy(track);
		return track;
	}
}
