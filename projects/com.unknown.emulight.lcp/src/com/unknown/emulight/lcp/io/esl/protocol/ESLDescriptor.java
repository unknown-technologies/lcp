package com.unknown.emulight.lcp.io.esl.protocol;

import java.nio.charset.StandardCharsets;

import com.unknown.util.BitTest;
import com.unknown.util.io.Endianess;
import com.unknown.util.io.FourCC;

/*
 * typedef struct {
 *       u32             device_id;
 *       u32             capabilities;
 *       u32             bus_speed;
 *       u8              type;
 *       u8              channels;
 *       u8              address;
 *       u8              revision;
 *       u8              name[32];
 *       u8              manufacturer[32];
 * } ESLDescriptor;
 */
public class ESLDescriptor {
	// @formatter:off
	// ESL device types
	public static final byte ESL_TYPE_UNKNOWN               = 0;
	public static final byte ESL_TYPE_CONTROLLER            = 1;
	public static final byte ESL_TYPE_SEQUENCER             = 2;
	public static final byte ESL_TYPE_MIDI                  = 3;
	public static final byte ESL_TYPE_CV                    = 4;
	public static final byte ESL_TYPE_MIXER                 = 5;
	public static final byte ESL_TYPE_RECORDER              = 6;
	public static final byte ESL_TYPE_SYNTHESIZER           = 7;
	public static final byte ESL_TYPE_SAMPLER               = 8;

	// ESL device capability flags
	public static final int ESL_FLAG_CONTROLLER             = 0x00000001;
	public static final int ESL_FLAG_USER_INTERFACE         = 0x00000002;
	public static final int ESL_FLAG_MIDI_INPUT             = 0x00000004;
	public static final int ESL_FLAG_MIDI_OUTPUT            = 0x00000008;
	public static final int ESL_FLAG_AUDIO_INPUT            = 0x00000010;
	public static final int ESL_FLAG_AUDIO_OUTPUT           = 0x00000020;
	public static final int ESL_FLAG_AUDIO_RECORDER         = 0x00000040;
	public static final int ESL_FLAG_AUDIO_PLAYER           = 0x00000080;
	public static final int ESL_FLAG_CV_INPUT               = 0x00000100;
	public static final int ESL_FLAG_CV_OUTPUT              = 0x00000200;
	public static final int ESL_FLAG_GATE_INPUT             = 0x00000400;
	public static final int ESL_FLAG_GATE_OUTPUT            = 0x00000800;
	public static final int ESL_FLAG_SEQUENCER              = 0x00001000;
	public static final int ESL_FLAG_MIXER                  = 0x00002000;
	public static final int ESL_FLAG_SYNTHESIZER            = 0x00004000;
	public static final int ESL_FLAG_SAMPLER                = 0x00008000;
	public static final int ESL_FLAG_STORAGE                = 0x00010000;
	public static final int ESL_FLAG_RTC                    = 0x00020000;
	public static final int ESL_FLAG_RAM                    = 0x00040000;
	public static final int ESL_FLAG_FX_PROCESSOR           = 0x00080000;
	public static final int ESL_FLAG_BUS_LAST_DEVICE        = 0x40000000;
	public static final int ESL_FLAG_BUS_MASTER             = 0x80000000;
	// @formatter:on

	private int deviceId;
	private int capabilities;
	private int busSpeed;
	private byte type;
	private byte channels;
	private byte address;
	private byte revision;
	private String name;
	private String manufacturer;

	public void read(byte[] data) {
		deviceId = Endianess.get32bitBE(data, 0);
		capabilities = Endianess.get32bitLE(data, 4);
		busSpeed = Endianess.get32bitLE(data, 8);
		type = data[12];
		channels = data[13];
		address = data[14];
		revision = data[15];
		name = new String(data, 16, 32, StandardCharsets.UTF_8).trim();
		manufacturer = new String(data, 48, 32, StandardCharsets.UTF_8).trim();
	}

	public int getDeviceId() {
		return deviceId;
	}

	public int getCapabilities() {
		return capabilities;
	}

	public boolean hasCapability(int cap) {
		return BitTest.test(capabilities, cap);
	}

	public int getBusSpeed() {
		return busSpeed;
	}

	public byte getType() {
		return type;
	}

	public byte getChannels() {
		return channels;
	}

	public byte getAddress() {
		return address;
	}

	public byte getRevision() {
		return revision;
	}

	public String getName() {
		return name;
	}

	public String getManufacturer() {
		return manufacturer;
	}

	public static ESLDescriptor parse(byte[] data) {
		ESLDescriptor desc = new ESLDescriptor();
		desc.read(data);
		return desc;
	}

	@Override
	public String toString() {
		return "Device[id='" + FourCC.ascii(deviceId) + "',name=\"" + name + "\",manufacturer=\"" +
				manufacturer + "\"]";
	}

	public String getTypeName() {
		switch(type) {
		default:
		case ESL_TYPE_UNKNOWN:
			return "unknown";
		case ESL_TYPE_CONTROLLER:
			return "controller";
		case ESL_TYPE_SEQUENCER:
			return "sequencer";
		case ESL_TYPE_MIDI:
			return "MIDI";
		case ESL_TYPE_CV:
			return "CV";
		case ESL_TYPE_MIXER:
			return "mixer";
		case ESL_TYPE_RECORDER:
			return "recorder";
		case ESL_TYPE_SYNTHESIZER:
			return "synthesizer";
		case ESL_TYPE_SAMPLER:
			return "sampler";
		}
	}
}
