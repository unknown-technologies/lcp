package com.unknown.emulight.lcp.sequencer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import com.unknown.audio.midi.smf.TempoEvent;
import com.unknown.emulight.lcp.audio.AudioData;
import com.unknown.emulight.lcp.audio.AudioPart;
import com.unknown.emulight.lcp.audio.AudioTrack;
import com.unknown.emulight.lcp.event.SequencerListener;
import com.unknown.emulight.lcp.project.PartContainer;
import com.unknown.emulight.lcp.project.TempoTrack;
import com.unknown.emulight.lcp.project.Track;
import com.unknown.emulight.lcp.sequencer.event.TimedAudioPart;
import com.unknown.emulight.lcp.sequencer.event.TimedEvent;
import com.unknown.emulight.lcp.sequencer.event.TimedNoteOff;
import com.unknown.emulight.lcp.sequencer.event.TimedNoteOn;
import com.unknown.emulight.lcp.sequencer.event.TimedPitchBend;
import com.unknown.emulight.lcp.sequencer.event.TimedTempo;
import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;

public class Sequencer {
	private static final Logger log = Trace.create(Sequencer.class);

	private int ppq;
	private volatile int microTempo;

	private volatile long time;

	private Thread player;

	private List<MidiTrack> tracks;
	private List<AudioTrack> audioTracks;
	private TempoTrack tempoTrack;

	private List<TimedEvent<?>> events;

	private volatile int tempoIndex;
	private volatile TempoCheckpoint tempoCheckpoint;
	private volatile boolean playing;
	private List<TempoCheckpoint> tempoCheckpoints;

	private List<SequencerListener> listeners = new ArrayList<>();

	private long startTick;

	public Sequencer() {
		setTempo(96, 120);
		startTick = 0;
		playing = false;
	}

	public void setTracks(List<MidiTrack> tracks) {
		this.tracks = tracks;
	}

	public void setAudioTracks(List<AudioTrack> tracks) {
		this.audioTracks = tracks;
	}

	public void setTempoTrack(TempoTrack tempoTrack) {
		this.tempoTrack = tempoTrack;
	}

	public void setTempo(int ppq, double bpm) {
		this.ppq = ppq;
		this.microTempo = (int) Math.round(60_000_000 / bpm);
	}

