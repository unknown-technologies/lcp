package com.unknown.emulight.lcp.audio;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.unknown.audio.meta.riff.DataChunk;
import com.unknown.audio.meta.riff.Riff;
import com.unknown.audio.meta.riff.RiffWave;
import com.unknown.audio.meta.riff.WaveFormatChunk;

public class AudioData {
	private File file;
	private int sampleRate;
	private int channels;
	private int length;
	private float[][] samples;
	private AudioPeakMap map;

	public AudioData() {
		file = null;
		channels = 1;
		sampleRate = 48000;
		samples = new float[channels][0];
		map = null;
	}

	public AudioData(File path) throws IOException {
		load(path);
	}

	public void load(File path) throws IOException {
		this.file = path;
		try(InputStream in = new BufferedInputStream(new FileInputStream(file))) {
			RiffWave wav = Riff.read(in);
			channels = wav.getChannels();
			sampleRate = wav.getSampleRate();
			samples = wav.getFloatSamples();
			length = wav.getSampleCount();
			map = new AudioPeakMap(this);
		}
	}

	public void writeWAV(File path) throws IOException {
		try(OutputStream out = new BufferedOutputStream(new FileOutputStream(path))) {
			RiffWave wav = new RiffWave();
			wav.set(new WaveFormatChunk());
			wav.set(new DataChunk());
			wav.setSampleRate(getSampleRate());
			wav.setSampleFormat(WaveFormatChunk.WAVE_FORMAT_PCM);
			wav.setChannels(getChannelCount());
			wav.setBitsPerSample(24);
			wav.set24bitSamples(samples);
			wav.write(out);
		}
	}

	public File getFile() {
		return file;
	}

	public int getSampleRate() {
		return sampleRate;
	}

	public int getChannelCount() {
		return channels;
	}

	public int getSampleCount() {
		return length;
	}

	public float[][] getSamples() {
		return samples;
	}

	public AudioPeakMap getPeakMap() {
		return map;
	}

	public long getLength() {
		return length * 1000L / sampleRate;
	}
}
