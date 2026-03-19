package com.unknown.emulight.lcp.io.esl.protocol;

import com.unknown.util.io.Endianess;

public abstract class ESLSystemPacket extends ESLPacket {
	public static final int ESL_ADDR_MASK = 0x3F;
	public static final int ESL_PREFIX_MASK = 0xFFFFFFC0;

	public static final byte ESL_SYSTEM_CMD_RESET = 0; /* bus reset */
	public static final byte ESL_SYSTEM_CMD_DEVICE_INFO = 1; /* device descriptor packet */
	public static final byte ESL_SYSTEM_CMD_GET_PARAM_INFO = 2; /* request info fo rone parameter */
	public static final byte ESL_SYSTEM_CMD_PARAM_INFO = 3; /* parameter description */
	public static final byte ESL_SYSTEM_CMD_MEASURE_AUDIO = 4; /* request audio routing detection */
	public static final byte ESL_SYSTEM_CMD_AUDIO_ROUTING = 5; /* audio routing info */
	public static final byte ESL_SYSTEM_CMD_GET_STRING = 6; /* request string */
	public static final byte ESL_SYSTEM_CMD_SET_STRING = 7; /* string content */
	public static final byte ESL_SYSTEM_CMD_RX_BYTES = 8; /* request bytes */
	public static final byte ESL_SYSTEM_CMD_TX_BYTES = 9; /* transmission of bytes */
	public static final byte ESL_SYSTEM_CMD_CONSOLE_RX = 10; /* character received on console */
	public static final byte ESL_SYSTEM_CMD_CONSOLE_TX = 11; /* character transmitted on console */
	public static final byte ESL_SYSTEM_CMD_SET_CLOCK = 12; /* set date and time */
	public static final byte ESL_SYSTEM_CMD_SET_PREFIX = 13; /* set global address prefix */
	public static final byte ESL_SYSTEM_CMD_LOG_MESSAGE = 14; /* log message received */
	public static final byte ESL_SYSTEM_CMD_REQUEST = 15; /* request data */
	public static final byte ESL_SYSTEM_CMD_RESPONSE = 16; /* response to a data request */
	public static final byte ESL_SYSTEM_CMD_DISK = 17; /* disk operations */
	public static final byte ESL_SYSTEM_CMD_GRE = -1; /* generic routing encapsulation */

	private byte channel;
	private byte command;
	private int source;
	private int destination;

	ESLSystemPacket(byte address) {
		super(address);
	}

	protected ESLSystemPacket(byte channel, byte command, int source, int destination) {
		this((byte) (destination & ESL_ADDR_MASK), channel, command, source, destination);
	}

	protected ESLSystemPacket(byte dest, byte channel, byte command, int source, int destination) {
		super(dest);
		this.channel = channel;
		this.command = command;
		this.source = source;
		this.destination = destination;
	}

	public int getSource() {
		return source;
	}

	public int getDestination() {
		return destination;
	}

	protected abstract int getDataSize();

	protected abstract void writeData(byte[] buf, int offset);

	protected abstract void readData(byte[] buf, int offset);

	@Override
	protected final int getPayloadSize() {
		return getDataSize() + 10;
	}

	@Override
	public final byte getProtocol() {
		return ESL_PROTO_SYSTEM;
	}

	@Override
	protected final void writePayload(byte[] buf, int offset) {
		buf[offset] = channel;
		buf[offset + 1] = command;
		Endianess.set32bitLE(buf, offset + 2, source);
		Endianess.set32bitLE(buf, offset + 6, destination);
		writeData(buf, offset + 10);
	}

	@Override
	protected final void readPayload(byte[] buf, int offset) {
		channel = buf[offset];
		command = buf[offset + 1];
		source = Endianess.get32bitLE(buf, offset + 2);
		destination = Endianess.get32bitLE(buf, offset + 6);
		readData(buf, offset + 10);
	}

	static final ESLPacket get(byte addr, byte[] buf, int offset) {
		byte cmd = buf[offset + 1];
		switch(cmd) {
		case ESL_SYSTEM_CMD_SET_CLOCK:
			return new ESLSetClockPacket(addr);
		case ESL_SYSTEM_CMD_LOG_MESSAGE:
			return new ESLLogMessagePacket(addr);
		case ESL_SYSTEM_CMD_REQUEST:
			return new ESLRequestPacket(addr);
		case ESL_SYSTEM_CMD_RESPONSE:
			return new ESLResponsePacket(addr);
		case ESL_SYSTEM_CMD_DISK:
			return new ESLDiskOperationPacket(addr);
		case ESL_SYSTEM_CMD_GRE:
			return new ESLGrePacket(addr);
		default:
			return null;
		}
	}
}
