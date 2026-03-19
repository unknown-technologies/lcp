package com.unknown.emulight.lcp.io.midi;

public interface MidiReceiver {
	void receive(int status, int data1, int data2);
}
