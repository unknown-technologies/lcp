package com.unknown.emulight.lcp.sequencer.event;

import com.unknown.emulight.lcp.sequencer.MidiTrack;
import com.unknown.emulight.lcp.sequencer.Note;

public class TimedNoteOn extends TimedEvent<MidiTrack> {
	private final Note note;

	public TimedNoteOn(MidiTrack track, long time, Note note) {
		super(track, time);
		this.note = note;
	}

	@Override
	public void transmit() {
		track.noteOn(note);
	}

	@Override
	public String toString() {
		return "TimedNoteOn[time=" + getTime() + ",note=" + note + "]";
	}
}
