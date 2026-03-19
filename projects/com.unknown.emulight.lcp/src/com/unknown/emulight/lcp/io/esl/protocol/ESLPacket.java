package com.unknown.emulight.lcp.io.esl.protocol;

import java.io.IOException;

public abstract class ESLPacket {
	public static final int ESL_ADDR_MASK = 0x3F;

	public static final byte ESL_PROTO_SYSTEM = 0;
	public static final byte ESL_PROTO_PARAMETER = 1;
	public static final byte ESL_PROTO_MIDI = 2;
	public static final byte ESL_PROTO_DATA = 3;

	private final byte address;

	protected ESLPacket(byte address) {
		this.address = address;
	}

	protected abstract int getPayloadSize();

	protected abstract void writePayload(byte[] buf, int offset);

	protected abstract void readPayload(byte[] buf, int offset);

	public abstract byte getProtocol();

	public final int getSize() {
		return getPayloadSize() + 2;
	}

	public final byte[] write() {
		byte[] buf = new byte[getSize()];
		write(buf, 0);
		return buf;
	}

	public final void write(byte[] buf, int offset) {
		buf[offset] = (byte) buf.length;
		buf[offset + 1] = (byte) (address | (getProtocol() << 6));
		writePayload(buf, offset + 2);
	}

	public final void read(byte[] data) {
		read(data, 0);
	}

	public final void read(byte[] data, int offset) {
		readPayload(data, offset + 2);
	}

	public static ESLPacket parse(byte[] data) throws IOException {
		if(data.length < 2) {
			throw new IOException("packet too short");
		}

		int len = Byte.toUnsignedInt(data[0]);
		if(len > data.length) {
			throw new IOException("truncated packet");
		}

		byte proto = (byte) (data[1] >>> 6);
		byte addr = (byte) (data[1] & ESL_ADDR_MASK);

		ESLPacket packet = null;
		switch(proto) {
		case ESL_PROTO_SYSTEM:
			packet = ESLSystemPacket.get(addr, data, 2);
			break;
		case ESL_PROTO_PARAMETER:
			packet = new ESLParameterPacket(addr);
			break;
		case ESL_PROTO_MIDI:
			packet = new ESLMidiPacket(addr);
			break;
		case ESL_PROTO_DATA:
		}

		if(packet != null) {
			packet.read(data);
		}

		return packet;
	}
}
