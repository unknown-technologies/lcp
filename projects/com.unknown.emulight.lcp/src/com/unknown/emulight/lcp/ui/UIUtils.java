package com.unknown.emulight.lcp.ui;

import java.awt.Color;
import java.awt.Component;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.colorchooser.AbstractColorChooserPanel;

import com.unknown.emulight.lcp.project.Palette;
import com.unknown.emulight.lcp.project.Project;
import com.unknown.emulight.lcp.ui.color.ColorTracker;
import com.unknown.emulight.lcp.ui.color.DisposeOnClose;
import com.unknown.emulight.lcp.ui.color.PaletteChooserPanel;
import com.unknown.emulight.lcp.ui.color.PaletteSwatchChooserPanel;
import com.unknown.emulight.lcp.ui.color.SwatchChooserPanel;

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

	public static Color showColorChooser(Component component, String title, Color initialColor, Palette palette,
			Color[] colors, Project project) {
		JColorChooser chooser = new JColorChooser(initialColor);

		chooser.addChooserPanel(new PaletteChooserPanel(palette));
		chooser.addChooserPanel(new PaletteSwatchChooserPanel(colors));

		AbstractColorChooserPanel[] panels = chooser.getChooserPanels();
		panels[0] = new SwatchChooserPanel(project);
		chooser.setChooserPanels(panels);

		for(AbstractColorChooserPanel panel : chooser.getChooserPanels()) {
			panel.setColorTransparencySelectionEnabled(false);
		}

		ColorTracker ok = new ColorTracker(chooser);
		JDialog dialog = JColorChooser.createDialog(component, title, true, chooser, ok, null);
		dialog.setResizable(false);

		dialog.addComponentListener(new DisposeOnClose());
		dialog.setVisible(true);

		return ok.getColor();
	}

	public static Color showPaletteColorChooser(Component component, String title, Color initialColor,
			Palette palette) {
		JColorChooser chooser = new JColorChooser(initialColor);

		AbstractColorChooserPanel[] panels = new AbstractColorChooserPanel[] {
				new PaletteChooserPanel(palette)
		};
		chooser.setChooserPanels(panels);

		for(AbstractColorChooserPanel panel : chooser.getChooserPanels()) {
			panel.setColorTransparencySelectionEnabled(false);
		}

		ColorTracker ok = new ColorTracker(chooser);
		JDialog dialog = JColorChooser.createDialog(component, title, true, chooser, ok, null);
		dialog.setResizable(false);

		dialog.addComponentListener(new DisposeOnClose());
		dialog.setVisible(true);

		return ok.getColor();
	}
}
