package com.unknown.emulight.lcp.sequencer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import com.unknown.audio.midi.smf.Event;
import com.unknown.audio.midi.smf.MIDIEvent;
import com.unknown.audio.midi.smf.MTrk;
import com.unknown.audio.midi.smf.SMF;
import com.unknown.emulight.lcp.project.AbstractPart;
import com.unknown.xml.dom.Element;

public class MidiPart extends AbstractPart {
	private List<Note> notes = new ArrayList<>();
	private List<PitchBend> pitchBends = new ArrayList<>();

	private int ppq;

	public MidiPart() {
		this(96);
	}

	public MidiPart(int ppq) {
		this.ppq = ppq;
	}

	public MidiPart(SMF smf) {
		this(smf.getHeader().getPPQ());

		for(MTrk track : smf.getTracks()) {
			loadTrack(track, ppq, ppq);
		}
	}

	public void loadAllTracks(SMF smf) {
		int smfPPQ = smf.getHeader().getPPQ();

		for(MTrk track : smf.getTracks()) {
			loadTrack(track, smfPPQ, ppq);
		}
	}

	public void loadAllTracks(SMF smf, int projectPPQ) {
		int smfPPQ = smf.getHeader().getPPQ();

		for(MTrk track : smf.getTracks()) {
			loadTrack(track, smfPPQ, projectPPQ);
		}
	}

	public void loadTrack(MTrk track, int smfPPQ) {
		loadTrack(track, smfPPQ, ppq);
	}

	public void loadTrack(MTrk track, int smfPPQ, int projectPPQ) {
		this.ppq = projectPPQ;

		@SuppressWarnings("unchecked")
		Deque<Note>[][] keymap = new LinkedList[16][128];

		for(int i = 0; i < keymap.length; i++) {
			for(int j = 0; j < keymap[i].length; j++) {
				keymap[i][j] = new LinkedList<>();
			}
		}

		int key = 0;

		for(Event evt : track.getEvents()) {
			if(evt instanceof MIDIEvent) {
				MIDIEvent midi = (MIDIEvent) evt;
				int ch = midi.getChannel();

				long tick = midi.getTime() * projectPPQ / smfPPQ;

				switch(midi.getCommand()) {
				case MIDIEvent.NOTE_ON:
					key = midi.getData1();

					if(midi.getData2() == 0) {
						// this is a NOTE_OFF
						if(keymap[ch][key].size() > 0) {
							Note n = keymap[ch][key].removeFirst();
							int length = (int) (tick - n.getTime());
							notes.add(new Note(n.getTime(), ch, key, n.getVelocity(),
									length));
						}
					} else {
						keymap[ch][key].add(new Note(tick, ch, key, midi.getData2(), 0));
					}
					break;
				case MIDIEvent.NOTE_OFF:
					key = midi.getData1();

					if(keymap[ch][key].size() > 0) {
						Note n = keymap[ch][key].removeFirst();
						int length = (int) (tick - n.getTime());
						notes.add(new Note(n.getTime(), ch, key, n.getVelocity(),
								midi.getData2(), length));
					}
					break;
				case MIDIEvent.CTRL_CHANGE:
					switch(midi.getData1()) {
					case MIDIEvent.CC_ALL_NOTE_OFF:
					case MIDIEvent.CC_ALL_SND_OFF:
						for(Deque<Note> queue : keymap[ch]) {
							for(Note n : queue) {
								int length = (int) (tick - n.getTime());
								notes.add(new Note(n.getTime(), ch, key,
										n.getVelocity(), length));
							}
							queue.clear();
						}
						break;
					}
					break;
				case MIDIEvent.PITCH_BEND:
					pitchBends.add(new PitchBend(tick, ch, midi.getBend()));
					break;
				}
			}
		}

		// end all "open" notes at the end of the track
		if(!track.getEvents().isEmpty()) {
			long time = track.getEvents().get(track.getEvents().size() - 1).getTime();
			for(int ch = 0; ch < keymap.length; ch++) {
				for(Deque<Note> queue : keymap[ch]) {
					for(Note n : queue) {
						int length = (int) (time - n.getTime());
						notes.add(new Note(n.getTime(), ch, key, n.getVelocity(), length));
					}
					queue.clear();
				}
			}
		}
	}

	public void clear() {
		notes.clear();
	}

	public int getPPQ() {
		return ppq;
	}

	public void addNote(Note note) {
		int index = Collections.binarySearch(notes, note, MidiPart::compareNote);
		if(index >= 0) {
			notes.add(index, note);
		} else {
			notes.add(~index, note);
		}
	}

	public void removeNote(Note note) {
		notes.remove(note);
	}

	public List<Note> getNotes() {
		return Collections.unmodifiableList(notes);
	}

