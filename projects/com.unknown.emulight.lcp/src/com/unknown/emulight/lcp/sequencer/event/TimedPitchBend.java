package com.unknown.emulight.lcp.sequencer.event;

import com.unknown.emulight.lcp.sequencer.MidiTrack;
import com.unknown.emulight.lcp.sequencer.PitchBend;

public class TimedPitchBend extends TimedEvent<MidiTrack> {
	private final PitchBend bend;

	public TimedPitchBend(MidiTrack track, long time, PitchBend bend) {
		super(track, time);
		this.bend = bend;
	}

	@Override
	public void transmit() {
		track.pitchBend(bend);
	}

	@Override
	public String toString() {
		return "TimedPitchBend[time=" + getTime() + ",bend=" + bend + "]";
	}
}
