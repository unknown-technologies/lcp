package com.unknown.emulight.lcp.sequencer;

import java.io.IOException;
import java.util.logging.Logger;

import javax.sound.midi.InvalidMidiDataException;

import com.unknown.audio.midi.smf.MIDIEvent;
import com.unknown.emulight.lcp.event.TrackListener;
import com.unknown.emulight.lcp.io.midi.MidiOutPort;
import com.unknown.emulight.lcp.project.Project;
import com.unknown.emulight.lcp.project.Track;
import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;
import com.unknown.xml.dom.Element;

public class MidiTrack extends Track<MidiPart> {
	private static Logger log = Trace.create(MidiTrack.class);

	public static final int ANY = -1;
	public static final int NO_PROGRAM = -1;

	private int[][] activeNotes = new int[16][128];
	private int[][] currentCCs = new int[16][128];
	private int[] currentProgram = new int[16];
	private int[] currentBend = new int[16];

	private MidiOutPort port;

	private int channel;

	private int program;

	public MidiTrack(Project project, String name) {
		super(MIDI, project, name);

		program = NO_PROGRAM;
		channel = 0;

		for(int i = 0; i < currentProgram.length; i++) {
			currentProgram[i] = NO_PROGRAM;
			currentBend[i] = 0;
		}

		setVolume(100);
	}

	@Override
	public MidiTrack clone() {
		MidiTrack track = new MidiTrack(getProject(), getName());

		track.program = program;
		track.channel = channel;
		track.port = port;

		copy(track);

		return track;
	}

	public int getChannel() {
		return channel;
	}

	public void setChannel(int channel) {
		if(this.channel != channel) {
			noteOff();
		}

		int oldChannel = this.channel;
		this.channel = channel;
		fireEvent(TrackListener.CHANNEL, oldChannel, channel);
	}

	public MidiOutPort getPort() {
		return port;
	}

	public void setPort(MidiOutPort port) {
		if(this.port != port && this.port != null) {
			noteOff();
		}

		MidiOutPort oldPort = this.port;
		this.port = port;
		fireEvent(TrackListener.PORT, oldPort, port);
	}

	public int getProgram() {
		return program;
	}

	public void setProgram(int program) {
		int oldProgram = this.program;
		this.program = program;
		if(fireEvent(TrackListener.PROGRAM, oldProgram, program)) {
			sendProgramChange(program);
		}
	}

	@Override
	protected void onMute() {
		noteOff();
	}

	@Override
	protected void onRecordingArmed() {
		getProject().addAllBusTrack(this);
	}

	@Override
	protected void onRecordingDisarmed() {
		getProject().removeAllBusTrack(this);
	}

	@Override
	protected void onVolumeChanged() {
		if(channel != ANY) {
			sendParameter(MIDIEvent.CC_VOLUME, clampCC(getVolume()));
		}
	}

	private static int clampCC(double value) {
		int val = (int) Math.round(value);
		if(val < 0) {
			return 0;
		} else if(val > 127) {
			return 127;
		} else {
			return val;
		}
	}

	private int getChannel(int ch) {
		if(channel == ANY) {
			return ch;
		} else {
			return channel;
		}
	}

	private void transmit(int status, int data1, int data2) {
		if(port == null) {
			return;
		}

		try {
			port.transmit(-1, status & 0xFF, data1, data2);
		} catch(InvalidMidiDataException e) {
			log.log(Levels.ERROR, "Failed to transmit MIDI event: " + e.getMessage(), e);
		}
	}

	public void transmit(int chan, int status, int data1, int data2) {
		int ch = getChannel(chan);
		transmit((status & 0xF0) | ch, data1, data2);
	}

	public void noteOn(Note note) {
		int ch = getChannel(note.getChannel());

		if(port != null) {
			activeNotes[ch][note.getKey()]++;
			transmit(MIDIEvent.NOTE_ON | ch, note.getKey(), note.getVelocity());
		}
	}

	public void noteOff(Note note) {
		int ch = getChannel(note.getChannel());

		if(port != null) {
			activeNotes[ch][note.getKey()]--;
			if(activeNotes[ch][note.getKey()] < 0) {
				activeNotes[ch][note.getKey()] = 0;
			}

			transmit(MIDIEvent.NOTE_OFF | ch, note.getKey(), note.getReleaseVelocity());
		}
	}

	public void cc(int chan, int cc, int value) {
		int ch = getChannel(chan);

		if(port != null) {
			currentCCs[ch][cc] = value;

			transmit(MIDIEvent.CTRL_CHANGE | ch, cc, value);
		}
	}

