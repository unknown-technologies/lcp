package com.unknown.emulight.lcp.io.esl.protocol;

public class ESLMidiPacket extends ESLPacket {
	private byte channel;
	private byte status;
	private byte data1;
	private byte data2;

	protected ESLMidiPacket(byte address) {
		super(address);
	}

	public ESLMidiPacket(byte address, byte channel, byte status, byte data1) {
		this(address, channel, status, data1, (byte) 0);
	}

	public ESLMidiPacket(byte address, byte channel, byte status, byte data1, byte data2) {
		super(address);
		this.channel = channel;
		this.status = status;
		this.data1 = data1;
		this.data2 = data2;
	}

	@Override
	protected int getPayloadSize() {
		return 4;
	}

	@Override
	public byte getProtocol() {
		return ESL_PROTO_MIDI;
	}

	@Override
	protected void writePayload(byte[] buf, int offset) {
		buf[offset] = channel;
		buf[offset + 1] = status;
		buf[offset + 2] = data1;
		buf[offset + 3] = data2;
	}

	@Override
	protected void readPayload(byte[] buf, int offset) {
		channel = buf[offset];
		status = buf[offset + 1];
		data1 = buf[offset + 2];
		data2 = buf[offset + 3];
	}
}
