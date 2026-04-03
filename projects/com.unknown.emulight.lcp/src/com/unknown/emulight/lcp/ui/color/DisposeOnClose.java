package com.unknown.emulight.lcp.ui.color;

import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.Serializable;

@SuppressWarnings("serial")
public class DisposeOnClose extends ComponentAdapter implements Serializable {
	@Override
	public void componentHidden(ComponentEvent e) {
		Window w = (Window) e.getComponent();
		w.dispose();
	}
}
