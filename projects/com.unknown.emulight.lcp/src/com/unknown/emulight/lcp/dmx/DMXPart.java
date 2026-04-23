package com.unknown.emulight.lcp.dmx;

import java.io.IOException;

import com.unknown.emulight.lcp.project.AbstractPart;
import com.unknown.xml.dom.Element;

public class DMXPart extends AbstractPart {
	@Override
	public long getLength() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void read(Element xml) throws IOException {
		// TODO Auto-generated method stub
	}

	@Override
	public void write(Element xml) {
		// TODO Auto-generated method stub
	}

	@Override
	public DMXPart clone() {
		DMXPart part = new DMXPart();
		copy(part);
		return part;
	}
}
