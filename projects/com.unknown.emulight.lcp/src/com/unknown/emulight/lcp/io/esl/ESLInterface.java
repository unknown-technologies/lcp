package com.unknown.emulight.lcp.io.esl;

import java.io.IOException;
import java.util.Date;

import com.unknown.emulight.lcp.io.esl.protocol.ESLDiskOperationPacket;
import com.unknown.emulight.lcp.io.esl.protocol.ESLGrePacket;
import com.unknown.emulight.lcp.io.esl.protocol.ESLMidiPacket;
import com.unknown.emulight.lcp.io.esl.protocol.ESLPacket;
import com.unknown.emulight.lcp.io.esl.protocol.ESLParameterPacket;
import com.unknown.emulight.lcp.io.esl.protocol.ESLRequestPacket;
import com.unknown.emulight.lcp.io.esl.protocol.ESLSetClockPacket;
import com.unknown.emulight.lcp.io.esl.protocol.ESLSystemPacket;

public class ESLInterface extends ESL {
	private ESLPHY phy;

	public ESLInterface(ESLPHY phy) {
		this(phy, DEFAULT_ADDRESS, NO_ROUTER);
	}

	public ESLInterface(ESLPHY phy, int address, int router) {
		this.phy = phy;
		setRouter(router);
		setAddress(address);
		phy.addPacketHandler(this);
	}

	private byte getDestination(int dest) {
		if(getRouter() == NO_ROUTER) {
			return (byte) (dest & ESLPacket.ESL_ADDR_MASK);
		} else {
			return (byte) getRouter();
		}
	}

	private static byte getL1Destination(int dest) {
		return (byte) (dest & ESLPacket.ESL_ADDR_MASK);
	}

	private ESLPacket encapsulate(ESLPacket packet, int dest) {
		if(packet instanceof ESLSystemPacket) {
			// is already a system packet, no need for encapsulation
			return packet;
		} else if(dest == 0) {
			return packet;
		} else if((dest & ESLSystemPacket.ESL_ADDR_MASK) == 0) {
			// destination is a broadcast address
			return new ESLGrePacket((byte) 0, getAddress(), 0, packet.write());
		} else {
			// assume getRouter returns the final router
			// TODO: perform routing and fill in the correct final router
			return new ESLGrePacket((byte) getRouter(), (byte) 0, getAddress(), getRouter(),
					packet.write());
		}
	}

	@Override
	public void setParameter(int dest, byte channel, byte parameter, int value) throws IOException {
		phy.send(encapsulate(new ESLParameterPacket(getL1Destination(dest), channel, parameter, value), dest));
	}

	@Override
	public void sendMIDI(int dest, byte channel, byte status, byte data1, byte data2) throws IOException {
		phy.send(encapsulate(new ESLMidiPacket(getL1Destination(dest), channel, status, data1, data2), dest));
	}

	@Override
	public void setClock(int dest, Date date) throws IOException {
		phy.send(new ESLSetClockPacket((byte) 0, getAddress(), dest, date));
	}

	@Override
	public void request(int dest, byte id, byte request, short param) throws IOException {
		phy.send(new ESLRequestPacket(getDestination(dest), (byte) 0, getAddress(), dest, request, id, param));
	}

	@Override
	public void disk(int dest, byte txid, byte op, byte disk, long offset, byte length) throws IOException {
		phy.send(new ESLDiskOperationPacket(getDestination(dest), (byte) 0, getAddress(), dest, txid, op, disk,
				offset, length, null));
	}

	@Override
	public void disk(int dest, byte txid, byte op, byte disk, long offset, byte[] data) throws IOException {
		phy.send(new ESLDiskOperationPacket(getDestination(dest), (byte) 0, getAddress(), dest, txid, op, disk,
				offset, (byte) data.length, data));
	}

	@Override
	public void open() throws IOException {
		phy.open();
	}

	@Override
	public void close() throws IOException {
		phy.close();
	}

	@Override
	public boolean isOpen() {
		return phy.isOpen();
	}

	@Override
	public boolean isError() {
		return phy.isError();
	}

	@Override
	public void reset() throws IOException {
		phy.reset();
		enumerateDevices();
	}
}
