package com.unknown.emulight.lcp.io.midi;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

public abstract class MidiOutPort extends MidiPort {
	protected MidiOutPort(MidiRouter router) {
		super(router);
	}

	public abstract void transmit(MidiMessage message, long timestamp);

	public void transmit(long timestamp, int status) throws InvalidMidiDataException {
		transmit(new ShortMessage(status), timestamp);
	}

	public void transmit(long timestamp, int status, int data1) throws InvalidMidiDataException {
		transmit(timestamp, status, data1, 0);
	}

	public void transmit(long timestamp, int status, int data1, int data2) throws InvalidMidiDataException {
		transmit(new ShortMessage(status, data1, data2), timestamp);
	}
}
