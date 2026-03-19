package com.unknown.emulight.lcp.ui.resources.icons.project.tracktype;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import com.unknown.emulight.lcp.project.Track;
import com.unknown.util.ResourceLoader;

public class TrackIcons {
	public static final String AUDIO = "audio";
	public static final String INSTRUMENT = "instrument";
	public static final String MIDI = "midi";
	public static final String SAMPLER = "sampler";
	public static final String ARRANGER = "arranger";
	public static final String CHORD = "chord";
	public static final String FX = "fx";
	public static final String FOLDER = "folder";
	public static final String GROUP = "group";
	public static final String MARKER = "marker";
	public static final String RULER = "ruler";
	public static final String SIGNATURE = "signature";
	public static final String TEMPO = "tempo";
	public static final String TRANSPOSE = "transpose";
	public static final String VCA = "vca";
	public static final String VIDEO = "video";
	public static final String LASER = "laser";
	public static final String DMX = "dmx";

	private static final Map<String, BufferedImage> ICONS = new HashMap<>();

	public static final BufferedImage get(String name) throws IOException {
		BufferedImage img = ICONS.get(name);
		if(img == null) {
			img = ImageIO.read(ResourceLoader.getResource(TrackIcons.class, name + ".png"));
			ICONS.put(name, img);
		}
		return img;
	}

	public static final BufferedImage get(int type) throws IOException {
		switch(type) {
		case Track.AUDIO:
			return get(AUDIO);
		case Track.INSTRUMENT:
			return get(INSTRUMENT);
		case Track.MIDI:
			return get(MIDI);
		case Track.SAMPLER:
			return get(SAMPLER);
		case Track.ARRANGER:
			return get(ARRANGER);
		case Track.CHORD:
			return get(CHORD);
		case Track.FX:
			return get(FX);
		case Track.FOLDER:
			return get(FOLDER);
		case Track.GROUP:
			return get(GROUP);
		case Track.MARKER:
			return get(MARKER);
		case Track.RULER:
			return get(RULER);
		case Track.SIGNATURE:
			return get(SIGNATURE);
		case Track.TEMPO:
			return get(TEMPO);
		case Track.TRANSPOSE:
			return get(TRANSPOSE);
		case Track.VCA:
			return get(VCA);
		case Track.VIDEO:
			return get(VIDEO);
		case Track.LASER:
			return get(LASER);
		case Track.DMX:
			return get(DMX);
		default:
			throw new IOException("unknown track type " + type);
		}
	}
}
