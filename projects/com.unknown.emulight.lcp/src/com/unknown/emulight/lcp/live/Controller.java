package com.unknown.emulight.lcp.live;

import java.io.IOException;

import com.unknown.xml.dom.Element;

public class Controller {
	private final int channel;
	private final int controller;

	public Controller(int channel, int controller) {
		this.channel = channel;
		this.controller = controller;
	}

	public int getChannel() {
		return channel;
	}

	public int getController() {
		return controller;
	}

	@Override
	public int hashCode() {
		return channel ^ controller;
	}

	@Override
	public boolean equals(Object o) {
		if(o == null || !(o instanceof Controller)) {
			return false;
		}
		Controller k = (Controller) o;
		return channel == k.channel && controller == k.controller;
	}

	@Override
	public String toString() {
		return "[ch=" + channel + ",cc=" + controller + "]";
	}

	public static Controller read(Element xml) throws IOException {
		if(!xml.name.equals("controller")) {
			throw new IOException("not a controller");
		}

		int channel;
		try {
			channel = Integer.parseInt(xml.getAttribute("channel"));
		} catch(NumberFormatException | NullPointerException e) {
			throw new IOException("invalid channel");
		}

		int controller;
		try {
			controller = Integer.parseInt(xml.getAttribute("cc"));
		} catch(NumberFormatException | NullPointerException e) {
			throw new IOException("invalid controller");
		}

		return new Controller(channel, controller);
	}

	public Element write() {
		Element xml = new Element("controller");
		xml.addAttribute("channel", Integer.toString(channel));
		xml.addAttribute("cc", Integer.toString(controller));
		return xml;
	}
}
