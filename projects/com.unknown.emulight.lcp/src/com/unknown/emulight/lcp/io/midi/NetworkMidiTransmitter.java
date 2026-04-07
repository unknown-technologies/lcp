package com.unknown.emulight.lcp.io.midi;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Transmitter;

import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;

public class NetworkMidiTransmitter implements Transmitter {
	private static final Logger log = Trace.create(NetworkMidiTransmitter.class);

	protected DatagramSocket socket;
	private Thread thread;
	private Receiver receiver;

	public NetworkMidiTransmitter(int port) throws SocketException {
		this(new DatagramSocket(port));
	}

	protected NetworkMidiTransmitter(DatagramSocket socket) {
		this.socket = socket;
		thread = new Thread(this::rxloop);
		thread.start();
	}

	@Override
	public void close() {
		if(thread != null) {
			thread.interrupt();
			if(thread.isAlive()) {
				try {
					thread.join();
				} catch(InterruptedException e) {
					// set the interrupted flag again
					Thread.currentThread().interrupt();
				}
			}
			thread = null;
		}
		if(receiver != null) {
			receiver.close();
			receiver = null;
		}
		if(socket != null) {
			socket.close();
			socket = null;
		}
	}

	@Override
	public Receiver getReceiver() {
		return receiver;
	}

	@Override
	public void setReceiver(Receiver receiver) {
		this.receiver = receiver;
	}

	private void rxloop() {
		while(!thread.isInterrupted() && socket != null) {
			try {
				receive();
			} catch(SocketException e) {
				if(socket != null && !socket.isClosed()) {
					log.log(Levels.ERROR, "MIDI receive thread failed: " + e.getMessage(), e);
				}
			} catch(Throwable t) {
				log.log(Levels.ERROR, "MIDI receive thread failed: " + t.getMessage(), t);
			}
		}
	}

	private void receive() throws IOException {
		byte[] buf = new byte[65536]; // maximum UDP packet size
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		socket.receive(packet);
		handle(packet);
	}

	private void handle(DatagramPacket packet) {
		byte[] data = packet.getData();
		int length = packet.getLength();
		byte[] midi = Arrays.copyOf(data, length);
		try {
			if(receiver != null) {
				MidiMessage msg = null;
				if(midi[0] == (byte) SysexMessage.SYSTEM_EXCLUSIVE) {
					msg = new SysexMessage(midi, midi.length);
				} else if(midi.length <= 3) {
					msg = new BinaryShortMessage(midi);
				}
				receiver.send(msg, -1);
			}
		} catch(InvalidMidiDataException e) {
			log.log(Levels.ERROR, "Invalid MIDI data received: " + e.getMessage());
		} catch(Throwable t) {
			log.log(Levels.ERROR, "Failed to process MIDI packet: " + t.getMessage(), t);
		}
	}
}