	public void addPitchBend(PitchBend bend) {
		int index = Collections.binarySearch(pitchBends, bend, MidiPart::compareBend);
		if(index >= 0) {
			pitchBends.add(index, bend);
		} else {
			pitchBends.add(~index, bend);
		}
	}

	public void removePitchBend(PitchBend bend) {
		pitchBends.remove(bend);
	}

	public List<PitchBend> getPitchBends() {
		return Collections.unmodifiableList(pitchBends);
	}

	public int getSize() {
		return notes.size();
	}

	public int getChannel() {
		if(notes.isEmpty() && pitchBends.isEmpty()) {
			return -1;
		}

		int channel = !notes.isEmpty() ? notes.get(0).getChannel() : pitchBends.get(0).getChannel();

		for(Note note : notes) {
			if(note.getChannel() != channel) {
				return -1;
			}
		}

		for(PitchBend bend : pitchBends) {
			if(bend.getChannel() != channel) {
				return -1;
			}
		}

		return channel;
	}

	@Override
	public long getLength() {
		long last = 0;

		for(Note note : notes) {
			if(note.getTime() > last) {
				last = note.getTime();
			}
			if(note.getEnd() > last) {
				last = note.getEnd();
			}
		}

		for(PitchBend bend : pitchBends) {
			if(bend.getTime() > last) {
				last = bend.getTime();
			}
		}

		return last;
	}

	public long getFirstTick() {
		long firstTick = getLength();

		if(!notes.isEmpty()) {
			firstTick = notes.get(0).getTime();
		}

		if(!pitchBends.isEmpty()) {
			long firstBend = pitchBends.get(0).getTime();
			if(firstBend < firstTick) {
				firstTick = firstBend;
			}
		}

		return firstTick;
	}

	public void move(long delta) {
		for(Note note : notes) {
			note.setTime(note.getTime() + delta);
		}

		for(PitchBend bend : pitchBends) {
			bend.setTime(bend.getTime() + delta);
		}
	}

	public void sort() {
		Collections.sort(notes, MidiPart::compareNote);
	}

	private static int compareNote(Note a, Note b) {
		long timeA = a.getTime();
		long timeB = b.getTime();
		return Long.compareUnsigned(timeA, timeB);
	}

	private static int compareBend(PitchBend a, PitchBend b) {
		long timeA = a.getTime();
		long timeB = b.getTime();
		return Long.compareUnsigned(timeA, timeB);
	}

	public List<MIDIEvent> toMidi(long time) {
		return toMidi(time, -1);
	}

	public List<MIDIEvent> toMidi(long time, int channel) {
		List<MIDIEvent> events = new ArrayList<>();

		// add notes
		for(Note note : notes) {
			long start = time + note.getTime();
			long end = start + note.getLength();
			int ch = channel == MidiTrack.ANY ? note.getChannel() : channel;

			MIDIEvent noteOn = new MIDIEvent(start, (byte) (MIDIEvent.NOTE_ON | ch),
					(byte) note.getKey(), (byte) note.getVelocity());
			MIDIEvent noteOff = new MIDIEvent(end, (byte) (MIDIEvent.NOTE_OFF | ch),
					(byte) note.getKey(), (byte) note.getReleaseVelocity());

			events.add(noteOn);
			events.add(noteOff);
		}

		// add pitch bends
		for(PitchBend bend : pitchBends) {
			int ch = channel == MidiTrack.ANY ? bend.getChannel() : channel;
			int lsb = bend.getBend() & 0x7F;
			int msb = (bend.getBend() >> 7) & 0x7F;

			MIDIEvent pitchBend = new MIDIEvent(bend.getTime(), (byte) (MIDIEvent.PITCH_BEND | ch),
					(byte) lsb, (byte) msb);

			events.add(pitchBend);
		}

		// final sort
		Collections.sort(events, (a, b) -> Long.compareUnsigned(a.getTime(), b.getTime()));

		return events;
	}

	@Override
	public void read(Element xml) throws IOException {
		for(Element child : xml.getChildren()) {
			if(child.name.equals("note")) {
				Note note = Note.read(child);
				addNote(note);
			} else if(child.name.equals("pitch-bend")) {
				PitchBend bend = PitchBend.read(child);
				addPitchBend(bend);
			}
		}
	}

	@Override
	public void write(Element xml) {
		for(Note note : notes) {
			xml.addChild(note.write());
		}

		for(PitchBend bend : pitchBends) {
			xml.addChild(bend.write());
		}
	}

	@Override
	public MidiPart clone() {
		MidiPart part = new MidiPart(ppq);

		copy(part);

		for(Note note : notes) {
			part.addNote(note.clone());
		}

		for(PitchBend bend : pitchBends) {
			part.addPitchBend(bend.clone());
		}

		return part;
	}
}
