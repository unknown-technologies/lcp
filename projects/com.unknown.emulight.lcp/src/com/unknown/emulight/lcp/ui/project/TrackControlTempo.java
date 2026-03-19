package com.unknown.emulight.lcp.ui.project;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.logging.Logger;

import com.unknown.emulight.lcp.project.TempoTrack;
import com.unknown.emulight.lcp.ui.UIUtils;
import com.unknown.emulight.lcp.ui.resources.icons.project.trackcontrols.TrackControlIcons;
import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;
import com.unknown.util.ui.ADM3AFont;

public class TrackControlTempo extends TrackControl {
	private static final Logger log = Trace.create(TrackControlTempo.class);

	private static final Color TRANSLUCENT = new Color(0, true);

	private static final NumberFormat FMT = new DecimalFormat("##0.000", UIUtils.NUMBER_FMT_SYMBOLS);

	private final TempoTrack track;

	public TrackControlTempo(ProjectView parent, TempoTrack track) {
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
			g.drawImage(bg, x, y, observer);
			double bpm = track.getTempo(parent.getTime());
			String tempo = FMT.format(bpm);
			int px = x + (getWidth() - (tempo.length() * ADM3AFont.WIDTH - 1)) / 2;
			int py = y + (getHeight() + 7) / 2;
			ADM3AFont.render(g, px, py, Color.BLACK, TRANSLUCENT, tempo);
		} catch(IOException e) {
			log.log(Levels.ERROR, "Failed to load image", e);
		}
	}

	@Override
	public boolean click(int x, int y, int px, int py) {
		return false;
	}

	@Override
	public boolean isIntegrated() {
		return true;
	}
}
