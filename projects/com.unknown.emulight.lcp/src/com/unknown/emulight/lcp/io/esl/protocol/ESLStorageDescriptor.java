package com.unknown.emulight.lcp.io.esl.protocol;

public class ESLStorageDescriptor {
	public static final int MAXCNT = 16;

	private static final String[] EXP = { "", "KiB", "MiB", "GiB", "TiB", "PiB" };
	private static final String[] TYPES = { "None", "RAM", "FLASH", "eMMC", "SD card", "MCU FLASH" };

	private final byte[] type;
	private final byte[] size;
	private final byte[] sizeExp;

	public ESLStorageDescriptor(byte[] data) {
		type = new byte[MAXCNT];
		size = new byte[MAXCNT];
		sizeExp = new byte[MAXCNT];

		for(int i = 0; i < MAXCNT; i++) {
			type[i] = data[i];
			size[i] = data[i + 16];
			sizeExp[i] = data[i + 32];
		}
	}

	public ESLStorageDescriptor(ESLResponsePacket packet) {
		this(check(packet));
	}

	private static byte[] check(ESLResponsePacket packet) {
		if(packet.getRequest() != ESLRequestPacket.ESL_SYSTEM_REQ_STORAGE) {
			throw new IllegalArgumentException("not a STORAGE response");
		}
		return packet.getData();
	}

	public byte getType(int idx) {
		return type[idx];
	}

	public String getDisplaySize(int idx) {
		return Byte.toUnsignedInt(size[idx]) + EXP[sizeExp[idx]];
	}

	public String getDisplayType(int idx) {
		int t = Byte.toUnsignedInt(type[idx]);
		if(t < TYPES.length) {
			return TYPES[t];
		} else {
			return "unknown type " + t;
		}
	}

	public int getMaxIndex() {
		for(int i = 0; i < MAXCNT; i++) {
			if(type[i] == 0) {
				return i;
			}
		}

		return MAXCNT;
	}
}
