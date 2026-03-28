package com.unknown.emulight.lcp.ui.event;

import java.util.Set;

import com.unknown.emulight.lcp.project.PartContainer;

public interface PartSelectionListener {
	void selectionChanged(Set<PartContainer<?>> part, PartContainer<?> lastPart);
}
