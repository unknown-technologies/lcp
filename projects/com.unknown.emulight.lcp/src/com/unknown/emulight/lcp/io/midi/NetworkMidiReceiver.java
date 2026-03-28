package com.unknown.emulight.lcp.io.midi;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.logging.Logger;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;

import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;

public class NetworkMidiReceiver implements Receiver {
	private static final Logger log = Trace.create(NetworkMidiReceiver.class);

	private DatagramSocket[] sockets;
	private InetSocketAddress target;

	public NetworkMidiReceiver(InetSocketAddress target) throws SocketException {
		this.target = target;
		sockets = new DatagramSocket[] { new DatagramSocket() };
	}

	public NetworkMidiReceiver(InetSocketAddress target, InetAddress[] binds) throws SocketException {
		this.target = target;
		if(binds.length == 0) {
			sockets = new DatagramSocket[] { new DatagramSocket() };
		} else {
			sockets = new DatagramSocket[binds.length];
			for(int i = 0; i < binds.length; i++) {
				sockets[i] = new DatagramSocket(new InetSocketAddress(binds[i], 0));
			}
		}
	}

	public InetSocketAddress getTarget() {
		return target;
	}

	@Override
	public void close() {
		for(DatagramSocket socket : sockets) {
			socket.close();
		}
	}

	@Override
	public void send(MidiMessage message, long timeStamp) {
		byte[] data = message.getMessage();
		if(data == null) {
			return;
		}
		DatagramPacket packet = new DatagramPacket(data, data.length);
		packet.setSocketAddress(target);
		for(DatagramSocket socket : sockets) {
			try {
				socket.send(packet);
			} catch(Throwable t) {
				log.log(Levels.ERROR, "Failed to send MIDI packet: " + t.getMessage(), t);
			}
		}
	}
}
