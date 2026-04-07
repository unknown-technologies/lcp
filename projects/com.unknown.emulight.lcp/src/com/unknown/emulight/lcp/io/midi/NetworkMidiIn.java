package com.unknown.emulight.lcp.io.midi;

import java.net.SocketException;
import java.util.logging.Logger;

import javax.sound.midi.MidiDevice.Info;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

import com.unknown.emulight.lcp.project.SystemConfiguration.MidiPortConfig;
import com.unknown.emulight.lcp.project.SystemConfiguration.NetworkMidiPortConfig;
import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;

public class NetworkMidiIn extends MidiInPort {
	private static final Logger log = Trace.create(NetworkMidiIn.class);

	private NetworkMidiTransmitter in;
	private NetworkMidiPortConfig cfg;
	private int port;

	public NetworkMidiIn(NetworkMidiPortConfig cfg, MidiRouter router) {
		super(router);
		this.cfg = cfg;
		loadConfig();
	}

	@Override
	public MidiPortConfig getConfig() {
		return cfg;
	}

	private void loadConfig() {
		setPort(cfg.getPort());
		if(isActive()) {
			try {
				openDevice();
			} catch(MidiUnavailableException e) {
				log.log(Levels.WARNING, "Failed to open network MIDI device: " + e.getMessage(), e);
			}
		}
	}

	public void setPort(int port) {
		cfg.setPort(port);

		boolean restart = in != null;
		if(restart) {
			closeDevice();
		}
		this.port = port;
		if(restart) {
			try {
				openDevice();
			} catch(MidiUnavailableException e) {
				log.log(Levels.ERROR, e.getMessage());
			}
		}
	}

	public void delete() {
		router.delete(this);
	}

	@Override
	public Info getInfo() {
		String name = getName();
		return new Info(name, "unknown technologies", "MIDI UDP receiver", "1.0") {
		};
	}

	@Override
	public void openDevice() throws MidiUnavailableException {
		try {
			in = new NetworkMidiTransmitter(port);
			in.setReceiver(new Receiver() {
				@Override
				public void send(MidiMessage message, long timestamp) {
					NetworkMidiIn.this.send(message);
				}

				@Override
				public void close() {
					// nothing
				}
			});
		} catch(SocketException e) {
			throw new MidiUnavailableException("Failed to open network MIDI port: " + e.getMessage());
		}
	}

	@Override
	public void closeDevice() {
		if(in != null) {
			in.close();
			in = null;
		}
	}

	@Override
	public String getName() {
		return Integer.toString(port);
	}

	private void send(MidiMessage message) {
		if(message instanceof ShortMessage) {
			ShortMessage msg = (ShortMessage) message;
			router.send(this, msg);
		}
	}
}
