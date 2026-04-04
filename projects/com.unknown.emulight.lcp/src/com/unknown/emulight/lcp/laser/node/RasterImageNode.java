package com.unknown.emulight.lcp.laser.node;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import com.unknown.emulight.lcp.laser.Point3D;
import com.unknown.math.g3d.Mtx44;
import com.unknown.math.g3d.Vec3;

public class RasterImageNode extends Node {
	public static final String TYPE = "raster-image";

	private final Property<CachedImage> image = new Property<>("image", CachedImage.class);
	private final Property<Double> width = new Property<>("width", 0.1, 0.0, 1.0);
	private final Property<Double> height = new Property<>("height", 0.1, 0.0, 1.0);
	private final Property<Integer> edgePoints = new Property<>("edgePoints", 12, 0, 1000);
	private final Property<Double> overscan = new Property<>("overscan", 1.0, 0.0, 100.0);
	private final Property<Double> offset = new Property<>("offset", 0.014, -1.0, 1.0);
	private final Property<Boolean> subsample = new Property<>("subsample", false);

	public RasterImageNode() {
		super(TYPE, true);

		addProperty(image);
		addProperty(width);
		addProperty(height);
		addProperty(edgePoints);
		addProperty(overscan);
		addProperty(offset);
		addProperty(subsample);
	}

	public BufferedImage getImage(int time) {
		CachedImage img = image.getValue(time);
		if(img == null) {
			return null;
		} else {
			return img.getImage();
		}
	}

	public double getWidth(int time) {
		return width.getValue(time);
	}

	public double getHeight(int time) {
		return height.getValue(time);
	}

	public int getEdgePoints(int time) {
		return edgePoints.getValue(time);
	}

	public double getOverscan(int time) {
		return overscan.getValue(time);
	}

	public double getOffset(int time) {
		return offset.getValue(time);
	}

	public boolean isSubsample(int time) {
		return subsample.getValue(time);
	}

	@Override
	protected List<Shape> render(List<Shape> result, int time, Mtx44 positionTransform, Mtx44 colorTransform) {
		Mtx44 positionMtx = positionTransform.concat(getTransformation(time));
		Mtx44 colorMtx = colorTransform.concat(getColorTransformation(time));

		BufferedImage img = getImage(time);
		if(img == null) {
			return result;
		}

		int imgwidth = img.getWidth();
		int imgheight = img.getHeight();
		double w = getWidth(time);
		double h = getHeight(time);
		int edge = getEdgePoints(time);
		double over = getOverscan(time);
		double off = getOffset(time);
		boolean fields = isSubsample(time);

		double pitchX = w / imgwidth;
		double pitchY = h / imgheight;

		List<Point3D> points = new ArrayList<>();

		if(fields) {
			int offY = 0; // this was meant for interlace ... but ShowNET is too crappy for that
			for(int line = 0; line < imgheight; line++) {
				int y = line + offY;
				if(y >= imgheight) {
					break;
				}
				double py = pitchY * (y - imgheight / 2.0);
				for(int i = 0; i < imgwidth; i++) {
					double offX;
					int x;
					if((line % 2) == 1) {
						x = imgwidth - i - 1;
						offX = -off;
					} else {
						x = i;
						offX = off;
					}

					double px = pitchX * (x - imgwidth / 2.0) + offX;
					int color;
					if((line % 2) == 1) {
						color = 0;
					} else {
						color = img.getRGB(x, y);
					}
					int r = (color >> 16) & 0xFF;
					int g = (color >> 8) & 0xFF;
					int b = color & 0xFF;

					Vec3 pos = positionMtx.mult(new Vec3(px, py, 0));
					Vec3 col = colorMtx.mult(new Vec3(r / 255.0, g / 255.0, b / 255.0));

					points.add(new Point3D(pos, col));
				}

				if(y == imgheight - 1) {
					continue;
				}

				for(int i = 0; i <= edge; i++) {
					int x;
					double scan;
					double offX;
					if((line % 2) == 0) {
						x = imgwidth - 1;
						scan = over;
						offX = off;
					} else {
						x = 0;
						scan = -over;
						offX = -off;
					}

					double px = pitchX * (x - imgwidth / 2.0);

					double phi = i / (double) edge;
					double sin = Math.sin(phi * Math.PI);
					double cos = Math.cos(phi * Math.PI);
					double posY = py + phi * pitchY;
					double posX = px + sin * scan * pitchX + offX * cos;

					Vec3 pos = positionMtx.mult(new Vec3(posX, posY, 0));
					Vec3 col = colorMtx.mult(new Vec3(0, 0, 0));

					points.add(new Point3D(pos, col));
				}
			}
		} else {
			for(int y = 0; y < imgheight; y++) {
				double py = pitchY * (y - imgheight / 2.0);
				for(int i = 0; i < imgwidth; i++) {
					double offX;
					int x;
					if((y % 2) == 1) {
						x = imgwidth - i - 1;
						offX = -off;
					} else {
						x = i;
						offX = off;
					}

					double px = pitchX * (x - imgwidth / 2.0) + offX;
					int color = img.getRGB(x, y);
					int r = (color >> 16) & 0xFF;
					int g = (color >> 8) & 0xFF;
					int b = color & 0xFF;

					Vec3 pos = positionMtx.mult(new Vec3(px, py, 0));
					Vec3 col = colorMtx.mult(new Vec3(r / 255.0, g / 255.0, b / 255.0));

					points.add(new Point3D(pos, col));
				}

				if(y == imgheight - 1) {
					continue;
				}

				for(int i = 0; i <= edge; i++) {
					int x;
					double scan;
					double offX;
					if((y % 2) == 0) {
						x = imgwidth - 1;
						scan = over;
						offX = off;
					} else {
						x = 0;
						scan = -over;
						offX = -off;
					}

					double px = pitchX * (x - imgwidth / 2.0);

					double phi = i / (double) edge;
					double sin = Math.sin(phi * Math.PI);
					double cos = Math.cos(phi * Math.PI);
					double posY = py + phi * pitchY;
					double posX = px + sin * scan * pitchX + offX * cos;

					Vec3 pos = positionMtx.mult(new Vec3(posX, posY, 0));
					Vec3 col = colorMtx.mult(new Vec3(0, 0, 0));

					points.add(new Point3D(pos, col));
				}
			}
		}

		result.add(new Shape(this, points));

		return result;
	}

	@Override
	public RasterImageNode clone() {
		RasterImageNode node = new RasterImageNode();
		copy(node);
		return node;
	}
}
