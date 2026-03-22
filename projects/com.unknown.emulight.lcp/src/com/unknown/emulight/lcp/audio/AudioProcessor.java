package com.unknown.emulight.lcp.audio;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import com.unknown.emulight.lcp.event.SequencerListener;
import com.unknown.util.io.Endianess;
import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;

public class AudioProcessor implements AutoCloseable, SequencerListener {
	private static final Logger log = Trace.create(AudioProcessor.class);

	private int sampleRate;
	private int channels;
	private SourceDataLine waveout;

	private Thread audioThread;
	private volatile boolean running;

	private volatile boolean playing = false;

	private Set<Clip> clips = new HashSet<>();

	private int blockSize = 1024;

	public AudioProcessor(int sampleRate) {
		this.sampleRate = sampleRate;
		channels = 2;
	}

	public int getSampleRate() {
		return sampleRate;
	}

	public int getChannelCount() {
		return channels;
	}

	public void setSampleRate(int sampleRate) throws LineUnavailableException {
		close();
		this.sampleRate = sampleRate;
		open();
	}

	public int getBlockSize() {
		return blockSize;
	}

	public void setBlockSize(int blockSize) throws LineUnavailableException {
		close();
		this.blockSize = blockSize;
		open();
	}

	public void open() throws LineUnavailableException {
		close();

		// @formatter:off
		AudioFormat format = new AudioFormat(
				Encoding.PCM_SIGNED,			// encoding
				sampleRate,				// sample rate
				16,					// bit/sample
				channels,				// channels
				2 * channels,
				sampleRate,
				true					// big-endian
		);
		// @formatter:on

		DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
		if(!AudioSystem.isLineSupported(info)) {
			throw new LineUnavailableException("Line matching " + info + " not supported");
		}

		waveout = (SourceDataLine) AudioSystem.getLine(info);
		waveout.open(format, blockSize * format.getFrameSize());

		log.info("Audio line opened: " + format);

		running = true;
		audioThread = new Thread() {
			@Override
			public void run() {
				waveout.start();
				while(running && !Thread.interrupted()) {
					try {
						process();
					} catch(Throwable t) {
						log.log(Levels.ERROR, "Audio processing failed: " + t.getMessage(), t);
						break;
					}
				}
				waveout.stop();
			}
		};
		audioThread.setDaemon(true);
		audioThread.start();
	}

	@Override
	public void close() {
		if(waveout != null && waveout.isOpen()) {
			running = false;
			audioThread.interrupt();

			try {
				audioThread.join();
			} catch(InterruptedException e) {
				// nothing
			}

			waveout.close();
			waveout = null;

			log.info("Audio line closed");
		}
	}

	private void process() {
		float[] sumL = new float[blockSize];
		float[] sumR = new float[blockSize];

		if(playing) {
			// process clips
			synchronized(clips) {
				Set<Clip> remove = new HashSet<>();
				clip: for(Clip clip : clips) {
					float volume = (float) clip.getVolume();
					AudioData data = clip.getData();
					if(data == null) {
						remove.add(clip);
						continue clip;
					}

					int ch = data.getChannelCount();

					for(int i = 0; i < blockSize; i++) {
						boolean kill = false;
						int pos = clip.getPosition();
						if(clip.advance(1)) {
							kill = true;
						}

						if(ch == 1) {
							float sample = data.getSamples()[0][pos] * volume;
							sumL[i] += sample;
							sumR[i] += sample;
						} else if(ch == 2) {
							sumL[i] = data.getSamples()[0][pos] * (float) clip.getVolume();
							sumR[i] = data.getSamples()[1][pos] * (float) clip.getVolume();
						} else {
							kill = true;
						}

						if(kill) {
							remove.add(clip);
							continue clip;
						}
					}
				}
				clips.removeAll(remove);
			}
		}

		byte[] samples = new byte[blockSize * 4];

		for(int i = 0; i < blockSize; i++) {
			int left = Math.round(sumL[i] * 32767.0f);
			int right = Math.round(sumR[i] * 32767.0f);

			// clamp samples
			if(left < -32767) {
				left = -32767;
			} else if(left > 32767) {
				left = 32767;
			}

			if(right < -32767) {
				right = -32767;
			} else if(right > 32767) {
				right = 32767;
			}

			// encode to 16bit BE and write to audio out
			Endianess.set16bitBE(samples, i * 4 + 0, (short) left);
			Endianess.set16bitBE(samples, i * 4 + 2, (short) right);
		}

		waveout.write(samples, 0, samples.length);
	}

	public void playbackStarted() {
		playing = true;
	}

	public void playbackStopped() {
		playing = false;
		synchronized(clips) {
			clips.clear();
		}
	}

	public void positionChanged(long tick) {
		// TODO
	}

	public void play(AudioTrack track, AudioPart part, int start, int end) {
		AudioData data = part.getData();
		if(data == null) {
			return;
		}
		if(data.getSampleRate() != sampleRate) {
			return;
		}

		synchronized(clips) {
			clips.add(new Clip(track, part, start, end));
		}
	}

	private static class Clip {
		private final AudioTrack track;
		private final AudioPart part;
		private int position;
		private final int end;

		public Clip(AudioTrack track, AudioPart part, int position, int end) {
			this.track = track;
			this.part = part;
			this.position = position;
			this.end = end;
		}

		public AudioData getData() {
			return part.getData();
		}

		public int getPosition() {
			return position;
		}

		public double getVolume() {
			return track.getVolume();
		}

		public boolean advance(int samples) {
			AudioData data = getData();
			if(data == null) {
				return false;
			}
			position += samples;
			return position >= end || position >= data.getSampleCount();
		}
	}
}