	public void generateEvents() {
		long outputDelay = tempoTrack.getProject().getSystem().getConfig().getOutputDelay();

		events = new ArrayList<>();
		tempoCheckpoints = new ArrayList<>();

		// compute tempo checkpoints

		// NOTE: this ONLY works correctly if there are no tempo changes
		// before tick=0 on the tempo track
		ppq = tempoTrack.getProject().getPPQ();
		TempoCheckpoint last = new TempoCheckpoint(0, 0, microTempo);
		tempoCheckpoints.add(last);
		for(PartContainer<TempoPart> part : tempoTrack.getParts()) {
			long t = part.getTime();
			for(TempoChange change : part.getPart().getTempoChanges()) {
				if(!part.containsEvent(change.getTime())) {
					continue;
				}

				double bpm = change.getTempo();
				int micro = TempoEvent.getMicroTempo(bpm);
				TimedTempo event = new TimedTempo(null, t + change.getTime(), micro);
				events.add(event);

				long dtick = event.getTime() - last.getTick();
				long nanotime = last.getTime() + dtick * last.getMicroTempo() * 1000 / ppq;
				if(last.getTime() == nanotime) {
					tempoCheckpoints.removeLast();
				}
				last = new TempoCheckpoint(event.getTime(), nanotime, micro);
				tempoCheckpoints.add(last);
			}
		}

		// add MIDI events
		for(MidiTrack track : tracks) {
			for(PartContainer<MidiPart> part : track.getParts()) {
				long t = part.getTime();
				long nanotime = tempoTrack.getTime(t) + outputDelay;
				t = tempoTrack.getTick(nanotime);
				for(Note note : part.getPart().getNotes()) {
					if(!part.containsEvent(note.getTime())) {
						continue;
					}

					Note n = note.clone();
					TimedEvent<?> noteOn = new TimedNoteOn(track, t + note.getTime(), n);
					TimedEvent<?> noteOff = new TimedNoteOff(track, t + note.getEnd(), n);
					events.add(noteOn);
					events.add(noteOff);
				}

				for(PitchBend bend : part.getPart().getPitchBends()) {
					if(!part.containsEvent(bend.getTime())) {
						continue;
					}

					PitchBend b = bend.clone();
					TimedEvent<?> event = new TimedPitchBend(track, t + bend.getTime(), b);
					events.add(event);
				}
			}
		}

		// add audio events
		for(AudioTrack track : audioTracks) {
			for(PartContainer<AudioPart> part : track.getParts()) {
				AudioData data = part.getPart().getData();
				if(data == null) {
					continue;
				}

				int sampleRate = data.getSampleRate();

				long partStartTick = part.getStart();
				long partEndTick = part.getEnd();
				long startTime = tempoTrack.getTime(partStartTick);
				long endTime = tempoTrack.getTime(partEndTick);

				int startSample = 0;
				int endSample = (int) ((endTime - startTime) * sampleRate / 1_000_000_000);
				if(endSample > data.getSampleCount()) {
					endSample = data.getSampleCount();
				}

				if(startTime < 0) {
					startSample = (int) (-startTime * sampleRate / 1_000_000_000);
				}

				if(partStartTick < 0) {
					partStartTick = 0;
				}

				// adjust for startTick
				if(startTick > 0) {
					long playbackStartTime = tempoTrack.getTime(startTick);
					if(playbackStartTime > startTime && playbackStartTime < endTime) {
						partStartTick = startTick;
						startSample = (int) ((playbackStartTime - startTime) * sampleRate /
								1_000_000_000);
					}
				}

				TimedEvent<?> event = new TimedAudioPart(track, part.getPart(), partStartTick,
						startSample, endSample);
				events.add(event);
			}
		}

		// sort everything into chronological order
		Collections.sort(events, (a, b) -> Long.compare(a.getTime(), b.getTime()));
	}

	private void process() {
		int index = 0;
		tempoIndex = 0;
		tempoCheckpoint = tempoCheckpoints.get(0); // this is guaranteed to have tick=0, nanotime=0

		// skip to start tick: tempo checkpoints
		for(TempoCheckpoint checkpoint : tempoCheckpoints) {
			if(checkpoint.getTick() < startTick) {
				tempoCheckpoint = checkpoint;
				tempoIndex++;
			} else {
				tempoIndex--;
				break;
			}
		}

		// Careful here: if the first tempo checkpoint on the track is
		// AFTER the start tick, the NEXT tempo checkpoint is that first
		// checkpoint. The current tempoIndex in this case is -1, such
		// that process() goes to that checkpoint once it is reached.

		microTempo = tempoCheckpoint.getMicroTempo();

		// skip to start tick: events
		while(index < events.size()) {
			TimedEvent<?> evt = events.get(index);
			if(evt.getTime() < startTick) {
				index++;
			} else {
				break;
			}
		}

		// compute start time
		time = System.nanoTime() - getTime(startTick);

		playing = true;
		try {
			while(index < events.size()) {
				TimedEvent<?> next = events.get(index);

				// wait until this event is scheduled
				long dt = waitFor(next);
				while(dt > 0) {
					long ms = dt / 1_000_000;
					int ns = (int) (dt % 1_000_000);
					Thread.sleep(ms, ns);

					dt = waitFor(next);
				}

				// process the event
				process(next);

				// proceed to next event
				index++;
			}

			// keep running forever
			while(true) {
				Thread.sleep(1000);
			}

			// fireStop();
		} catch(InterruptedException e) {
			playing = false;
			return;
		}
	}

