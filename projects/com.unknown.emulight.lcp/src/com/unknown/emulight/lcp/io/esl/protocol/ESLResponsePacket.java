package com.unknown.emulight.lcp.io.esl.protocol;

import com.unknown.util.HexFormatter;

/*
 * typedef struct {
 *        u8              request;
 *        u8              id;
 *        u8              pad;
 *        u8              length;
 *        u8              data[96];
 * } ESLResponse;
 */
public class ESLResponsePacket extends ESLSystemPacket {
	private byte request;
	private byte id;
	private byte[] data;

	ESLResponsePacket(byte addr) {
		super(addr);
	}

	public ESLResponsePacket(byte dest, byte channel, int source, int destination, byte request, byte id,
			byte[] data) {
		super(dest, channel, ESL_SYSTEM_CMD_RESPONSE, source, destination);
		this.request = request;
		this.id = id;
		this.data = data;
	}

	public ESLResponsePacket(byte channel, int source, int destination, byte request, byte id, byte[] data) {
		super(channel, ESL_SYSTEM_CMD_RESPONSE, source, destination);
		this.request = request;
		this.id = id;
		this.data = data;
	}

	@Override
	protected int getDataSize() {
		return data.length + 4;
	}

	@Override
	protected void writeData(byte[] buf, int offset) {
		buf[offset + 0] = request;
		buf[offset + 1] = id;
		buf[offset + 3] = (byte) data.length;
		System.arraycopy(buf, offset + 4, data, 0, data.length);
	}

	@Override
	protected void readData(byte[] buf, int offset) {
		request = buf[offset + 0];
		id = buf[offset + 1];
		int len = Byte.toUnsignedInt(buf[offset + 3]);
		data = new byte[len];
		System.arraycopy(buf, offset + 4, data, 0, len);
	}

	public byte getRequest() {
		return request;
	}

	public byte getId() {
		return id;
	}

	public byte[] getData() {
		return data;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder("ESLResponsePacket[id=");
		buf.append(Byte.toUnsignedInt(id));
		buf.append(",data=[");
		boolean first = true;
		for(byte b : data) {
			if(!first) {
				buf.append(' ');
			} else {
				first = false;
			}
			buf.append(HexFormatter.tohex(Byte.toUnsignedInt(b), 2));
		}
		return buf.append("]]").toString();
	}
}
