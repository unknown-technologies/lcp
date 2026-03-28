package com.unknown.emulight.lcp.io.midi;

import javax.sound.midi.ShortMessage;

public class BinaryShortMessage extends ShortMessage {
	public BinaryShortMessage(byte[] data) {
		super(data);
	}
}
