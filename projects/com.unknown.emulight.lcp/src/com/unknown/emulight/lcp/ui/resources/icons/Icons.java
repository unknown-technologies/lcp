package com.unknown.emulight.lcp.ui.resources.icons;

import java.awt.Image;

import javax.swing.ImageIcon;

import com.unknown.util.ResourceLoader;

public class Icons {
	public static final String BOOK = "book";
	public static final String CHIP = "chip";
	public static final String SETTINGS = "settings";
	public static final String EMULIGHT = "emulight";

	public static final ImageIcon get(String name, int size) {
		return new ImageIcon(ResourceLoader.getResource(Icons.class, name + "-" + size + ".png"));
	}

	public static final Image getImage(String name, int size) {
		return get(name, size).getImage();
	}
}
