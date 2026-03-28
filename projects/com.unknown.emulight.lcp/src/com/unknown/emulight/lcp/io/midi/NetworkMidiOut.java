package com.unknown.emulight.lcp.io.midi;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.logging.Logger;

import javax.sound.midi.MidiDevice.Info;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;

import com.unknown.emulight.lcp.project.SystemConfiguration.MidiPortConfig;
import com.unknown.emulight.lcp.project.SystemConfiguration.NetworkMidiPortConfig;
import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;

public class NetworkMidiOut extends MidiOutPort {
	private static final Logger log = Trace.create(NetworkMidiOut.class);

	private InetSocketAddress target;
	private NetworkMidiReceiver out;
	private NetworkMidiPortConfig cfg;

	public NetworkMidiOut(NetworkMidiPortConfig cfg, MidiRouter router) {
		super(router);
		this.cfg = cfg;
		loadConfig();
	}

	@Override
	public MidiPortConfig getConfig() {
		return cfg;
	}

	private void loadConfig() {
		String hostname = cfg.getAddress();
		int port = cfg.getPort();
		setTarget(new InetSocketAddress(hostname, port));
		if(isActive()) {
			try {
				openDevice();
			} catch(MidiUnavailableException e) {
				log.log(Levels.WARNING, "Failed to open network MIDI device: " + e.getMessage(), e);
			}
		}
	}

	public void setTarget(String hostname, int port) {
		setTarget(new InetSocketAddress(hostname, port));
	}

	public void setTarget(InetSocketAddress target) {
		cfg.setAddress(target.getHostString());
		cfg.setPort(target.getPort());

		boolean restart = out != null;
		if(restart) {
			closeDevice();
		}
		this.target = target;
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
	public void transmit(MidiMessage message, long timestamp) {
		if(out != null) {
			out.send(message, timestamp);
		}
	}

	@Override
	public Info getInfo() {
		String name = getName();
		return new Info(name, "unknown technologies", "MIDI UDP transmitter", "1.0") {
		};
	}

	@Override
	public void openDevice() throws MidiUnavailableException {
		if(target.isUnresolved()) {
			throw new MidiUnavailableException("Failed to open network MIDI port: unresolved address");
		}
		try {
			out = new NetworkMidiReceiver(target);
		} catch(SocketException e) {
			throw new MidiUnavailableException("Failed to open network MIDI port: " + e.getMessage());
		}
	}

	@Override
	public void closeDevice() {
		if(out != null) {
			out.close();
			out = null;
		}
	}

	@Override
	public String getName() {
		return target.getHostString() + ":" + target.getPort();
	}
}
