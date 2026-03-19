package com.unknown.emulight.lcp.sequencer;

import java.io.IOException;

import com.unknown.xml.dom.Element;

public class PitchBend {
	private long time;
	private int channel;
	private int bend;

	public PitchBend(long time, int channel, int bend) {
		this.time = time;
		this.channel = channel;
		this.bend = bend;
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

	public int getBend() {
		return bend;
	}

	public void setBend(int bend) {
		this.bend = bend;
	}

	@Override
	public String toString() {
		return "PitchBend[time=" + time + ",channel=" + channel + ",bend=" + bend + "]";
	}

	@Override
	public PitchBend clone() {
		return new PitchBend(time, channel, bend);
	}

	public static PitchBend read(Element xml) throws IOException {
		if(!xml.name.equals("pitch-bend")) {
			throw new IOException("not a pitch bend");
		}

		long time = Long.parseLong(xml.getAttribute("time"));
		int channel = Integer.parseInt(xml.getAttribute("channel"));
		int bend = Integer.parseInt(xml.getAttribute("bend"));

		return new PitchBend(time, channel, bend);
	}

	public Element write() {
		Element xml = new Element("pitch-bend");
		xml.addAttribute("time", Long.toString(time));
		xml.addAttribute("channel", Integer.toString(channel));
		xml.addAttribute("bend", Integer.toString(bend));
		return xml;
	}
}
