package com.unknown.emulight.lcp.laser;

import java.net.InetAddress;

import com.unknown.net.shownet.InterfaceId;
import com.unknown.net.shownet.Laser;

public class LaserReference {
	private final Project project;

	private final InterfaceId interfaceId;

	private String name;

	private Laser laser;

	public LaserReference(Project project, InterfaceId interfaceId) {
		this.project = project;
		this.interfaceId = interfaceId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public InterfaceId getInterfaceId() {
		return interfaceId;
	}

	public Laser get() {
		if(laser != null) {
			return laser;
		} else {
			LaserProcessor processor = project.getProcessor();
			InetAddress addr = processor.getLaserAddress(interfaceId);
			if(addr != null) {
				laser = processor.getLaser(addr);
				assert laser.getInterfaceId().equals(interfaceId);
				return laser;
			} else {
				return null;
			}
		}
	}
}
