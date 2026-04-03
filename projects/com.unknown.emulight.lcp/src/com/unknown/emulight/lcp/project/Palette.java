package com.unknown.emulight.lcp.project;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Palette {
	private final List<Color> colors = new ArrayList<>();

	public Palette() {
		initDefault();
	}

	public int getColorCount() {
		return colors.size();
	}

	public Color getColor(int index) {
		if(index < 0 || index >= colors.size()) {
			return colors.get(0);
		} else {
			return colors.get(index);
		}
	}

	public Color getColor(int index, Color defaultColor) {
		if(index < 0 || index >= colors.size()) {
			return defaultColor;
		} else {
			return colors.get(index);
		}
	}

	public void addColor(Color color) {
		colors.add(color);
	}

	public List<Color> getColors() {
		return Collections.unmodifiableList(colors);
	}

	public int getColorIndex(Color color) {
		int i = 0;
		for(Color c : colors) {
			if(c.equals(color)) {
				return i;
			} else {
				i++;
			}
		}
		return -1;
	}

	public void clear() {
		colors.clear();
	}

	public void initDefault() {
		colors.clear();
		colors.add(new Color(233, 64, 62));
		colors.add(new Color(232, 132, 63));
		colors.add(new Color(233, 195, 67));
		colors.add(new Color(218, 236, 86));
		colors.add(new Color(152, 233, 62));
		colors.add(new Color(90, 222, 68));
		colors.add(new Color(61, 226, 106));
		colors.add(new Color(58, 222, 164));
		colors.add(new Color(55, 210, 209));
		colors.add(new Color(73, 178, 237));
		colors.add(new Color(106, 140, 239));
		colors.add(new Color(131, 117, 241));
		colors.add(new Color(170, 96, 238));
		colors.add(new Color(212, 77, 234));
		colors.add(new Color(233, 63, 194));
		colors.add(new Color(233, 63, 135));
	}
}
