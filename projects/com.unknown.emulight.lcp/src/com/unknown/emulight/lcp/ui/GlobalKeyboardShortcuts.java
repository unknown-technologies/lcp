package com.unknown.emulight.lcp.ui;

import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import com.unknown.emulight.lcp.project.Project;

public class GlobalKeyboardShortcuts {
	public static void create(Project project) {
		project.addKeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0),
				e -> project.playbackToggle());
		project.addKeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0, 0),
				e -> project.playbackStop());
		project.addKeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD1, 0),
				e -> project.playbackPositionReset());
	}
}
