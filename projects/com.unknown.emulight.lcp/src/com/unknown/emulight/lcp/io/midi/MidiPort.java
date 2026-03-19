package com.unknown.emulight.lcp.io.midi;

import com.unknown.emulight.lcp.project.SystemConfiguration.MidiPortConfig;

public abstract class MidiPort {
	protected final MidiRouter router;

	protected MidiPort(MidiRouter router) {
		this.router = router;
	}

	public abstract String getName();

	protected MidiPortConfig getConfig() {
		return router.getPortConfig(this);
	}

	public String getAlias() {
		return getConfig().getAlias();
	}

	public void setAlias(String alias) {
		getConfig().setAlias(alias);
	}

	public String getDisplayName() {
		return getAlias() == null ? getName() : getAlias();
	}

	public boolean isActive() {
		return getConfig().isActive();
	}

	public void setActive(boolean active) {
		getConfig().setActive(active);
	}
}
