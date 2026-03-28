package com.unknown.emulight.lcp.io.midi;

import java.util.logging.Logger;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice.Info;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.ShortMessage;

import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;

public abstract class MidiOutPort extends MidiPort {
	private static final Logger log = Trace.create(MidiOutPort.class);

	private boolean clock;

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

	public void setClock(boolean clock) {
		this.clock = clock;
	}

	public boolean isClock() {
		return clock;
	}

	public abstract Info getInfo();

	public abstract void openDevice() throws MidiUnavailableException;

	public abstract void closeDevice();

	@Override
	public void setActive(boolean active) {
		super.setActive(active);

		try {
			if(active) {
				openDevice();
			} else {
				closeDevice();
			}
		} catch(MidiUnavailableException e) {
			log.log(Levels.WARNING, "Failed to " + (active ? "open" : "close") + " MIDI device", e);
		}
	}
}
