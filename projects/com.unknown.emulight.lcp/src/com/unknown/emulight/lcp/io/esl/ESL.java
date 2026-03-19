package com.unknown.emulight.lcp.io.esl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import com.unknown.emulight.lcp.io.esl.protocol.ESLDescriptor;
import com.unknown.emulight.lcp.io.esl.protocol.ESLDiskException;
import com.unknown.emulight.lcp.io.esl.protocol.ESLDiskOperationPacket;
import com.unknown.emulight.lcp.io.esl.protocol.ESLLogMessagePacket;
import com.unknown.emulight.lcp.io.esl.protocol.ESLPacket;
import com.unknown.emulight.lcp.io.esl.protocol.ESLRequestPacket;
import com.unknown.emulight.lcp.io.esl.protocol.ESLResponsePacket;
import com.unknown.util.BitTest;
import com.unknown.util.HexFormatter;
import com.unknown.util.io.Endianess;
import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;

public abstract class ESL implements PacketHandler {
	private static final Logger log = Trace.create(ESLInterface.class);

	public static final int NO_ROUTER = -1;
	public static final int DEFAULT_ADDRESS = 0x41;

	private int address = DEFAULT_ADDRESS;
	private int router = NO_ROUTER;

	private byte enumerate = 0;
	private long activeDevices = 0;

	private ESLDescriptor[] descriptors = new ESLDescriptor[64];

	private List<ESLListener> listeners = new ArrayList<>();

	private Map<Integer, CompletableFuture<ESLResponsePacket>> requests = new HashMap<>();
	private Map<Byte, CompletableFuture<ESLDiskOperationPacket>> diskOps = new HashMap<>();

	private byte requestId = 0;
	private byte diskTxId = 0;

	// event handling
	public void addESLListener(ESLListener listener) {
		listeners.add(listener);
	}

	public void removeESLListener(ESLListener listener) {
		listeners.remove(listener);
	}

	protected void fireDeviceListChanged() {
		ESLDescriptor[] desc = getDescriptors();
		for(ESLListener listener : listeners) {
			try {
				listener.deviceListChanged(desc);
			} catch(Throwable t) {
				log.log(Levels.ERROR, "Error while executing listener", t);
			}
		}
	}

	protected void fireLog(ESLLogMessagePacket packet) {
		int src = packet.getSource();
		String message = packet.getMessage();
		for(ESLListener listener : listeners) {
			try {
				listener.log(src, message);
			} catch(Throwable t) {
				log.log(Levels.ERROR, "Error while executing listener", t);
			}
		}
	}

	public CompletableFuture<ESLResponsePacket> sendRequest(int dest, byte request, short param)
			throws IOException {
		byte id;
		synchronized(requests) {
			id = requestId++;
		}

		int reqid = Byte.toUnsignedInt(id) | (Byte.toUnsignedInt(request) << 8);
		CompletableFuture<ESLResponsePacket> future = new CompletableFuture<>();

		synchronized(requests) {
			CompletableFuture<ESLResponsePacket> old = requests.put(reqid, future);
			if(old != null) {
				old.cancel(true);
			}
		}

		request(dest, id, request, param);

		return future;
	}

	private CompletableFuture<ESLDiskOperationPacket> disk(int dest, byte op, byte disk, long offset, byte length)
			throws IOException {
		byte id;
		synchronized(diskOps) {
			id = diskTxId++;
		}

		CompletableFuture<ESLDiskOperationPacket> future = new CompletableFuture<>();

		synchronized(diskOps) {
			CompletableFuture<ESLDiskOperationPacket> old = diskOps.put(id, future);
			if(old != null) {
				old.cancel(true);
			}
		}

		disk(dest, id, op, disk, offset, length);

		return future;
	}

