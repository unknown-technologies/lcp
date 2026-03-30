package com.unknown.emulight.lcp.laser.node.plugin;

import com.unknown.emulight.lcp.laser.node.Node;

public abstract class CustomNodePlugin {
	private final String id;

	protected CustomNodePlugin(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public String getDisplayName() {
		return id;
	}

	public abstract Node create();
}
