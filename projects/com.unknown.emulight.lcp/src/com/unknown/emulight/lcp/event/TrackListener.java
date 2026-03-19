package com.unknown.emulight.lcp.event;

public interface TrackListener {
	final static String NAME = "name";
	final static String CHANNEL = "channel";
	final static String PORT = "port";
	final static String PART = "part";
	final static String MUTE = "mute";
	final static String SOLO = "solo";
	final static String MONITOR = "monitor";
	final static String RECORD = "record";
	final static String ENABLE = "enable";
	final static String LOCK = "lock";
	final static String VOLUME = "volume";
	final static String PAN = "pan";
	final static String PROGRAM = "program";

	void propertyChanged(String key);
}
