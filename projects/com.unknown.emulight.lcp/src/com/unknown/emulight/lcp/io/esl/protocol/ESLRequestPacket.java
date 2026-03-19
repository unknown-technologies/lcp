package com.unknown.emulight.lcp.io.esl.protocol;

import com.unknown.util.io.Endianess;

/*
 * typedef struct {
 *        u8              request;
 *        u8              id;
 *        u16             param;
 * } ESLResponse;
 */
public class ESLRequestPacket extends ESLSystemPacket {
	public static final byte ESL_SYSTEM_REQ_DEVICE_COUNT = 0;
	public static final byte ESL_SYSTEM_REQ_DEVICE_INFO = 1;
	public static final byte ESL_SYSTEM_REQ_DEVICE_ID = 2;
	public static final byte ESL_SYSTEM_REQ_ROUTER = 3;
	public static final byte ESL_SYSTEM_REQ_STORAGE = 4;
	public static final byte ESL_SYSTEM_REQ_STORAGE_DEVICE = 5;
	public static final byte ESL_SYSTEM_REQ_PARAMETER_VALUE = 6;

	private byte request;
	private byte id;
	private short param;

	ESLRequestPacket(byte addr) {
		super(addr);
	}

	public ESLRequestPacket(byte dest, byte channel, int source, int destination, byte request, byte id,
			short param) {
		super(dest, channel, ESL_SYSTEM_CMD_REQUEST, source, destination);
		this.request = request;
		this.id = id;
		this.param = param;
	}

	public ESLRequestPacket(byte channel, int source, int destination, byte request, byte id, short param) {
		super(channel, ESL_SYSTEM_CMD_REQUEST, source, destination);
		this.request = request;
		this.id = id;
		this.param = param;
	}

	@Override
	protected int getDataSize() {
		return 4;
	}

	@Override
	protected void writeData(byte[] buf, int offset) {
		buf[offset + 0] = request;
		buf[offset + 1] = id;
		Endianess.set16bitLE(buf, offset + 2, param);
	}

	@Override
	protected void readData(byte[] buf, int offset) {
		request = buf[offset + 0];
		id = buf[offset + 1];
		param = Endianess.get16bitLE(buf, offset + 2);
	}
}
