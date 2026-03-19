package com.unknown.emulight.lcp.event;

public interface SequencerListener {
	void playbackStarted();

	void playbackStopped();

	void positionChanged(long tick);
}
