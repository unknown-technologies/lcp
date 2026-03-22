package com.unknown.emulight.lcp.sequencer.event;

import com.unknown.emulight.lcp.sequencer.MidiTrack;
import com.unknown.emulight.lcp.sequencer.Note;

public class TimedNoteOff extends TimedEvent<MidiTrack> {
	private final Note note;

	public TimedNoteOff(MidiTrack track, long time, Note note) {
		super(track, time);
		this.note = note;
	}

	@Override
	public void transmit() {
		track.noteOff(note);
	}

	@Override
	public String toString() {
		return "TimedNoteOff[time=" + getTime() + ",note=" + note + "]";
	}
}
