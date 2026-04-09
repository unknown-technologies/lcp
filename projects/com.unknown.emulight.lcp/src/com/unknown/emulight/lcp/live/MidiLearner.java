package com.unknown.emulight.lcp.live;

public interface MidiLearner {
	void noteOn(int channel, int key);

	void noteOff(int channel, int key);

	void controller(int channel, int controller);

	void program(int channel, int program);

	void bend(int channel);
}
