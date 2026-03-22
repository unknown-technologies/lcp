package com.unknown.emulight.lcp.sequencer.event;

import com.unknown.emulight.lcp.audio.AudioPart;
import com.unknown.emulight.lcp.audio.AudioProcessor;
import com.unknown.emulight.lcp.audio.AudioTrack;

public class TimedAudioPart extends TimedEvent<AudioTrack> {
	private final AudioPart part;
	private final int start;
	private final int end;

	public TimedAudioPart(AudioTrack track, AudioPart part, long time, int start, int end) {
		super(track, time);
		this.part = part;
		this.start = start;
		this.end = end;
	}

	@Override
	public void transmit() {
		AudioProcessor processor = track.getProject().getSystem().getAudioProcessor();
		processor.play(track, part, start, end);
	}
}
