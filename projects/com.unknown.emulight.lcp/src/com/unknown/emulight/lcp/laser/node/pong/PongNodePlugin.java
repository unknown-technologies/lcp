package com.unknown.emulight.lcp.laser.node.pong;

import com.unknown.emulight.lcp.laser.node.Node;
import com.unknown.emulight.lcp.laser.node.plugin.CustomNodePlugin;

public class PongNodePlugin extends CustomNodePlugin {
	public static final String ID = "pong";

	public PongNodePlugin() {
		super(ID);
	}

	@Override
	public String getDisplayName() {
		return "Laser Pong";
	}

	@Override
	public Node create() {
		return new PongNode();
	}
}
