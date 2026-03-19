package com.unknown.emulight.lcp.ui.resources.icons.project.trackcontrols;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import com.unknown.util.ResourceLoader;

public class TrackControlIcons {
	public static final String BACKGROUND1 = "background-1";
	public static final String BACKGROUND2 = "background-2";
	public static final String BACKGROUND48 = "background-48";
	public static final String CHANNEL = "channel";

	public static final String MUTE = "mute";
	public static final String SOLO = "solo";
	public static final String RECORD = "record";
	public static final String MONITOR = "monitor";
	public static final String EDITOR = "editor";
	public static final String INSTRUMENT = "instrument";
	public static final String ENABLE = "enable";
	public static final String LOCK = "lock";
	public static final String TRACKLOCK = "tracklock";

	private static final Map<String, BufferedImage> ICONS = new HashMap<>();

	public static final BufferedImage get(String name) throws IOException {
		BufferedImage img = ICONS.get(name);
		if(img == null) {
			img = ImageIO.read(ResourceLoader.getResource(TrackControlIcons.class, name + ".png"));
			ICONS.put(name, img);
		}
		return img;
	}

	public static final BufferedImage get(String name, boolean active) throws IOException {
		String filename = name + "-" + (active ? "active" : "inactive");
		BufferedImage img = ICONS.get(filename);
		if(img == null) {
			img = ImageIO.read(ResourceLoader.getResource(TrackControlIcons.class, filename + ".png"));
			ICONS.put(filename, img);
		}
		return img;
	}
}
