package com.unknown.emulight.lcp.ui.help;

public abstract class HelpNode {
	private final String name;

	protected HelpNode(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return getName();
	}
}
