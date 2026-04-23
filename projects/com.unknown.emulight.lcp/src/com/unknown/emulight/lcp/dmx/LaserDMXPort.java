package com.unknown.emulight.lcp.dmx;

import java.io.IOException;

import com.unknown.emulight.lcp.laser.LaserReference;
import com.unknown.net.shownet.Laser;

public class LaserDMXPort extends DMXOutPort {
	private final LaserReference laser;

	public LaserDMXPort(LaserReference laser) {
		this.laser = laser;
	}

	@Override
	void send(byte[] data) throws IOException {
		Laser l = laser.get();
		if(l != null && l.isConnected()) {
			l.sendDMX(data);
		}
	}

	@Override
	public String getName() {
		return laser.getInterfaceId().toString();
	}

	@Override
	public String getAlias() {
		return laser.getName();
	}

	@Override
	public void setAlias(String alias) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isActive() {
		return true;
	}

	@Override
	public void setActive(boolean active) {
		// ignored
	}
}
