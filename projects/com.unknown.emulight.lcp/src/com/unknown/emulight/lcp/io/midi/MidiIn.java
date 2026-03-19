package com.unknown.emulight.lcp.io.midi;

import java.util.logging.Logger;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDevice.Info;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;

public class MidiIn extends MidiInPort {
	private static final Logger log = Trace.create(MidiIn.class);

	private final int id;
	private final Info info;
	private MidiDevice dev;

	public MidiIn(int id, Info info, MidiRouter router) {
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
		dev.getTransmitter().setReceiver(new Receiver() {
			@Override
			public void send(MidiMessage message, long timestamp) {
				MidiIn.this.send(message);
			}

			@Override
			public void close() {
				// nothing
			}
		});
	}

	public void closeDevice() {
		if(dev != null && dev.isOpen()) {
			log.info("Closing MIDI device " + info.getName());

			dev.close();
		}

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

	private void send(MidiMessage message) {
		if(message instanceof ShortMessage) {
			ShortMessage msg = (ShortMessage) message;
			router.send(id, msg);
		}
	}
}
