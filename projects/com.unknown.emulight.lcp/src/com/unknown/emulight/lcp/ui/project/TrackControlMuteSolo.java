package com.unknown.emulight.lcp.ui.project;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.IOException;
import java.util.logging.Logger;

import com.unknown.emulight.lcp.project.Track;
import com.unknown.emulight.lcp.ui.resources.icons.project.trackcontrols.TrackControlIcons;
import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;

public class TrackControlMuteSolo extends TrackControl {
	private static final Logger log = Trace.create(TrackControlMuteSolo.class);

	private final Track<?> track;

	public TrackControlMuteSolo(ProjectView parent, Track<?> track) {
		super(parent);
		this.track = track;
	}

	@Override
	public int getWidth() {
		return 45;
	}

	@Override
	public int getHeight() {
		return 17;
	}

	@Override
	public void paint(Graphics g, int x, int y, ImageObserver observer) {
		try {
			BufferedImage mute = TrackControlIcons.get(TrackControlIcons.MUTE, track.isMuted());
			BufferedImage solo = TrackControlIcons.get(TrackControlIcons.SOLO, track.isSolo());
			g.drawImage(mute, x, y, observer);
			g.drawImage(solo, x + mute.getWidth() - 1, y, observer);
		} catch(IOException e) {
			log.log(Levels.ERROR, "Failed to load image", e);
		}
	}

	@Override
	public boolean click(int x, int y, int px, int py) {
		if(x >= 1 && x <= 22 && y >= 1 && y <= 16) {
			track.setMuted(!track.isMuted());
			return true;
		} else if(x >= 23 && x <= 44 && y >= 1 && y <= 16) {
			track.setSolo(!track.isSolo());
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean isIntegrated() {
		return false;
	}
}
