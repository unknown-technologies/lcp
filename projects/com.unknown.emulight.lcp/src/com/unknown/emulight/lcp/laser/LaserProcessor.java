package com.unknown.emulight.lcp.laser;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import com.unknown.net.shownet.InterfaceId;
import com.unknown.net.shownet.Laser;
import com.unknown.net.shownet.LaserDiscoveryListener;
import com.unknown.net.shownet.LaserInfo;
import com.unknown.net.shownet.ShowNET;
import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;

public class LaserProcessor {
	private static final Logger log = Trace.create(LaserProcessor.class);

	private final ShowNET net;

	private final Timer timer;
	private final int rate;
	private long startTime;
	private long lastTime;

	private final ConcurrentMap<InetAddress, Laser> lasers = new ConcurrentHashMap<>();
	private final ConcurrentMap<InterfaceId, Clip> currentClip = new ConcurrentHashMap<>();

	public LaserProcessor() throws IOException {
		this(60);
	}

	public LaserProcessor(int rate) throws IOException {
		this.rate = rate;

		net = new ShowNET(true);

		startTime = System.currentTimeMillis();
		lastTime = startTime;

		timer = new Timer();
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				try {
					process();
				} catch(Throwable t) {
					log.log(Levels.ERROR, "Exception while processing laser data: " +
							t.getMessage(), t);
				}
			}
		};

		long delay = 1000 / rate;
		log.info("Processing started with interval of " + rate + " Hz (" + delay + " ms)");
		timer.scheduleAtFixedRate(task, delay, delay);
	}

	public int getRate() {
		return rate;
	}

	public void addDiscoveryAddress(InetAddress address) {
		net.addDiscoveryAddress(address);
	}

	public void removeDiscoveryAddress(InetAddress address) {
		net.removeDiscoveryAddress(address);
	}

	public Set<InetAddress> getDiscoveryAddresses() {
		return net.getDiscoveryAddresses();
	}

	public Laser connect(InetAddress address) throws IOException {
		Laser laser = lasers.get(address);
		if(laser != null) {
			return laser;
		} else {
			laser = net.connect(address);
			lasers.put(address, laser);
			return laser;
		}
	}

	public void disconnect(Laser laser) {
		net.disconnect(laser);
		lasers.remove(laser.getAddress());
	}

	public Laser getLaser(InetAddress address) {
		return net.getLaser(address);
	}

	public InetAddress getLaserAddress(InterfaceId id) {
		return net.getLaserAddress(id);
	}

	public Set<Laser> getLasers() {
		return new HashSet<>(lasers.values());
	}

	public Set<LaserInfo> getAvailableLasers() {
		return net.getDiscoveredLasers();
	}

	public void addLaserDiscoveryListener(LaserDiscoveryListener listener) {
		net.addLaserDiscoveryListener(listener);
	}

	public void removeLaserDiscoveryListener(LaserDiscoveryListener listener) {
		net.removeLaserDiscoveryListener(listener);
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime() {
		startTime = System.currentTimeMillis();
	}

	public void setCurrentClip(Laser laser, Clip clip) {
		currentClip.put(laser.getInterfaceId(), clip);
	}

	public void clearCurrentClip(Laser laser) {
		currentClip.remove(laser.getInterfaceId());
	}

	private void process() {
		long now = System.currentTimeMillis();
		int dt = (int) (now - lastTime);
		for(Laser laser : lasers.values()) {
			try {
				Clip clip = currentClip.get(laser.getInterfaceId());
				if(clip != null) {
					laser.sendFrame(clip.render(dt), clip.getSpeed());
				} else {
					laser.sendNop();
				}
			} catch(IOException e) {
				log.log(Levels.ERROR, "Failed to communicate with laser " + laser.getAddress() + ": " +
						e.getMessage());
				net.disconnect(laser);
				lasers.remove(laser.getAddress());
			} catch(Throwable t) {
				log.log(Levels.ERROR, "Unknown error: " + t.getMessage(), t);
				net.disconnect(laser);
				lasers.remove(laser.getAddress());
			}
		}
		lastTime = now;
	}
}