	private CompletableFuture<ESLDiskOperationPacket> disk(int dest, byte op, byte disk, long offset, byte[] data)
			throws IOException {
		byte id;
		synchronized(diskOps) {
			id = diskTxId++;
		}

		CompletableFuture<ESLDiskOperationPacket> future = new CompletableFuture<>();
		synchronized(diskOps) {
			CompletableFuture<ESLDiskOperationPacket> old = diskOps.put(id, future);
			if(old != null) {
				old.cancel(true);
			}
		}

		disk(dest, id, op, disk, offset, data);

		return future;
	}

	public CompletableFuture<ESLDiskOperationPacket> diskRead(int dest, byte disk, long offset, byte length)
			throws IOException {
		return disk(dest, ESLDiskOperationPacket.ESL_SYSTEM_DISK_READ, disk, offset, length);
	}

	public CompletableFuture<ESLDiskOperationPacket> diskWrite(int dest, byte disk, long offset, byte[] data)
			throws IOException {
		return disk(dest, ESLDiskOperationPacket.ESL_SYSTEM_DISK_WRITE, disk, offset, data);
	}

	// PacketHandler
	@Override
	public void received(ESLPacket packet) {
		if(packet instanceof ESLResponsePacket) {
			ESLResponsePacket response = (ESLResponsePacket) packet;

			// internal handlers
			switch(response.getRequest()) {
			case ESLRequestPacket.ESL_SYSTEM_REQ_DEVICE_COUNT:
				activeDevices = Endianess.get64bitLE(response.getData());
				try {
					retrieveDeviceInfo();
				} catch(IOException e) {
					log.log(Levels.ERROR, "Failed to retrieve device info", e);
				}
				break;
			case ESLRequestPacket.ESL_SYSTEM_REQ_DEVICE_INFO:
				try {
					processDeviceInfo(response.getId(), response.getData());
				} catch(IOException e) {
					log.log(Levels.ERROR, "Failed to retrieve device info", e);
				}
				break;
			default:
				// this case is handled by the CompletableFuture
				break;
			}

			int reqid = Byte.toUnsignedInt(response.getId()) |
					(Byte.toUnsignedInt(response.getRequest()) << 8);
			CompletableFuture<ESLResponsePacket> future;
			synchronized(requests) {
				future = requests.remove(reqid);
			}
			if(future != null) {
				future.complete(response);
			} else {
				log.log(Levels.WARNING, "Received ESLResponsePacket with reqid " +
						HexFormatter.tohex(reqid, 4) + ", no corresponding request registered");
			}
		} else if(packet instanceof ESLDiskOperationPacket) {
			ESLDiskOperationPacket disk = (ESLDiskOperationPacket) packet;

			byte txid = disk.getTxId();
			CompletableFuture<ESLDiskOperationPacket> future;
			synchronized(diskOps) {
				future = diskOps.remove(txid);
			}
			if(future != null) {
				switch(disk.getType()) {
				case ESLDiskOperationPacket.ESL_SYSTEM_DISK_READ_DATA:
				case ESLDiskOperationPacket.ESL_SYSTEM_DISK_WRITE_OK:
					future.complete(disk);
					break;
				case ESLDiskOperationPacket.ESL_SYSTEM_DISK_READ_FAIL:
				case ESLDiskOperationPacket.ESL_SYSTEM_DISK_WRITE_FAIL:
				default:
					future.completeExceptionally(new ESLDiskException(disk));
					break;
				}
			} else {
				log.log(Levels.WARNING,
						"Received ESLDiskOperationPacket with txid " +
								HexFormatter.tohex(Byte.toUnsignedInt(txid), 2) +
								", no corresponding request registered");
			}
		} else if(packet instanceof ESLLogMessagePacket) {
			ESLLogMessagePacket msg = (ESLLogMessagePacket) packet;
			fireLog(msg);
		} else {
			log.log(Levels.INFO, "Received unhandled packet: " + packet);
		}
	}

