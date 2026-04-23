package com.unknown.emulight.lcp.dmx;

public abstract class DMXPort {
	public abstract String getName();

	public abstract String getAlias();

	public abstract void setAlias(String alias);

	public String getDisplayName() {
		return getAlias() == null ? getName() : getAlias();
	}

	public abstract boolean isActive();

	public abstract void setActive(boolean active);
}
