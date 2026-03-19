package com.unknown.emulight.lcp.sequencer;

public class TempoChange {
	private final long time;
	private final double tempo;

	public TempoChange(long time, double tempo) {
		this.time = time;
		this.tempo = tempo;
	}

	public long getTime() {
		return time;
	}

	public double getTempo() {
		return tempo;
	}

	@Override
	public String toString() {
		return "TempoChange[t=" + time + ",bpm=" + tempo + "]";
	}
}
