package com.unknown.emulight.lcp.audio;

import java.io.File;
import java.io.IOException;

import com.unknown.emulight.lcp.project.AbstractPart;
import com.unknown.xml.dom.Element;

public class AudioPart extends AbstractPart {
	private AudioData data;

	public AudioPart() {
		// nothing loaded
		data = null;
	}

	public AudioPart(AudioData data) {
		this.data = data;
	}

	public AudioPart(File file) throws IOException {
		data = new AudioData(file);
	}

	public AudioData getData() {
		return data;
	}

	public void setData(AudioData data) {
		this.data = data;
	}

	@Override
	public long getLength() {
		// this is in ms, not ticks
		if(data != null) {
			return data.getLength();
		} else {
			return 0;
		}
	}

	@Override
	public void read(Element xml) throws IOException {
		String path = xml.getAttribute("file");
		if(path != null) {
			data = new AudioData(new File(path));
		}
	}

	@Override
	public void write(Element xml) {
		if(data != null) {
			xml.addAttribute("file", data.getFile().toString());
		}
	}

	@Override
	public AbstractPart clone() {
		AudioPart part = new AudioPart();
		copy(part);
		part.data = data;
		return part;
	}
}
