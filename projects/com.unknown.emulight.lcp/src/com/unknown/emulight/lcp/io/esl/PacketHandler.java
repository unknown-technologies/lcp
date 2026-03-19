package com.unknown.emulight.lcp.io.esl;

import com.unknown.emulight.lcp.io.esl.protocol.ESLPacket;

public interface PacketHandler {
	void received(ESLPacket packet);

	void setLocalAddress(byte addr);
}
