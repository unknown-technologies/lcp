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
import com.unknown.math.g3d.Mtx44;
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

	private final ConcurrentMap<InterfaceId, ClipRef> currentClip = new ConcurrentHashMap<>();

	private final SystemConfiguration config;

	private final Timer connectTimer;

	private LaserRenderer renderer;

	private long frameNumber = 0;

	public LaserProcessor(SystemConfiguration config, int rate) throws IOException {
		this.config = config;
		this.rate = rate;

		net = new ShowNET(true);

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
					frameNumber++;
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
					if(laser != null) {
						disconnect(laser);
					}
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

	public long getFrameNumber() {
		return frameNumber;
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
		assert laser != null;
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

	public void setCurrentClip(Laser laser, LaserPart clip, double bpm, int ppq, int length, boolean mirrorX,
			boolean mirrorY) {
		long now = System.nanoTime();
		currentClip.put(laser.getInterfaceId(), new ClipRef(now, clip, bpm, ppq, length, mirrorX, mirrorY));
	}

	public LaserPart getCurrentClip(Laser laser) {
		ClipRef ref = currentClip.get(laser.getInterfaceId());
		if(ref != null) {
			return ref.clip;
		} else {
			return null;
		}
	}

	public void clearCurrentClip(Laser laser) {
		currentClip.remove(laser.getInterfaceId());
	}

	public boolean hasCurrentClip(Laser laser) {
		return currentClip.containsKey(laser.getInterfaceId());
	}

	private void process() {
		long now = System.nanoTime();
		Mtx44 identity = new Mtx44();
		for(Laser laser : getLasers()) {
			if(!laser.isConnected()) {
				continue;
			}
			try {
				ClipRef ref = currentClip.get(laser.getInterfaceId());
				if(ref != null) {
					Mtx44 posmtx = Mtx44.scale(ref.mirrorX ? -1.0 : 1.0, ref.mirrorY ? -1.0 : 1.0,
							1.0);
					LaserPart clip = ref.clip;
					int t = ref.getTick(now);
					if(clip.isLoop()) {
						t %= clip.getLength();
						laser.sendFrame(clip.render(t, posmtx, identity), clip.getSpeed());
					} else if(ref.length != 0 && t >= ref.length) {
						currentClip.remove(laser.getInterfaceId());
						laser.sendFrame(List.of(new Point()), 1000);
					} else {
						laser.sendFrame(clip.render(t, posmtx, identity), clip.getSpeed());
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
		public final int length;
		public final boolean mirrorX;
		public final boolean mirrorY;
		private final int ppq;
		private final int microTempo;

		public ClipRef(long startTime, LaserPart clip, double bpm, int ppq, int length, boolean mirrorX,
				boolean mirrorY) {
			this.startTime = startTime;
			this.clip = clip;
			this.ppq = ppq;
			this.microTempo = (int) Math.round(60_000_000 / bpm);
			this.length = length;
			this.mirrorX = mirrorX;
			this.mirrorY = mirrorY;
		}

		public int getTick(long time) {
			long dtime = time - startTime;
			return (int) (dtime * ppq / microTempo / 1000);
		}
	}
}
