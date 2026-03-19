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

public class TrackControlEditInstrument extends TrackControl {
	private static final Logger log = Trace.create(TrackControlEditInstrument.class);

	private final Track<?> track;

	public TrackControlEditInstrument(ProjectView parent, Track<?> track) {
		super(parent);
		this.track = track;
	}

	@Override
	public int getWidth() {
		return 48;
	}

	@Override
	public int getHeight() {
		return 17;
	}

	@Override
	public void paint(Graphics g, int x, int y, ImageObserver observer) {
		try {
			BufferedImage bg = TrackControlIcons.get(TrackControlIcons.BACKGROUND48, true);
			BufferedImage editor = TrackControlIcons.get(TrackControlIcons.EDITOR,
					parent.isTrackEditorOpen(track));
			BufferedImage instrument = TrackControlIcons.get(TrackControlIcons.INSTRUMENT, false);
			g.drawImage(bg, x, y, observer);
			g.drawImage(editor, x, y, observer);
			g.drawImage(instrument, x + editor.getWidth() + 2, y, observer);
		} catch(IOException e) {
			log.log(Levels.ERROR, "Failed to load image", e);
		}
	}

	@Override
	public boolean click(int x, int y, int px, int py) {
		if(x >= 1 && x < 24 && y >= 1 && y <= 16) {
			parent.showTrackEditor(track);
			return true;
		} else if(x > 26 && x <= 48 && y >= 1 && y <= 16) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean isIntegrated() {
		return true;
	}
}
