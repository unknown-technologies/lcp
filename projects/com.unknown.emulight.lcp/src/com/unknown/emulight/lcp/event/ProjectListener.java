package com.unknown.emulight.lcp.event;

import com.unknown.emulight.lcp.project.Track;

public interface ProjectListener {
	final static String NAME = "name";
	final static String AUTHOR = "author";
	final static String TRACK = "track";

	void propertyChanged(String key);

	void trackAdded(Track<?> track);

	void trackRemoved(Track<?> track);

	void projectLoaded();
}
