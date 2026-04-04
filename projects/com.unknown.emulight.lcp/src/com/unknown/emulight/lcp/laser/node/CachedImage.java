package com.unknown.emulight.lcp.laser.node;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class CachedImage {
	private final File file;
	private final BufferedImage image;

	public CachedImage(File file) throws IOException {
		this.file = file;
		this.image = ImageIO.read(file);
	}

	public File getFile() {
		return file;
	}

	public BufferedImage getImage() {
		return image;
	}
}
