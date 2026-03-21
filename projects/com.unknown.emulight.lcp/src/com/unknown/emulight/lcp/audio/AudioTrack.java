package com.unknown.emulight.lcp.audio;

import java.io.IOException;

import com.unknown.emulight.lcp.project.Project;
import com.unknown.emulight.lcp.project.Track;
import com.unknown.xml.dom.Element;

public class AudioTrack extends Track<AudioPart> {
	public AudioTrack(Project project, String name) {
		super(AUDIO, project, name);
	}

	@Override
	protected AudioPart createPart() {
		return new AudioPart();
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
