package com.unknown.emulight.lcp.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Logger;

import com.unknown.emulight.lcp.audio.AudioData;
import com.unknown.emulight.lcp.laser.LaserProcessor;
import com.unknown.emulight.lcp.project.Project;
import com.unknown.net.shownet.Laser;
import com.unknown.net.shownet.Point;
import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;

public class AudioDelayMeasurementTool {
	private static final Logger log = Trace.create(AudioDelayMeasurementTool.class);

	private final Project project;

	private Thread thread;
	private volatile boolean running;
	private Queue<Long> clicks = new ConcurrentLinkedDeque<>();

	private final AudioData data;

	public AudioDelayMeasurementTool(Project project) {
		this.project = project;

		project.setLaserRenderer(this::render);

		data = new AudioData();
		int sampleRate = project.getSystem().getAudioProcessor().getSampleRate();
		int length = sampleRate / 100;
		int freq = 1000;
		float[][] samples = new float[1][length];
		for(int i = 0; i < length; i++) {
			double t = i * freq / (double) sampleRate;
			double volume = 0.2 * (length - i) / length;
			samples[0][i] = (float) (Math.sin(t * 2 * Math.PI) * volume);
		}
		data.setData(samples, sampleRate);
	}

	public void destroy() {
		project.clearLaserRenderer();
	}

	public void start() {
		stop();
		thread = new Thread() {
			@Override
			public void run() {
				running = true;
				long last = System.nanoTime();
				project.playSystemSound(data);
				clicks.add(last);
				long beat = 1_000_000_000 / 2;
				while(running && !Thread.interrupted()) {
					long now = System.nanoTime();
					long t = now - last;
					if(t >= beat) {
						last = now;
						project.playSystemSound(data);
						clicks.add(last);
					}
					try {
						Thread.sleep(1);
					} catch(InterruptedException e) {
						break;
					}
				}
			}
		};
		thread.start();
	}

	public void stop() {
		if(thread != null) {
			running = false;
			thread.interrupt();
			try {
				thread.join();
			} catch(InterruptedException e) {
				// nothing
			}
			thread = null;
		}
	}

	private void render() {
		LaserProcessor processor = project.getSystem().getLaserProcessor();
		List<Point> points = new ArrayList<>();
		int cnt = 50;
		int max = cnt - 1;
		double r = 0.2;
		long now = System.nanoTime();
		if(clicks.isEmpty()) {
			points.add(new Point());
		} else {
			long dt = now - clicks.peek();
			long t = dt - project.getSystem().getConfig().getOutputDelay();
			int intensity;
			long period = 200_000_000;
			if(t > period) {
				intensity = 0;
				clicks.remove();
			} else {
				intensity = (int) (t * 128 / period);
				if(intensity < 0) {
					intensity = 0;
				} else {
					intensity = (128 - intensity) << 8;
				}
			}
			for(int i = 0; i < cnt; i++) {
				double phi = i / (double) max;
				double x = Math.cos(phi * 2.0 * Math.PI) * r;
				double y = Math.sin(phi * 2.0 * Math.PI) * r;
				Point p = new Point();
				p.x = (short) (x * 0x8000 + 0x8000);
				p.y = (short) (y * 0x8000 + 0x8000);
				p.red = (short) intensity;
				p.green = (short) intensity;
				p.blue = (short) intensity;
				points.add(p);
			}
		}
		for(Laser laser : processor.getLasers()) {
			try {
				laser.sendFrame(points, 2000);
			} catch(IOException e) {
				log.log(Levels.WARNING, "Failed to send frame: " + e.getMessage());
			}
		}
	}
}
