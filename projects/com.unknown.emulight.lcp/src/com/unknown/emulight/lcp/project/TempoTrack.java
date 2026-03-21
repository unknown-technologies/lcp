package com.unknown.emulight.lcp.project;

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import com.unknown.audio.midi.smf.TempoEvent;
import com.unknown.emulight.lcp.sequencer.TempoChange;
import com.unknown.emulight.lcp.sequencer.TempoPart;
import com.unknown.xml.dom.Element;

public class TempoTrack extends Track<TempoPart> {
	private NavigableMap<Long, TempoCheckpoint> tempoCheckpoints = new TreeMap<>();

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

	public void recompute() {
		long ppq = getProject().getPPQ();
		double initialBpm = getTempo(0);
		int initialMicroTempo = (int) Math.round(60_000_000 / initialBpm);
		TempoCheckpoint last = new TempoCheckpoint(0, 0, initialMicroTempo, ppq);
		tempoCheckpoints.clear();
		tempoCheckpoints.put(0L, last);
		for(PartContainer<TempoPart> part : getParts()) {
			long t = part.getTime();
			for(TempoChange change : part.getPart().getTempoChanges()) {
				if(!part.containsEvent(change.getTime())) {
					continue;
				}

				double bpm = change.getTempo();
				int micro = TempoEvent.getMicroTempo(bpm);
				long time = t + change.getTime();

				long dtick = time - last.getTick();
				long nanotime = last.getTime() + dtick * last.getMicroTempo() * 1000 / ppq;
				last = new TempoCheckpoint(time, nanotime, micro, ppq);
				tempoCheckpoints.put(time, last);
			}
		}
	}

	public NavigableMap<Long, TempoCheckpoint> getTempoCheckpoints() {
		return tempoCheckpoints;
	}

	public long getTime(long tick) {
		Entry<Long, TempoCheckpoint> entry = tempoCheckpoints.floorEntry(tick);
		if(entry == null) {
			long ppq = getProject().getPPQ();
			double initialBpm = getTempo(0);
			int initialMicroTempo = (int) Math.round(60_000_000 / initialBpm);
			TempoCheckpoint checkpoint = new TempoCheckpoint(0, 0, initialMicroTempo, ppq);
			return checkpoint.getTime(tick);
		} else {
			TempoCheckpoint checkpoint = entry.getValue();
			return checkpoint.getTime(tick);
		}
	}

	@Override
	public PartContainer<TempoPart> addPart(long time, TempoPart part) {
		PartContainer<TempoPart> container = super.addPart(time, part);
		recompute();
		return container;
	}

	@Override
	public void removePart(PartContainer<TempoPart> part) {
		super.removePart(part);
		recompute();
	}

	@Override
	public PartContainer<TempoPart> movePart(long time, PartContainer<TempoPart> container) {
		PartContainer<TempoPart> result = super.movePart(time, container);
		recompute();
		return result;
	}

	@Override
	public PartContainer<TempoPart> clonePart(long time, PartContainer<TempoPart> container) {
		PartContainer<TempoPart> result = super.clonePart(time, container);
		recompute();
		return result;
	}

	@Override
	public PartContainer<TempoPart> linkPart(long time, PartContainer<TempoPart> container) {
		PartContainer<TempoPart> result = super.linkPart(time, container);
		recompute();
		return result;
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

	public static class TempoCheckpoint {
		private final long ppq;
		private final long tick;
		private final long nanotime;
		private final int microTempo;

		public TempoCheckpoint(long tick, long nanotime, int microTempo, long ppq) {
			this.tick = tick;
			this.nanotime = nanotime;
			this.microTempo = microTempo;
			this.ppq = ppq;
		}

		public long getTick() {
			return tick;
		}

		public long getTick(long time) {
			long dtime = time - nanotime;
			long dtick = dtime * ppq / microTempo / 1000;
			return tick + dtick;
		}

		public long getTime() {
			return nanotime;
		}

		public long getTime(long t) {
			long dtick = t - tick;
			long dtime = dtick * microTempo * 1000 / ppq;
			return nanotime + dtime;
		}

		public int getMicroTempo() {
			return microTempo;
		}
	}
}