	public void programChange(int chan, int prog) {
		int ch = getChannel(chan);

		if(port != null) {
			currentProgram[ch] = prog;

			transmit(MIDIEvent.PROG_CHANGE | ch, prog, 0);
		}
	}

	public void pitchBend(PitchBend bend) {
		pitchBend(bend.getChannel(), bend.getBend());
	}

	public void pitchBend(int chan, int bend) {
		int ch = getChannel(chan);

		if(port != null) {
			currentBend[ch] = bend;

			int data = bend + 8192;
			int msb = (data >> 7) & 0x7F;
			int lsb = data & 0x7F;
			transmit(MIDIEvent.PITCH_BEND | ch, lsb, msb);
		}
	}

	public void noteOff() {
		for(int ch = 0; ch < 16; ch++) {
			for(int key = 0; key < 128; key++) {
				while(activeNotes[ch][key] > 0) {
					activeNotes[ch][key]--;
					transmit(MIDIEvent.NOTE_OFF | ch, key, 64);
				}
			}
		}
	}

	public void sendParameter(int cc, int value) {
		if(channel != ANY) {
			cc(channel, cc, value);
		}
	}

	public void sendProgramChange(int prog) {
		if(channel != ANY && prog != NO_PROGRAM) {
			programChange(channel, prog);
		}
	}

	public void reset() {
		if(channel != ANY) {
			for(int key = 0; key < 128; key++) {
				activeNotes[channel][key] = 0;
			}

			// send an ALL_NOTE_OFF command
			transmit(MIDIEvent.CTRL_CHANGE, MIDIEvent.CC_ALL_NOTE_OFF, 0);
			transmit(MIDIEvent.CTRL_CHANGE, MIDIEvent.CC_RESET_CTRL, 0);
		}

		for(int ch = 0; ch < 16; ch++) {
			if(ch == channel) {
				continue;
			}

			boolean active = false;

			// any active notes on this channel?
			for(int key = 0; key < 128; key++) {
				if(activeNotes[ch][key] > 0) {
					active = true;
					activeNotes[ch][key] = 0;
				}
			}

			// if yes, send an ALL_NOTE_OFF command
			if(active) {
				transmit(MIDIEvent.CTRL_CHANGE, MIDIEvent.CC_ALL_NOTE_OFF, 0);
			}
		}

		sendParameter(MIDIEvent.CC_VOLUME, clampCC(getVolume()));

		if(program != NO_PROGRAM) {
			sendProgramChange(program);
		}

		if(channel != ANY) {
			pitchBend(channel, currentBend[channel]);
		}
	}

	public void inputAllBus(int status, int data1, int data2) {
		switch((byte) (status & 0xF0)) {
		case MIDIEvent.NOTE_ON:
			noteOn(new Note(-1, data1, data2, 0));
			break;
		case MIDIEvent.NOTE_OFF:
			noteOff(new Note(-1, data1, data2, 0));
			break;
		case MIDIEvent.CTRL_CHANGE:
			cc(getChannel(status & 0x0F), data1, data2);
			break;
		case MIDIEvent.PROG_CHANGE:
			programChange(status & 0x0F, data1);
			break;
		case MIDIEvent.PITCH_BEND:
			pitchBend(status & 0x0F, MIDIEvent.getBend(data1, data2));
			break;
		default:
			transmit(status, data1, data2);
			break;
		}
	}

	@Override
	protected MidiPart createPart() {
		return new MidiPart(getProject().getPPQ());
	}

	@Override
	protected void readTrack(Element xml) throws IOException {
		setChannel(Integer.parseInt(xml.getAttribute("channel")));
		if(xml.getAttribute("port") != null) {
			String portName = xml.getAttribute("port");
			MidiOutPort[] ports = getProject().getSystem().getMidiRouter().getOutputPorts();
			for(MidiOutPort out : ports) {
				if(out.getName() != null && out.getName().equals(portName)) {
					setPort(out);
					break;
				}
			}
		}
		if(xml.getAttribute("program") != null) {
			setProgram(Integer.parseInt(xml.getAttribute("program")));
		}
		onVolumeChanged();
	}

	@Override
	protected void writeTrack(Element xml) {
		xml.addAttribute("channel", Integer.toString(channel));
		if(port != null) {
			xml.addAttribute("port", port.getName());
		}
		if(program != NO_PROGRAM) {
			xml.addAttribute("program", Integer.toString(program));
		}
	}
}
