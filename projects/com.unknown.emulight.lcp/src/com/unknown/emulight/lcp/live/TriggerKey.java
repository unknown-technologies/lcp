package com.unknown.emulight.lcp.live;

import java.io.IOException;

import com.unknown.xml.dom.Element;

public class TriggerKey {
	private final int channel;
	private final int key;

	public TriggerKey(int channel, int key) {
		this.channel = channel;
		this.key = key;
	}

	public int getChannel() {
		return channel;
	}

	public int getKey() {
		return key;
	}

	@Override
	public int hashCode() {
		return channel ^ key;
	}

	@Override
	public boolean equals(Object o) {
		if(o == null || !(o instanceof TriggerKey)) {
			return false;
		}
		TriggerKey k = (TriggerKey) o;
		return channel == k.channel && key == k.key;
	}

	@Override
	public String toString() {
		return "[ch=" + channel + ",key=" + key + "]";
	}

	public static TriggerKey read(Element xml) throws IOException {
		if(!xml.name.equals("trigger-key")) {
			throw new IOException("not a trigger-key");
		}

		int channel;
		try {
			channel = Integer.parseInt(xml.getAttribute("channel"));
		} catch(NumberFormatException | NullPointerException e) {
			throw new IOException("invalid channel");
		}

		int key;
		try {
			key = Integer.parseInt(xml.getAttribute("key"));
		} catch(NumberFormatException | NullPointerException e) {
			throw new IOException("invalid key");
		}

		return new TriggerKey(channel, key);
	}

	public Element write() {
		Element xml = new Element("trigger-key");
		xml.addAttribute("channel", Integer.toString(channel));
		xml.addAttribute("key", Integer.toString(key));
		return xml;
	}
}
