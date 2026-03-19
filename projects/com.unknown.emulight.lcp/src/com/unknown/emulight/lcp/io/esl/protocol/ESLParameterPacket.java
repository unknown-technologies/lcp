package com.unknown.emulight.lcp.io.esl.protocol;

import com.unknown.util.io.Endianess;

public class ESLParameterPacket extends ESLPacket {
	private byte channel;
	private byte parameter;
	private int value;

	ESLParameterPacket(byte address) {
		super(address);
	}

	public ESLParameterPacket(byte address, byte channel, byte parameter, int value) {
		super(address);
		this.channel = channel;
		this.parameter = parameter;
		this.value = value;
	}

	@Override
	public byte getProtocol() {
		return ESL_PROTO_PARAMETER;
	}

	@Override
	protected int getPayloadSize() {
		return 6;
	}

	@Override
	protected void writePayload(byte[] buf, int offset) {
		buf[offset] = channel;
		buf[offset + 1] = parameter;
		Endianess.set32bitLE(buf, offset + 2, value);
	}

	@Override
	protected void readPayload(byte[] buf, int offset) {
		channel = buf[offset];
		parameter = buf[offset + 1];
		value = Endianess.get32bitLE(buf, offset + 2);
	}
}
