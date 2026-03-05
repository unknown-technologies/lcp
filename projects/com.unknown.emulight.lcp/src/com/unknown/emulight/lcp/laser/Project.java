package com.unknown.emulight.lcp.laser;

import java.util.ArrayList;
import java.util.List;

public class Project {
	private final LaserProcessor processor;

	private final List<Clip> clipLibrary = new ArrayList<>();

	public Project(LaserProcessor processor) {
		this.processor = processor;
	}

	public LaserProcessor getProcessor() {
		return processor;
	}

	public List<Clip> getClipLibrary() {
		return clipLibrary;
	}
}
