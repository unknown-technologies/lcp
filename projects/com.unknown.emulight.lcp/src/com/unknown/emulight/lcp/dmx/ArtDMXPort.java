package com.unknown.emulight.lcp.dmx;

import java.io.IOException;
import java.net.InetAddress;

import com.unknown.emulight.lcp.project.SystemConfiguration.DMXPortConfig;
import com.unknown.net.artnet.ArtDMXPacket;
import com.unknown.net.artnet.ArtNetTransmitter;

public class ArtDMXPort extends DMXOutPort {
	private final ArtNetTransmitter tx;
	private final DMXPortConfig cfg;
	private InetAddress target;

	protected ArtDMXPort(ArtNetTransmitter tx, DMXPortConfig cfg) {
		this.tx = tx;
		this.cfg = cfg;
		target = cfg.getAddress();
	}

	protected DMXPortConfig getConfig() {
		return cfg;
	}

	public int getUniverse() {
		return getConfig().getUniverse();
	}

	public int getPhysicalPort() {
		return 0;
	}

	@Override
	public String getAlias() {
		return getConfig().getName();
	}

	@Override
	public void setAlias(String alias) {
		getConfig().setName(alias);
	}

	@Override
	public boolean isActive() {
		return getConfig().isActive();
	}

	@Override
	public void setActive(boolean active) {
		getConfig().setActive(active);
	}

	@Override
	void send(byte[] data) throws IOException {
		tx.send(target, new ArtDMXPacket(getUniverse(), getPhysicalPort(), data));
	}

	@Override
	public String getName() {
		return target.getHostAddress() + ":" + getUniverse() + "/" + getPhysicalPort();
	}
}
