package com.unknown.emulight.lcp.io.midi;

public abstract class MidiInPort extends MidiPort {
	protected MidiInPort(MidiRouter router) {
		super(router);
	}

	public void setAll(boolean all) {
		getConfig().setAll(all);
	}

	public boolean isAll() {
		return getConfig().isAll();
	}
}
