package com.unknown.emulight.lcp.io.esl;

import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.unknown.emulight.lcp.io.esl.protocol.ESLPacket;
import com.unknown.platform.serial.RS232;
import com.unknown.util.HexFormatter;
import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;

public class SerialInterface extends ESLPHY implements Closeable {
	private static final Logger log = Trace.create(SerialInterface.class);

	private static final byte SC_CMD_PING = 0;
	private static final byte SC_CMD_PONG = 1;
	private static final byte SC_CMD_REQ_ADDR = 2;
	private static final byte SC_CMD_RESP_ADDR = 3;
	private static final byte SC_CMD_RESET = 6;
	private static final byte SC_CMD_ERROR = -1;

	private static final int STATE_NORMAL = 0;
	private static final int STATE_DLE = 1;

	private String line;

	private RS232 rs232;
	private OutputStream rs232out;

	private byte[] buffer = new byte[128];
	private int bufptr = 0;
	private int state = STATE_NORMAL;

	private Thread reader;

	private boolean error;

	public SerialInterface(String line) {
		this.line = line;
		error = false;
	}

	public void setLine(String line) {
		if(isOpen()) {
			throw new IllegalStateException("line is still open");
		}

		this.line = line;
	}

	public String getLine() {
		return line;
	}

	@Override
	public void open() throws IOException {
		if(line == null) {
			throw new IllegalStateException("no line configured");
		}

		log.log(Levels.INFO, "Opening serial line...");
		try {
			try {
				rs232 = new RS232(line, 115200, RS232.FORMAT_8N1_HWFLOW);
				rs232out = null;
			} catch(UnsatisfiedLinkError e) {
				log.log(Levels.WARNING,
						"Cannot use native serial line implementation: " + e.getMessage());
				rs232 = null;
				rs232out = new FileOutputStream(line);
			}

			reader = new Thread() {
				@Override
				public void run() {
					while(!Thread.interrupted()) {
						try {
							process();
						} catch(Throwable t) {
							log.log(Levels.ERROR,
									"Failed to receive data via RS232: " +
											t.getMessage(),
									t);
						}
					}
				}
			};
			reader.start();

			// set MCP into SC mode
			write((byte) 0x90);

			// request local address
			send(SC_CMD_REQ_ADDR);

			error = false;
		} catch(Throwable t) {
			error = true;
			throw t;
		}
	}

	@Override
	public void close() throws IOException {
		if(!isOpen()) {
			return;
		}

		log.log(Levels.INFO, "Closing serial line...");

		if(reader != null) {
			reader.interrupt();
		}

		IOException exception = null;

		// reset MCP to text mode
		try {
			write((byte) 0x9C);
		} catch(IOException e) {
			exception = e;
			log.log(Levels.ERROR, "Failed to reset MCP", e);
		}

		try {
			if(rs232 != null) {
				rs232.close();
				rs232 = null;
			} else {
				rs232out.close();
				rs232out = null;
			}
		} catch(IOException e) {
			exception = e;
			log.log(Levels.ERROR, "Failed to close RS232 line", e);
		}

		try {
			if(reader != null) {
				reader.join();
			}
			reader = null;
		} catch(InterruptedException e) {
			// nothing
		}

		if(exception != null) {
			error = true;
			throw exception;
		}
	}

	@Override
	public boolean isOpen() {
		return reader != null && (rs232 != null || rs232out != null);
	}

	@Override
	public boolean isError() {
		return error;
	}

	public void ping() throws IOException {
		send(SC_CMD_PING);
	}

	@Override
	public void reset() throws IOException {
		send(SC_CMD_RESET);
	}

	private void process() throws IOException {
		byte[] buf = new byte[1];
		int n = read(buf);
		if(n > 0) {
			for(int i = 0; i < n; i++) {
				rx(buf[i]);
			}
		}
	}

