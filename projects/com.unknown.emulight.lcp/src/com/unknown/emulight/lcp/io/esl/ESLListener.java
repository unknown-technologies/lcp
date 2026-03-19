package com.unknown.emulight.lcp.io.esl;

import com.unknown.emulight.lcp.io.esl.protocol.ESLDescriptor;

public interface ESLListener {
	void deviceListChanged(ESLDescriptor[] descriptors);

	void log(int address, String message);
}
