package com.unknown.emulight.lcp.sequencer;

import java.io.IOException;

import com.unknown.xml.dom.Element;

public class Note {
	private long time;
	private int length;
	private int channel;

	private int key;
	private int velocity;
	private int releaseVelocity;

	public Note(long time, int key, int velocity, int length) {
		this(time, 0, key, velocity, 64, length);
	}

	public Note(long time, int channel, int key, int velocity, int length) {
		this(time, channel, key, velocity, 64, length);
	}

	public Note(long time, int channel, int key, int velocity, int releaseVelocity, int length) {
		this.time = time;
		this.channel = channel;
		this.key = key;
		this.velocity = velocity;
		this.releaseVelocity = releaseVelocity;
		this.length = length;
	}

	@Override
	public Note clone() {
		return new Note(time, channel, key, velocity, releaseVelocity, length);
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public int getChannel() {
		return channel;
	}

	public void setChannel(int channel) {
		this.channel = channel;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public long getEnd() {
		return time + length;
	}

	public boolean containsTime(long t) {
		return t >= time && t < time + length;
	}

	public int getKey() {
		return key;
	}

	public void setKey(int key) {
		this.key = key;
	}

	public int getVelocity() {
		return velocity;
	}

	public void setVelocity(int velocity) {
		this.velocity = velocity;
	}

	public int getReleaseVelocity() {
		return releaseVelocity;
	}

	public void setReleaseVelocity(int releaseVelocity) {
		this.releaseVelocity = releaseVelocity;
	}

	@Override
	public String toString() {
		return "Note[time=" + time + ",length=" + length + ",channel=" + channel + ",key=" + key +
				",velocity=" + velocity + ",release=" + releaseVelocity + "]";
	}

	public static Note read(Element xml) throws IOException {
		if(!xml.name.equals("note")) {
			throw new IOException("not a note");
		}

		long time = Long.parseLong(xml.getAttribute("time"));
		int length = Integer.parseInt(xml.getAttribute("length"));
		int channel = Integer.parseInt(xml.getAttribute("channel"));
		int key = Integer.parseInt(xml.getAttribute("key"));
		int velocity = Integer.parseInt(xml.getAttribute("velocity"));
		int releaseVelocity = Integer.parseInt(xml.getAttribute("release-velocity"));

		return new Note(time, channel, key, velocity, releaseVelocity, length);
	}

	public Element write() {
		Element xml = new Element("note");
		xml.addAttribute("time", Long.toString(time));
		xml.addAttribute("length", Integer.toString(length));
		xml.addAttribute("channel", Integer.toString(channel));
		xml.addAttribute("key", Integer.toString(key));
		xml.addAttribute("velocity", Integer.toString(velocity));
		xml.addAttribute("release-velocity", Integer.toString(releaseVelocity));
		return xml;
	}
}