	private byte getNextDevice(byte start) {
		if(start >= 63) {
			return 0;
		}

		for(int i = Byte.toUnsignedInt(start) + 1; i < 64; i++) {
			if(BitTest.test(activeDevices, 1L << i)) {
				return (byte) i;
			}
		}

		return 0;
	}

	private void retrieveDeviceInfo() throws IOException {
		for(int i = 0; i < descriptors.length; i++) {
			descriptors[i] = null;
		}

		log.info("Retrieving device descriptors...");
		if(activeDevices == 0) {
			fireDeviceListChanged();
			return;
		}

		enumerate = getNextDevice((byte) 0);
		if(enumerate == 0) {
			fireDeviceListChanged();
			return;
		}

		log.log(Levels.DEBUG, "Requesting device descriptor for device " + enumerate);
		request(0, enumerate, ESLRequestPacket.ESL_SYSTEM_REQ_DEVICE_INFO,
				(short) Byte.toUnsignedInt(enumerate));
	}

	private void processDeviceInfo(byte id, byte[] data) throws IOException {
		log.log(Levels.DEBUG, "Device info for " + Byte.toUnsignedInt(id));
		ESLDescriptor desc = ESLDescriptor.parse(data);
		descriptors[Byte.toUnsignedInt(id)] = desc;
		log.log(Levels.DEBUG, "Device descriptor: " + desc);

		// request next device info
		enumerate = getNextDevice(enumerate);
		if(enumerate != 0) {
			log.log(Levels.DEBUG, "Requesting device descriptor for device " + enumerate);
			request(0, enumerate, ESLRequestPacket.ESL_SYSTEM_REQ_DEVICE_INFO,
					(short) Byte.toUnsignedInt(enumerate));
		} else {
			fireDeviceListChanged();
		}
	}

	// data queries
	public ESLDescriptor getDescriptor(int addr) {
		return descriptors[addr];
	}

	public long getActiveDevices() {
		return activeDevices;
	}

	public boolean isActiveDevice(int addr) {
		return BitTest.test(activeDevices, 1L << addr);
	}

	public ESLDescriptor[] getDescriptors() {
		int count = Long.bitCount(activeDevices);
		ESLDescriptor[] result = new ESLDescriptor[count];
		for(int i = 0, j = 0; i < 64; i++) {
			if(isActiveDevice(i)) {
				result[j++] = getDescriptor(i);
			}
		}
		return result;
	}

	// commands
	public void enumerateDevices() throws IOException {
		request(getRouter(), (byte) 0, ESLRequestPacket.ESL_SYSTEM_REQ_DEVICE_COUNT, (short) 0);
	}

	public void synchronizeTime() throws IOException {
		setClock(0, new Date());
	}

	// address configuration
	public void setAddress(int address) {
		this.address = address;
	}

	public int getAddress() {
		return address;
	}

	@Override
	public void setLocalAddress(byte addr) {
		setRouter(Byte.toUnsignedInt(addr));

		// now that we know the peer's address, get a list of all connected devices
		try {
			enumerateDevices();
		} catch(IOException e) {
			log.log(Levels.ERROR, "Failed to enumerate devices", e);
		}
	}

	public void setRouter(int address) {
		router = address;
	}

	public int getRouter() {
		return router;
	}

	// packet transmission
	public abstract void setParameter(int dest, byte channel, byte parameter, int value) throws IOException;

	public abstract void sendMIDI(int dest, byte channel, byte status, byte data1, byte data2) throws IOException;

	public abstract void setClock(int dest, Date date) throws IOException;

	public abstract void request(int dest, byte id, byte request, short param) throws IOException;

	public abstract void disk(int dest, byte txid, byte op, byte disk, long offset, byte length) throws IOException;

	public abstract void disk(int dest, byte txid, byte op, byte disk, long offset, byte[] data) throws IOException;

	// info
	public abstract void open() throws IOException;

	public abstract void close() throws IOException;

	public abstract boolean isOpen();

	public abstract boolean isError();

	// reset
	public abstract void reset() throws IOException;
}
