package com.unknown.emulight.lcp.io.esl;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.unknown.emulight.lcp.io.esl.protocol.ESLPacket;
import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;

public abstract class ESLPHY implements Closeable {
	private static final Logger log = Trace.create(ESLPHY.class);

	private List<PacketHandler> handlers = new ArrayList<>();

	private byte localAddr = -1;

	public void addPacketHandler(PacketHandler handler) {
		handlers.add(handler);
	}

	public void removePacketHandler(PacketHandler handler) {
		handlers.remove(handler);
	}

	protected void handle(ESLPacket packet) {
		for(PacketHandler handler : handlers) {
			try {
				handler.received(packet);
			} catch(Throwable t) {
				log.log(Levels.ERROR, "Packet handler failed to process packet", t);
			}
		}
	}

	protected void setLocalAddress(byte addr) {
		log.info("Local address is " + Byte.toUnsignedInt(addr));
		localAddr = addr;

		for(PacketHandler handler : handlers) {
			try {
				handler.setLocalAddress(addr);
			} catch(Throwable t) {
				log.log(Levels.ERROR, "Packet handler failed to process local address", t);
			}
		}
	}

	public byte getLocalAddress() {
		return localAddr;
	}

	public abstract void open() throws IOException;

	@Override
	public abstract void close() throws IOException;

	public abstract boolean isOpen();

	public abstract boolean isError();

	@SuppressWarnings("unused")
	public void reset() throws IOException {
		// nothing
	}

	public abstract void send(ESLPacket packet) throws IOException;
}
