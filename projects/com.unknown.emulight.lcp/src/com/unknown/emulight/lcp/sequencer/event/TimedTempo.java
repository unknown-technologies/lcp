package com.unknown.emulight.lcp.sequencer.event;

import com.unknown.audio.midi.smf.TempoEvent;
import com.unknown.emulight.lcp.sequencer.MidiTrack;

public class TimedTempo extends TimedEvent {
	private final int microTempo;

	public TimedTempo(MidiTrack track, long time, int microTempo) {
		super(track, time);
		this.microTempo = microTempo;
	}

	public int getMicroTempo() {
		return microTempo;
	}

	@Override
	public void transmit() {
		// empty
	}

	@Override
	public String toString() {
		return "TimedTempo[time=" + getTime() + ",bpm=" + TempoEvent.getBPM(microTempo) + "]";
	}
}
