package com.unknown.emulight.lcp.project;

import java.io.IOException;

import com.unknown.xml.dom.Element;

public class AudioTrack extends Track<AbstractPart> {
	public AudioTrack(Project project, String name) {
		super(AUDIO, project, name);
	}

	@Override
	protected AbstractPart createPart() {
		// TODO
		return null;
	}

	@Override
	protected void readTrack(Element xml) throws IOException {
		// TODO
	}

	@Override
	protected void writeTrack(Element xml) {
		// TODO
	}

	@Override
	public AudioTrack clone() {
		AudioTrack track = new AudioTrack(getProject(), getName());
		copy(track);
		return track;
	}
}
