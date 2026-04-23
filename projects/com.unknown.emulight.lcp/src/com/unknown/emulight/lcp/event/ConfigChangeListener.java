package com.unknown.emulight.lcp.event;

import com.unknown.emulight.lcp.project.SystemConfiguration.DMXPortConfig;
import com.unknown.emulight.lcp.project.SystemConfiguration.LaserConfig;
import com.unknown.emulight.lcp.project.SystemConfiguration.MidiPortConfig;

public interface ConfigChangeListener {
	void configChanged(String key, String value);

	void laserChanged(LaserConfig laser);

	void midiPortChanged(MidiPortConfig port);

	void dmxPortChanged(DMXPortConfig port);
}
