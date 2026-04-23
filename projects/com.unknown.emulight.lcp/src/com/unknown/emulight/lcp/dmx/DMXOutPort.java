package com.unknown.emulight.lcp.dmx;

import java.io.IOException;

public abstract class DMXOutPort extends DMXPort {
	abstract void send(byte[] data) throws IOException;
}
