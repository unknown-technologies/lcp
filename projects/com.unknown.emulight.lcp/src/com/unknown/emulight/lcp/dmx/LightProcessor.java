package com.unknown.emulight.lcp.dmx;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import com.unknown.emulight.lcp.event.ConfigChangeListener;
import com.unknown.emulight.lcp.laser.LaserReference;
import com.unknown.emulight.lcp.project.EmulightSystem;
import com.unknown.emulight.lcp.project.SystemConfiguration;
import com.unknown.emulight.lcp.project.SystemConfiguration.DMXPortConfig;
import com.unknown.emulight.lcp.project.SystemConfiguration.LaserConfig;
import com.unknown.emulight.lcp.project.SystemConfiguration.MidiPortConfig;
import com.unknown.net.artnet.ArtNetTransmitter;
import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;

public class LightProcessor {
	private static final Logger log = Trace.create(LightProcessor.class);

	private final int rate;

	private final Timer timer;

	private final ArtNetTransmitter artnet;

	private Set<DMXOutPort> dmxPorts = new HashSet<>();

	public LightProcessor(EmulightSystem sys, SystemConfiguration config, int rate) throws IOException {
		this.rate = rate;

		artnet = new ArtNetTransmitter();

		for(DMXPortConfig cfg : config.getDMXPorts()) {
			if(cfg.isActive() && cfg.getAddress() != null) {
				addPort(new ArtDMXPort(artnet, cfg));
			}
		}

		for(LaserConfig cfg : config.getLasers()) {
			if(cfg.isActive()) {
				addPort(new LaserDMXPort(new LaserReference(sys, cfg)));
			}
		}

		timer = new Timer(true);
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				try {
					process();
				} catch(Throwable t) {
					log.log(Levels.ERROR, "Exception while processing DMX data: " +
							t.getMessage(), t);
				}
			}
		};

		long delay = 1000 / rate;
		log.info("Light processing started with interval of " + rate + " Hz (" + delay + " ms)");
		timer.scheduleAtFixedRate(task, delay, delay);

		config.addConfigChangeListener(new ConfigChangeListener() {
			@Override
			public void configChanged(String key, String value) {
				// empty
			}

			@Override
			public void laserChanged(LaserConfig laser) {
				synchronized(dmxPorts) {
					DMXOutPort out = null;
					for(DMXOutPort p : dmxPorts) {
						if(laser.getName().equals(p.getAlias())) {
							out = p;
							break;
						}
					}
					if(laser.isActive()) {
						// add new ArtNet port
						if(out == null) {
							addPort(new LaserDMXPort(new LaserReference(sys, laser)));
						}
					} else {
						// remove deactivated ArtNet port
						if(out != null) {
							dmxPorts.remove(out);
						}
					}
				}
			}

			@Override
			public void midiPortChanged(MidiPortConfig port) {
				// empty
			}

			@Override
			public void dmxPortChanged(DMXPortConfig port) {
				synchronized(dmxPorts) {
					DMXOutPort out = null;
					for(DMXOutPort p : dmxPorts) {
						if(port.getName().equals(p.getAlias())) {
							out = p;
							break;
						}
					}
					if(port.isActive()) {
						// add new ArtNet port
						if(out == null && port.getAddress() != null) {
							addPort(new ArtDMXPort(artnet, port));
						}
					} else {
						// remove deactivated ArtNet port
						if(out != null) {
							dmxPorts.remove(out);
						}
					}
				}
			}
		});
	}

	public int getRate() {
		return rate;
	}

	public void addPort(DMXOutPort port) {
		synchronized(dmxPorts) {
			dmxPorts.add(port);
		}
	}

	public void removePort(DMXOutPort port) {
		synchronized(dmxPorts) {
			dmxPorts.remove(port);
		}
	}

	public Set<DMXOutPort> getPorts() {
		return Collections.unmodifiableSet(dmxPorts);
	}

	private void process() {
		synchronized(dmxPorts) {
			for(DMXOutPort port : dmxPorts) {
				byte[] dmx = new byte[512];
				try {
					port.send(dmx);
				} catch(IOException e) {
					log.log(Levels.ERROR, "Failed to send DMX data: " + e.getMessage());
				}
			}
		}
	}
}