	private long getTime(long tick) {
		long dtick = tick - tempoCheckpoint.getTick();
		long dtime = dtick * microTempo * 1000 / ppq;
		return tempoCheckpoint.getTime() + dtime;
	}

	private long waitFor(TimedEvent<?> event) {
		long t = getTime(event.getTime());
		long now = System.nanoTime() - time;
		long dt = t - now;
		if(dt > 0) {
			return dt;
		} else {
			return 0;
		}
	}

	private void process(TimedEvent<?> event) {
		if(event instanceof TimedTempo) {
			TimedTempo tempo = (TimedTempo) event;
			microTempo = tempo.getMicroTempo();
			tempoIndex++;
			tempoCheckpoint = tempoCheckpoints.get(tempoIndex);
		} else {
			Track<?> track = event.getTrack();
			if(!track.isMuted() || track instanceof AudioTrack) {
				event.transmit();
			}
		}
	}

	public void play() {
		if(tracks == null) {
			return;
		}

		if(player != null) {
			stop();
		}

		player = new Thread() {
			@Override
			public void run() {
				process();
			}
		};

		player.setPriority(Thread.MAX_PRIORITY);
		player.setDaemon(true);

		player.start();
		fireStart();
	}

	public void stop() {
		if(player == null || !player.isAlive()) {
			return;
		}

		playing = false;
		player.interrupt();
		try {
			player.join();
		} catch(InterruptedException e) {
			// restore interrupted flag
			Thread.currentThread().interrupt();
		}

		for(MidiTrack track : tracks) {
			track.noteOff();
		}

		player = null;

		fireStop();
	}

	public boolean isPlaying() {
		return player != null && player.isAlive() && playing;
	}

	public long getTick() {
		if(isPlaying()) {
			long now = System.nanoTime() - time;
			long dtime = now - tempoCheckpoint.getTime();
			long dtick = dtime * ppq / microTempo / 1000;
			return tempoCheckpoint.getTick() + dtick;
		} else {
			return startTick;
		}
	}

	public long getTime() {
		if(isPlaying()) {
			return System.nanoTime() - time;
		} else {
			return tempoTrack.getTime(startTick);
		}
	}

	public void setTick(long tick) {
		boolean wasPlaying = isPlaying();
		stop();
		startTick = tick;
		// TODO: adjust the rest
		fireSetPosition(startTick);
		if(wasPlaying) {
			play();
		}
	}

	public double getBPM() {
		return (int) Math.round(60_000_000.0 / microTempo);
	}

	public void addListener(SequencerListener listener) {
		listeners.add(listener);
	}

	public void removeListener(SequencerListener listener) {
		listeners.remove(listener);
	}

	protected void fireStart() {
		for(SequencerListener listener : listeners) {
			try {
				listener.playbackStarted();
			} catch(Throwable t) {
				log.log(Levels.ERROR, "Failed to execute sequencer listener: " + t.getMessage(), t);
			}
		}
	}

	protected void fireStop() {
		for(SequencerListener listener : listeners) {
			try {
				listener.playbackStopped();
			} catch(Throwable t) {
				log.log(Levels.ERROR, "Failed to execute sequencer listener: " + t.getMessage(), t);
			}
		}
	}

	protected void fireSetPosition(long tick) {
		for(SequencerListener listener : listeners) {
			try {
				listener.positionChanged(tick);
			} catch(Throwable t) {
				log.log(Levels.ERROR, "Failed to execute sequencer listener: " + t.getMessage(), t);
			}
		}
	}

	private static class TempoCheckpoint {
		private final long tick;
		private final long nanotime;
		private final int microTempo;

		public TempoCheckpoint(long tick, long nanotime, int microTempo) {
			this.tick = tick;
			this.nanotime = nanotime;
			this.microTempo = microTempo;
		}

		public long getTick() {
			return tick;
		}

		public long getTime() {
			return nanotime;
		}

		public int getMicroTempo() {
			return microTempo;
		}
	}
}