	private void rx(byte c) {
		switch(state) {
		case STATE_NORMAL:
			switch(c) {
			case 0x10: /* DLE */
				state = STATE_DLE;
				break;
			case 0x02: /* STX */
				bufptr = 0;
				break;
			case 0x03: /* ETX */
				process(bufptr);
				bufptr = 0;
				break;
			default:
				if(bufptr < buffer.length) {
					buffer[bufptr++] = c;
				}
				break;
			}
			break;
		case STATE_DLE:
			if(bufptr < buffer.length) {
				switch(c) {
				case 0x12: /* escaped STX */
					buffer[bufptr++] = 0x02;
					break;
				case 0x13: /* escaped ETX */
					buffer[bufptr++] = 0x03;
					break;
				case 0x20: /* escaped DLE */
					buffer[bufptr++] = 0x10;
					break;
				case 0x4C: /* escaped ST */
					buffer[bufptr++] = (byte) 0x9C;
					break;
				case 0x40:
					buffer[bufptr++] = (byte) 0x90;
					break;
				default:
					buffer[bufptr++] = c;
					break;
				}
			}
			state = STATE_NORMAL;
			break;
		}
	}

	private void process(int length) {
		log.log(Levels.DEBUG, "Received " + length + " bytes");
		log.log(Levels.DEBUG, () -> "Received message: " + IntStream.range(0, length)
				.map(i -> Byte.toUnsignedInt(buffer[i]))
				.mapToObj(x -> HexFormatter.tohex(x, 2))
				.collect(Collectors.joining(" ")));

		if(length <= 2) {
			if(length == 0) {
				return;
			}

			switch(buffer[0]) {
			case SC_CMD_PONG:
				log.info("Received SC_CMD_PONG");
				return;
			case SC_CMD_RESP_ADDR:
				if(length == 2) {
					setLocalAddress(buffer[1]);
					return;
				} else {
					log.log(Levels.WARNING,
							"Received SC_CMD_RESP_ADDR of length 1, expected length 2");
				}
				return;
			case SC_CMD_ERROR:
				log.log(Levels.WARNING, "Received SC_CMD_ERROR");
				return;
			default:
				log.log(Levels.WARNING, "Received unknown SC command 0x" +
						HexFormatter.tohex(Byte.toUnsignedInt(buffer[0]), 2));
				return;
			}
		}

		byte[] data = Arrays.copyOf(buffer, length);
		try {
			handle(ESLPacket.parse(data));
		} catch(IOException e) {
			log.log(Levels.ERROR, "Failed to parse ESL packet", e);
		}
	}

	@Override
	public void send(ESLPacket packet) throws IOException {
		send(packet.write());
	}

	private void send(byte... data) throws IOException {
		if(data.length > 128) {
			throw new IOException("message too long");
		}

		int j = 0;
		byte[] buf = new byte[data.length * 2 + 2];

		buf[j++] = 0x02; // STX
		for(int i = 0; i < data.length; i++) {
			if(data[i] == 0x02 || data[i] == 0x03 || data[i] == 0x10) { // escape STX/ETX/DLE
				buf[j++] = 0x10; // DLE
				buf[j++] = (byte) (data[i] + 0x10);
			} else if(data[i] == (byte) 0x90) { // escape DCS
				buf[j++] = 0x10; // DLE
				buf[j++] = 0x40;
			} else if(data[i] == (byte) 0x9C) { // escape ST
				buf[j++] = 0x10; // DLE
				buf[j++] = 0x4C;
			} else {
				buf[j++] = data[i];
			}
		}
		buf[j++] = 0x03; // ETX

		write(buf, 0, j);
	}

	private void write(byte... buf) throws IOException {
		write(buf, 0, buf.length);
	}

	private void write(byte[] buf, int off, int len) throws IOException {
		log.log(Levels.DEBUG, () -> "Transmitting data: " + IntStream.range(0, len)
				.map(i -> Byte.toUnsignedInt(buf[i]))
				.mapToObj(x -> HexFormatter.tohex(x, 2))
				.collect(Collectors.joining(" ")));

		if(rs232 != null) {
			int written = rs232.write(buf, off, len);
			while(written < len) {
				// try again if there was a short write
				int wr = rs232.write(buf, off + written, len - written);
				if(wr == 0) {
					try {
						Thread.sleep(1);
					} catch(InterruptedException e) {
						// bad idea, but swallow
					}
				} else {
					written += wr;
				}
			}
		} else {
			rs232out.write(buf, off, len);
			rs232out.flush();
		}
	}

	private int read(byte[] data) throws IOException {
		return rs232.read(data, 0, data.length);
	}
}
