package com.unknown.emulight.lcp.io.esl.protocol;

import com.unknown.util.io.Endianess;

public class ESLDiskOperationPacket extends ESLSystemPacket {
	// @formatter:off
	public static final byte ESL_SYSTEM_DISK_READ          = 0; /* request for block read */
	public static final byte ESL_SYSTEM_DISK_READ_DATA     = 1; /* response to block read */
	public static final byte ESL_SYSTEM_DISK_READ_FAIL     = 2; /* block read failed */
	public static final byte ESL_SYSTEM_DISK_WRITE         = 3; /* request for block write */
	public static final byte ESL_SYSTEM_DISK_WRITE_OK      = 4; /* block write completed */
	public static final byte ESL_SYSTEM_DISK_WRITE_FAIL    = 5; /* block write failed */
	// @formatter:on

	private long offset;
	private byte length;
	private byte disk;
	private byte type;
	private byte txid;
	private byte[] data;

	ESLDiskOperationPacket(byte addr) {
		super(addr);
	}

	public ESLDiskOperationPacket(byte dest, byte channel, int source, int destination, byte txid, byte type,
			byte disk, long offset, byte length, byte[] data) {
		super(dest, channel, ESL_SYSTEM_CMD_DISK, source, destination);
		this.txid = txid;
		this.type = type;
		this.disk = disk;
		this.offset = offset;
		this.length = length;
		this.data = data;
	}

	public ESLDiskOperationPacket(byte channel, int source, int destination, byte txid, byte type, byte disk,
			long offset, byte length, byte[] data) {
		super(channel, ESL_SYSTEM_CMD_DISK, source, destination);
		this.txid = txid;
		this.type = type;
		this.disk = disk;
		this.offset = offset;
		this.length = length;
		this.data = data;
	}

	public byte getType() {
		return type;
	}

	public byte getTxId() {
		return txid;
	}

	public byte[] getData() {
		return data;
	}

	public boolean isError() {
		switch(type) {
		case ESL_SYSTEM_DISK_READ:
		case ESL_SYSTEM_DISK_READ_DATA:
		case ESL_SYSTEM_DISK_WRITE:
		case ESL_SYSTEM_DISK_WRITE_OK:
			return false;
		case ESL_SYSTEM_DISK_READ_FAIL:
		case ESL_SYSTEM_DISK_WRITE_FAIL:
		default:
			return true;
		}
	}

	public String getDisplayType() {
		switch(type) {
		case ESL_SYSTEM_DISK_READ:
			return "read";
		case ESL_SYSTEM_DISK_READ_DATA:
			return "read ok";
		case ESL_SYSTEM_DISK_READ_FAIL:
			return "read failed";
		case ESL_SYSTEM_DISK_WRITE:
			return "write";
		case ESL_SYSTEM_DISK_WRITE_OK:
			return "write ok";
		case ESL_SYSTEM_DISK_WRITE_FAIL:
			return "write failed";
		default:
			return "unknown type " + Byte.toUnsignedInt(type);
		}
	}

	@Override
	protected int getDataSize() {
		return 12 + (data != null ? data.length : 0);
	}

	@Override
	protected void writeData(byte[] buf, int off) {
		Endianess.set64bitLE(buf, off, offset);
		buf[off + 8] = length;
		buf[off + 9] = disk;
		buf[off + 10] = type;
		buf[off + 11] = txid;
		if(data != null && data.length > 0) {
			System.arraycopy(data, 0, buf, off + 12, data.length);
		}
	}

	@Override
	protected void readData(byte[] buf, int off) {
		offset = Endianess.get64bitLE(buf, off);
		length = buf[off + 8];
		disk = buf[off + 9];
		type = buf[off + 10];
		txid = buf[off + 11];
		if(length > 0 && (type == ESL_SYSTEM_DISK_READ_DATA || type == ESL_SYSTEM_DISK_WRITE)) {
			data = new byte[length];
			System.arraycopy(buf, off + 12, data, 0, length);
		}
	}

	@Override
	public String toString() {
		return "ESLDiskOperationPacket[type=" + getDisplayType() + ",txid=" + txid + ",disk=" + disk +
				",offset=" + offset + ",length=" + length + "]";
	}
}
