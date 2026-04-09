package com.unknown.emulight.lcp.laser;

import java.net.InetAddress;

import com.unknown.emulight.lcp.project.EmulightSystem;
import com.unknown.emulight.lcp.project.SystemConfiguration.LaserConfig;
import com.unknown.net.shownet.InterfaceId;
import com.unknown.net.shownet.Laser;

public class LaserReference {
	private final EmulightSystem sys;

	private LaserConfig cfg;
	private String name;

	private Laser laser;

	public LaserReference(EmulightSystem sys, LaserConfig cfg) {
		this.sys = sys;
		this.cfg = cfg;
		this.name = cfg.getName();
	}

	public LaserReference(EmulightSystem sys, String name) {
		this.sys = sys;
		this.cfg = sys.getConfig().getLaser(name); // immediately try to resolve the name
		this.name = name;
	}

	public String getName() {
		if(cfg != null) {
			return cfg.getName();
		} else {
			return name;
		}
	}

	public InterfaceId getInterfaceId() {
		if(cfg == null) {
			cfg = sys.getConfig().getLaser(name);
		}
		if(cfg != null) {
			return cfg.getId();
		} else {
			return null;
		}
	}

	public Laser get() {
		if(laser != null && laser.isConnected()) {
			return laser;
		} else {
			LaserProcessor processor = sys.getLaserProcessor();
			InterfaceId id = getInterfaceId();
			if(id == null) {
				return null;
			}
			InetAddress addr = processor.getLaserAddress(id);
			if(addr != null) {
				laser = processor.getLaser(addr);
				assert laser == null || laser.getInterfaceId() == null ||
						laser.getInterfaceId().equals(cfg.getId());
				return laser;
			} else {
				return null;
			}
		}
	}

	@Override
	public int hashCode() {
		return getName().hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if(o == null || !(o instanceof LaserReference)) {
			return false;
		}

		LaserReference l = (LaserReference) o;
		return l.getName().equals(getName());
	}
}
