package com.unknown.emulight.lcp.io.midi;

import java.io.IOException;
import java.util.logging.Logger;

import javax.sound.midi.MidiDevice.Info;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

import com.unknown.emulight.lcp.io.esl.ESL;
import com.unknown.emulight.lcp.project.SystemConfiguration.ESLMidiPortConfig;
import com.unknown.emulight.lcp.project.SystemConfiguration.MidiPortConfig;
import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;

public class ESLMidiOut extends MidiOutPort {
	private static final Logger log = Trace.create(ESLMidiOut.class);

	private final ESL esl;

	private final ESLMidiPortConfig cfg;

	public ESLMidiOut(ESL esl, ESLMidiPortConfig cfg, MidiRouter router) {
		super(router);
		this.esl = esl;
		this.cfg = cfg;
	}

	public int getAddress() {
		return cfg.getAddress();
	}

	public void setAddress(int address) {
		cfg.setAddress(address);
	}

	public int getPort() {
		return cfg.getPort();
	}

	public void setPort(int port) {
		cfg.setPort(port);
	}

	public void delete() {
		router.delete(this);
	}

	@Override
	public String getName() {
		return cfg.getAlias();
	}

	@Override
	public Info getInfo() {
		return new Info(getName(), "unknown technologies", "ESL MIDI output port", "1.0") {
		};
	}

	@Override
	public MidiPortConfig getConfig() {
		return cfg;
	}

	@Override
	public void openDevice() {
		// nothing
	}

	@Override
	public void closeDevice() {
		// nothing
	}

	@Override
	public void transmit(MidiMessage message, long timestamp) {
		if(message instanceof ShortMessage) {
			ShortMessage msg = (ShortMessage) message;
			byte status = (byte) msg.getStatus();
			byte data1 = (byte) msg.getData1();
			byte data2 = (byte) msg.getData2();

			int addr = cfg.getAddress();
			byte port = (byte) cfg.getPort();

			try {
				esl.sendMIDI(addr, port, status, data1, data2);
			} catch(IOException e) {
				log.log(Levels.ERROR, "Failed to transmit MIDI packet", e);
			}
		}
	}
}
