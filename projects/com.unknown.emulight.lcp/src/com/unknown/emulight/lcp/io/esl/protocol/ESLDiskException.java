package com.unknown.emulight.lcp.io.esl.protocol;

@SuppressWarnings("serial")
public class ESLDiskException extends Exception {
	private ESLDiskOperationPacket packet;

	public ESLDiskException(ESLDiskOperationPacket packet) {
		super(packet.getDisplayType());
		this.packet = packet;
	}

	public ESLDiskOperationPacket getPacket() {
		return packet;
	}
}
