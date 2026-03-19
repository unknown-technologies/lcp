package com.unknown.emulight.lcp.io.esl.protocol;

import java.nio.charset.StandardCharsets;

import com.unknown.util.HexFormatter;

public class ESLLogMessagePacket extends ESLSystemPacket {
	private byte[] data;

	ESLLogMessagePacket(byte addr) {
		super(addr);
	}

	public ESLLogMessagePacket(byte dest, byte channel, int source, int destination, byte[] data) {
		super(dest, channel, ESL_SYSTEM_CMD_LOG_MESSAGE, source, destination);
		this.data = data;
	}

	public ESLLogMessagePacket(byte channel, int source, int destination, byte[] data) {
		super(channel, ESL_SYSTEM_CMD_LOG_MESSAGE, source, destination);
		this.data = data;
	}

	public String getMessage() {
		return new String(data, StandardCharsets.ISO_8859_1);
	}

	public byte[] getData() {
		return data;
	}

	@Override
	protected int getDataSize() {
		return data.length + 1;
	}

	@Override
	protected void writeData(byte[] buf, int offset) {
		buf[offset + 0] = (byte) data.length;
		System.arraycopy(buf, offset + 1, data, 0, data.length);
	}

	@Override
	protected void readData(byte[] buf, int offset) {
		int len = Byte.toUnsignedInt(buf[offset]);
		data = new byte[len];
		System.arraycopy(buf, offset + 1, data, 0, len);
	}

	@Override
	public String toString() {
		return "ESLLogMessage[src=" + HexFormatter.tohex(getSource()) + ";" + getMessage().trim() + "]";
	}
}
