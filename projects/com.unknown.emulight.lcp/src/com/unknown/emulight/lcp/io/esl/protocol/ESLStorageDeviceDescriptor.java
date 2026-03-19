package com.unknown.emulight.lcp.io.esl.protocol;

import java.nio.charset.StandardCharsets;

import com.unknown.util.io.Endianess;

/*
 * typedef struct {
 *         char            name[8];
 *         u64             size;
 *         u16             sector_size;
 *         u8              id;
 *         u8              type;
 *         u8              size_mantissa;
 *         u8              size_exponent;
 *         u8              pad[2];
 * } ESLStorageDeviceDescriptor;
 */
public class ESLStorageDeviceDescriptor {
	private final String name;
	private final long size;
	private final int sectorSize;
	private final byte id;
	private final byte type;

	public ESLStorageDeviceDescriptor(byte[] data) {
		int length = 8;
		for(int i = 0; i < 8; i++) {
			if(data[i] == 0) {
				length = i;
				break;
			}
		}

		name = new String(data, 0, length, StandardCharsets.ISO_8859_1);
		size = Endianess.get64bitLE(data, 8);
		sectorSize = Short.toUnsignedInt(Endianess.get16bitLE(data, 16));
		id = data[18];
		type = data[19];
	}

	public ESLStorageDeviceDescriptor(ESLResponsePacket packet) {
		this(check(packet));
	}

	private static byte[] check(ESLResponsePacket packet) {
		if(packet.getRequest() != ESLRequestPacket.ESL_SYSTEM_REQ_STORAGE_DEVICE) {
			throw new IllegalArgumentException("not a STORAGE DEVICE response");
		}
		return packet.getData();
	}

	public String getName() {
		return name;
	}

	public long getSize() {
		return size;
	}

	public int getSectorSize() {
		return sectorSize;
	}

	public byte getId() {
		return id;
	}

	public byte getType() {
		return type;
	}
}
