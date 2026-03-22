package com.unknown.emulight.lcp.laser;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import com.unknown.emulight.lcp.project.SystemConfiguration;
import com.unknown.emulight.lcp.project.SystemConfiguration.LaserConfig;
import com.unknown.net.shownet.InterfaceId;
import com.unknown.net.shownet.Laser;
import com.unknown.net.shownet.LaserConnectionListener;
import com.unknown.net.shownet.LaserDiscoveryListener;
import com.unknown.net.shownet.LaserInfo;
import com.unknown.net.shownet.Point;
import com.unknown.net.shownet.ShowNET;
import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;

public class LaserProcessor {
	private static final Logger log = Trace.create(LaserProcessor.class);

	private final ShowNET net;

	private final Timer timer;
	private final int rate;
	private long startTime;

	private final ConcurrentMap<InterfaceId, ClipRef> currentClip = new ConcurrentHashMap<>();

	private final SystemConfiguration config;

	private final Timer connectTimer;

	private LaserRenderer renderer;

	public LaserProcessor(SystemConfiguration config, int rate) throws IOException {
		this.config = config;
		this.rate = rate;

		net = new ShowNET(true);

		startTime = System.currentTimeMillis();

		timer = new Timer(true);
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				try {
					LaserRenderer r = renderer;
					if(r != null) {
						r.render();
					} else {
						process();
					}
				} catch(Throwable t) {
					log.log(Levels.ERROR, "Exception while processing laser data: " +
							t.getMessage(), t);
				}
			}
		};

		long delay = 1000 / rate;
		log.info("Laser processing started with interval of " + rate + " Hz (" + delay + " ms)");
		timer.scheduleAtFixedRate(task, delay, delay);

		connectTimer = new Timer(true);
		TimerTask connectTask = new TimerTask() {
			@Override
			public void run() {
				for(LaserInfo info : getAvailableLasers()) {
					Laser laser = getLaser(info.getAddress());
					LaserConfig cfg = config.getLaser(info.getInterfaceId());
					if(cfg != null) {
						if(laser == null && cfg.isActive()) {
							// try to connect
							try {
								connect(info.getAddress());
							} catch(Throwable t) {
								log.info("Failed to connect to laser: " +
										t.getMessage());
							}
						} else if(laser != null && !cfg.isActive()) {
							disconnect(laser);
						}
					}
				}
			}
		};
		connectTimer.schedule(connectTask, 2000, 2000);

		net.addLaserDiscoveryListener(new LaserDiscoveryListener() {
			@Override
			public void laserLost(LaserInfo info) {
				InetAddress addr = info.getAddress();
				if(addr != null) {
					Laser laser = getLaser(addr);
					disconnect(laser);
				}
			}

			@Override
			public void laserDiscovered(LaserInfo info) {
				tryConnect(info);
			}
		});
	}

	private void tryConnect(LaserInfo info) {
		InterfaceId id = info.getInterfaceId();
		if(id != null) {
			LaserConfig cfg = config.getLaser(id);
			if(cfg != null && cfg.isActive()) {
				Laser laser = net.getLaser(info.getAddress());
				if(laser != null && laser.isConnected()) {
					// laser is already connected
					return;
				}
				try {
					connect(info.getAddress());
				} catch(IOException e) {
					log.info("Failed to connect to laser: " + e.getMessage());
				}
			}
		}
	}

	public int getRate() {
		return rate;
	}

	public void setRenderer(LaserRenderer renderer) {
		this.renderer = renderer;
	}

	public void resetAll() {
		for(Laser laser : getLasers()) {
			try {
				laser.sendFrame(List.of(new Point()), 1000);
			} catch(IOException e) {
				log.log(Levels.WARNING, "Failed to send empty frame: " + e.getMessage(), e);
			}
		}
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
		Laser laser = net.getLaser(address);
		if(laser != null) {
			return laser;
		} else {
			return net.connect(address);
		}
	}

	public void disconnect(Laser laser) {
		net.disconnect(laser);
	}

	public Laser getLaser(InetAddress address) {
		return net.getLaser(address);
	}

	public InetAddress getLaserAddress(InterfaceId id) {
		return net.getLaserAddress(id);
	}

	public Set<Laser> getLasers() {
		return new HashSet<>(net.getConnectedLasers());
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

	public void addLaserConnectionListener(LaserConnectionListener listener) {
		net.addLaserConnectionListener(listener);
	}

	public void removeLaserConnectionListener(LaserConnectionListener listener) {
		net.removeLaserConnectionListener(listener);
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime() {
		startTime = System.currentTimeMillis();
	}

	public void setCurrentClip(Laser laser, LaserPart clip) {
		long now = System.currentTimeMillis();
		currentClip.put(laser.getInterfaceId(), new ClipRef(now, clip));
	}

	public void clearCurrentClip(Laser laser) {
		currentClip.remove(laser.getInterfaceId());
	}

	public boolean hasCurrentClip(Laser laser) {
		return currentClip.containsKey(laser.getInterfaceId());
	}

	private void process() {
		long now = System.currentTimeMillis();
		for(Laser laser : getLasers()) {
			if(!laser.isConnected()) {
				continue;
			}
			try {
				ClipRef ref = currentClip.get(laser.getInterfaceId());
				if(ref != null) {
					LaserPart clip = ref.clip;
					int t = (int) (now - ref.startTime);
					if(t > clip.getLength()) {
						if(clip.isLoop()) {
							t %= clip.getLength();
							laser.sendFrame(clip.render(t), clip.getSpeed());
						} else {
							currentClip.remove(laser.getInterfaceId());
							laser.sendNop();
						}
					} else {
						laser.sendFrame(clip.render(t), clip.getSpeed());
					}
				} else {
					laser.sendNop();
				}
			} catch(IOException e) {
				log.log(Levels.ERROR, "Failed to communicate with laser " + laser.getAddress() + ": " +
						e.getMessage());
				net.disconnect(laser);
			} catch(Throwable t) {
				log.log(Levels.ERROR, "Unknown error: " + t.getMessage(), t);
				net.disconnect(laser);
			}
		}
	}

	public void shutdown() {
		timer.cancel();
		for(Laser laser : getLasers()) {
			try {
				laser.sendFrame(List.of(new Point()), 1000);
			} catch(IOException e) {
				log.log(Levels.ERROR, "Failed to communicate with laser " + laser.getAddress() + ": " +
						e.getMessage());
			}

			net.disconnect(laser);
		}

		net.close();
	}

	private static class ClipRef {
		public final long startTime;
		public final LaserPart clip;

		public ClipRef(long startTime, LaserPart clip) {
			this.startTime = startTime;
			this.clip = clip;
		}
	}
}
