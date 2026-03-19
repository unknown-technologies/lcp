package com.unknown.emulight.lcp.ui.event;

import com.unknown.emulight.lcp.sequencer.Note;

public interface PreviewListener {
	void pressed(Note note);

	void released(Note note);
}
