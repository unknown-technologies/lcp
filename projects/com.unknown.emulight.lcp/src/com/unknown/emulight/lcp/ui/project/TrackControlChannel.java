package com.unknown.emulight.lcp.ui.project;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.IOException;
import java.util.logging.Logger;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.unknown.emulight.lcp.sequencer.MidiTrack;
import com.unknown.emulight.lcp.ui.resources.icons.project.trackcontrols.TrackControlIcons;
import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;
import com.unknown.util.ui.ADM3AFont;

public class TrackControlChannel extends TrackControl {
	private static final Logger log = Trace.create(TrackControlChannel.class);
	private static final Color TRANSLUCENT = new Color(0, true);

	private final MidiTrack track;

	private final JPopupMenu menu;

	public TrackControlChannel(ProjectView parent, MidiTrack track) {
		super(parent);
		this.track = track;

		menu = new JPopupMenu();
		JMenuItem any = new JMenuItem("Any");
		any.addActionListener(e -> setChannel(MidiTrack.ANY));
		menu.add(any);

		for(int i = 0; i < 16; i++) {
			JMenuItem item = new JMenuItem(Integer.toString(i + 1));
			int ch = i;
			item.addActionListener(e -> setChannel(ch));
			menu.add(item);
		}
	}

	private void setChannel(int ch) {
		track.setChannel(ch);
		parent.repaint();
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
			BufferedImage channel = TrackControlIcons.get(TrackControlIcons.CHANNEL);
			g.drawImage(bg, x, y, observer);
			g.drawImage(channel, x, y, observer);
			String ch = track.getChannel() == MidiTrack.ANY ? "Any"
					: Integer.toString(track.getChannel() + 1);
			int px = x + channel.getWidth() + (24 - (ch.length() * ADM3AFont.WIDTH - 1)) / 2;
			int py = y + (getHeight() + 7) / 2;
			ADM3AFont.render(g, px, py, Color.BLACK, TRANSLUCENT, ch);
		} catch(IOException e) {
			log.log(Levels.ERROR, "Failed to load image", e);
		}
	}

	@Override
	public boolean click(int x, int y, int px, int py) {
		if(x >= 1 && x < 48 && y >= 1 && y < 16) {
			menu.show(parent, px, py);
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
