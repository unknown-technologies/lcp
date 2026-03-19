package com.unknown.emulight.lcp.io.esl;

import java.io.Closeable;
import java.io.IOException;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDevice.Info;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

public class PCIF implements Receiver, Closeable {
	private SerialInterface rs232;
	private ESL esl;

	private MidiDevice midi;

	public PCIF(String device) throws IOException {
		rs232 = new SerialInterface(device);
		rs232.open();

		esl = new ESLInterface(rs232);

		synchronizeTime();
	}

	@Override
	public void close() {
		if(rs232 != null) {
			try {
				rs232.close();
			} catch(IOException e) {
				// nothing
			}
			rs232 = null;
		}
	}

	public void openMIDI(Info device) throws MidiUnavailableException {
		midi = MidiSystem.getMidiDevice(device);
		midi.open();
		midi.getTransmitter().setReceiver(this);
	}

	public void closeMIDI() {
		if(midi != null) {
			midi.close();
			midi = null;
		}
	}

	public void addESLListener(ESLListener listener) {
		esl.addESLListener(listener);
	}

	public void removeESLListener(ESLListener listener) {
		esl.removeESLListener(listener);
	}

	public void synchronizeTime() throws IOException {
		esl.synchronizeTime();
	}

	public void enumerateDevices() throws IOException {
		esl.enumerateDevices();
	}

	@Override
	public void send(MidiMessage message, long timeStamp) {
		if(message instanceof ShortMessage) {
			ShortMessage msg = (ShortMessage) message;
			try {
				esl.sendMIDI(1, (byte) 0, (byte) msg.getStatus(), (byte) msg.getData1(),
						(byte) msg.getData2());
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
}
