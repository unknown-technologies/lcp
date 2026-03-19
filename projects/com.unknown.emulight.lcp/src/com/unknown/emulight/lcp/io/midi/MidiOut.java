package com.unknown.emulight.lcp.io.midi;

import java.util.logging.Logger;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDevice.Info;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;

import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;

public class MidiOut extends MidiOutPort {
	private static final Logger log = Trace.create(MidiOut.class);

	private final int id;
	private final Info info;
	private MidiDevice dev;
	private Receiver recv;
	private boolean clock;

	public MidiOut(int id, Info info, MidiRouter router) {
		super(router);
		this.id = id;
		this.info = info;
	}

	public void openDevice() throws MidiUnavailableException {
		if(dev != null) {
			return;
		}

		log.info("Opening MIDI device " + info.getName());

		dev = MidiSystem.getMidiDevice(info);
		dev.open();
		recv = dev.getReceiver();
	}

	public void closeDevice() {
		if(dev != null && dev.isOpen()) {
			log.info("Closing MIDI device " + info.getName());

			dev.close();
		}

		recv = null;
		dev = null;
	}

	public int getId() {
		return id;
	}

	public Info getInfo() {
		return info;
	}

	@Override
	public String getName() {
		return info.getName();
	}

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

	public void setClock(boolean clock) {
		this.clock = clock;
	}

	public boolean isClock() {
		return clock;
	}

	@Override
	public void transmit(MidiMessage message, long timestamp) {
		if(dev != null && dev.isOpen()) {
			recv.send(message, timestamp);
		}
	}
}
