package com.unknown.emulight.lcp.io.esl.protocol;

public class ESLGrePacket extends ESLSystemPacket {
	private byte[] payload;

	ESLGrePacket(byte addr) {
		super(addr);
	}

	public ESLGrePacket(byte dest, byte channel, int source, int destination, byte[] payload) {
		super(dest, channel, ESL_SYSTEM_CMD_GRE, source, destination);
		this.payload = payload;
	}

	public ESLGrePacket(byte channel, int source, int destination, byte[] payload) {
		super(channel, ESL_SYSTEM_CMD_GRE, source, destination);
		this.payload = payload;
	}

	@Override
	protected int getDataSize() {
		return payload.length;
	}

	@Override
	protected void writeData(byte[] buf, int offset) {
		System.arraycopy(payload, 0, buf, offset, payload.length);
	}

	@Override
	protected void readData(byte[] buf, int offset) {
		int len = buf.length - offset;
		payload = new byte[len];
		System.arraycopy(buf, offset, payload, 0, len);
	}
}
