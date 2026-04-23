package com.unknown.emulight.lcp.dmx;

import java.io.IOException;

import com.unknown.emulight.lcp.project.Project;
import com.unknown.emulight.lcp.project.Track;
import com.unknown.xml.dom.Element;

public class DMXTrack extends Track<DMXPart> {
	public DMXTrack(Project project, String name) {
		super(DMX, project, name);
	}

	@Override
	protected void readTrack(Element xml) throws IOException {
		// TODO Auto-generated method stub
	}

	@Override
	protected void writeTrack(Element xml) {
		// TODO Auto-generated method stub
	}

	@Override
	protected DMXPart createPart() {
		return new DMXPart();
	}

	@Override
	public DMXTrack clone() {
		DMXTrack track = new DMXTrack(getProject(), getName());
		copy(track);
		return track;
	}
}
