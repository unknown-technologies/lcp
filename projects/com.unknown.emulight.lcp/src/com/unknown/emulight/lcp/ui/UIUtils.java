package com.unknown.emulight.lcp.ui;

import java.awt.Color;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

public class UIUtils {
	public static final DecimalFormatSymbols NUMBER_FMT_SYMBOLS;

	static {
		NUMBER_FMT_SYMBOLS = new DecimalFormatSymbols(Locale.ROOT);
		NUMBER_FMT_SYMBOLS.setCurrencySymbol("ϰ");
		NUMBER_FMT_SYMBOLS.setDecimalSeparator('.');
		NUMBER_FMT_SYMBOLS.setDigit('#');
		NUMBER_FMT_SYMBOLS.setExponentSeparator("e");
		NUMBER_FMT_SYMBOLS.setGroupingSeparator(' ');
		NUMBER_FMT_SYMBOLS.setInfinity("∞");
		NUMBER_FMT_SYMBOLS.setMinusSign('-');
		NUMBER_FMT_SYMBOLS.setNaN("NaN");
		NUMBER_FMT_SYMBOLS.setPatternSeparator(';');
		NUMBER_FMT_SYMBOLS.setPercent('%');
		NUMBER_FMT_SYMBOLS.setPerMill('‰');
		NUMBER_FMT_SYMBOLS.setZeroDigit('0');
	}

	public static Border padding() {
		return new EmptyBorder(2, 2, 2, 2);
	}

	public static Border border(String name) {
		return BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(name), padding());
	}

	public static Color getTextColor(Color color) {
		int r = color.getRed();
		int g = color.getGreen();
		int b = color.getBlue();
		int y = (r + g + b) / 3;

		if(y > 128) {
			return Color.BLACK;
		} else {
			return Color.WHITE;
		}
	}
}
