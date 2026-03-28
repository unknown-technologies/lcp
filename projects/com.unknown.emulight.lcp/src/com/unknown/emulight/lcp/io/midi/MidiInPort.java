package com.unknown.emulight.lcp.io.midi;

import java.util.logging.Logger;

import javax.sound.midi.MidiDevice.Info;
import javax.sound.midi.MidiUnavailableException;

import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;

public abstract class MidiInPort extends MidiPort {
	private static final Logger log = Trace.create(MidiInPort.class);

	protected MidiInPort(MidiRouter router) {
		super(router);
	}

	public void setAll(boolean all) {
		getConfig().setAll(all);
	}

	public boolean isAll() {
		return getConfig().isAll();
	}

	public abstract Info getInfo();

	public abstract void openDevice() throws MidiUnavailableException;

	public abstract void closeDevice();

	@Override
	public void setActive(boolean active) {
		super.setActive(active);

		try {
			if(active) {
				openDevice();
			} else {
				closeDevice();
			}
		} catch(MidiUnavailableException e) {
			log.log(Levels.WARNING, "Failed to " + (active ? "open" : "close") + " MIDI device", e);
		}
	}

}
