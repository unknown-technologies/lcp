package com.unknown.emulight.lcp.sequencer.event;

import com.unknown.emulight.lcp.project.Track;

public abstract class TimedEvent<T extends Track<?>> {
	protected final T track;
	protected final long time;

	protected TimedEvent(T track, long time) {
		this.track = track;
		this.time = time;
	}

	public long getTime() {
		return time;
	}

	public T getTrack() {
		return track;
	}

	public abstract void transmit();
}
