package com.unknown.emulight.lcp.live;

public class TriggerKey {
	public static final int TYPE_NOTE = 0;
	public static final int TYPE_CONTROLLER = 1;

	private final int channel;
	private final int key;
	private final int type;

	public TriggerKey(int type, int channel, int key) {
		this.type = type;
		this.channel = channel;
		this.key = key;
	}

	public int getType() {
		return type;
	}

	public int getChannel() {
		return channel;
	}

	public int getKey() {
		return key;
	}

	public String getTypeName() {
		switch(type) {
		case TYPE_NOTE:
			return "note";
		case TYPE_CONTROLLER:
			return "cc";
		default:
			return "unknown";
		}
	}

	public static int getType(String name) {
		switch(name) {
		case "note":
			return TYPE_NOTE;
		case "cc":
			return TYPE_CONTROLLER;
		default:
			return 0;
		}
	}

	@Override
	public int hashCode() {
		return type ^ channel ^ key;
	}

	@Override
	public boolean equals(Object o) {
		if(o == null || !(o instanceof TriggerKey)) {
			return false;
		}
		TriggerKey k = (TriggerKey) o;
		return type == k.type && channel == k.channel && key == k.key;
	}

	@Override
	public String toString() {
		return "[ch=" + channel + ",key=" + key + ",type=" + type + "]";
	}
}
