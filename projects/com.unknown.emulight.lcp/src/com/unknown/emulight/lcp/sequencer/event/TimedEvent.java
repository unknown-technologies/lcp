package com.unknown.emulight.lcp.sequencer.event;

import com.unknown.emulight.lcp.sequencer.MidiTrack;

public abstract class TimedEvent {
	protected final MidiTrack track;
	protected final long time;

	protected TimedEvent(MidiTrack track, long time) {
		this.track = track;
		this.time = time;
	}

	public long getTime() {
		return time;
	}

	public MidiTrack getTrack() {
		return track;
	}

	public abstract void transmit();
}
