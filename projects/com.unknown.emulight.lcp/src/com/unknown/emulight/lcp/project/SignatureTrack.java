package com.unknown.emulight.lcp.project;

import java.io.IOException;

import com.unknown.xml.dom.Element;

public class SignatureTrack extends Track<AbstractPart> {
	public SignatureTrack(Project project, String name) {
		super(SIGNATURE, project, name);
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
	public SignatureTrack clone() {
		SignatureTrack track = new SignatureTrack(getProject(), getName());
		copy(track);
		return track;
	}
}
