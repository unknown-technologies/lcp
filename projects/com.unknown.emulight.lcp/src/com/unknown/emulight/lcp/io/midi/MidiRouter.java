package com.unknown.emulight.lcp.io.midi;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDevice.Info;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.ShortMessage;

import com.unknown.emulight.lcp.io.esl.ESL;
import com.unknown.emulight.lcp.project.SystemConfiguration;
import com.unknown.emulight.lcp.project.SystemConfiguration.ESLMidiPortConfig;
import com.unknown.emulight.lcp.project.SystemConfiguration.MidiPortConfig;
import com.unknown.emulight.lcp.project.SystemConfiguration.NetworkMidiPortConfig;
import com.unknown.util.HexFormatter;
import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;

public class MidiRouter {
	private static final Logger log = Trace.create(MidiRouter.class);

	private final ESL esl;
	private final SystemConfiguration conf;

	private MidiInPort[] inputs;
	private MidiOutPort[] outputs;

	private List<ESLMidiOut> eslOutPorts = new ArrayList<>();
	private List<NetworkMidiOut> networkOutPorts = new ArrayList<>();

	private List<MidiReceiver> allBusReceivers = new ArrayList<>();

	public MidiRouter(ESL esl, SystemConfiguration conf) {
		this.esl = esl;
		this.conf = conf;
		updateMidiDevices();
	}

	public void closeAll() {
		if(inputs != null) {
			for(MidiInPort input : inputs) {
				input.closeDevice();
			}
		}

		if(outputs != null) {
			for(MidiOutPort output : outputs) {
				output.closeDevice();
			}
		}
	}

	public void updateMidiDevices() {
		closeAll();

		try {
			List<Info> inputList = getMIDIDevices(true);
			inputs = new MidiIn[inputList.size()];
			for(int i = 0; i < inputs.length; i++) {
				inputs[i] = new MidiIn(i, inputList.get(i), this);
			}

			List<Info> outputList = getMIDIDevices(false);
			outputs = new MidiOut[outputList.size()];
			for(int i = 0; i < outputs.length; i++) {
				outputs[i] = new MidiOut(i, outputList.get(i), this);
			}
		} catch(MidiUnavailableException e) {
			inputs = new MidiIn[0];
			outputs = new MidiOut[0];
		}

		configure();
	}

	private void configure() {
		for(MidiInPort in : inputs) {
			MidiPortConfig cfg = conf.getMidiPort(in.getName(), true);
			in.setAlias(cfg.getAlias());
			in.setActive(cfg.isActive());
			in.setAll(cfg.isAll());
		}

		for(MidiOutPort out : outputs) {
			MidiPortConfig cfg = conf.getMidiPort(out.getName(), false);
			out.setAlias(cfg.getAlias());
			out.setActive(cfg.isActive());
		}

		for(NetworkMidiPortConfig cfg : conf.getNetworkMidiPorts()) {
			NetworkMidiOut port = new NetworkMidiOut(cfg, this);
			networkOutPorts.add(port);
		}

		for(ESLMidiPortConfig cfg : conf.getESLMidiPorts()) {
			ESLMidiOut port = new ESLMidiOut(esl, cfg, this);
			eslOutPorts.add(port);
		}
	}

	public MidiPortConfig getPortConfig(MidiPort port) {
		return conf.getMidiPort(port.getName(), !(port instanceof MidiOutPort));
	}

	public ESLMidiPortConfig getESLPortConfig(MidiPort port) {
		if(port instanceof ESLMidiOut) {
			return conf.getESLMidiPort(port.getName());
		} else {
			throw new IllegalArgumentException("not an ESL port");
		}
	}

	public NetworkMidiPortConfig getNetworkPortConfig(MidiPort port) {
		if(port instanceof NetworkMidiOut) {
			return conf.getNetworkMidiPort(port.getAlias());
		} else {
			throw new IllegalArgumentException("not a network MIDI port");
		}
	}

	public ESLMidiOut addESLMidiOutPort(String name) {
		try {
			ESLMidiPortConfig cfg = conf.addESLMidiPort(name);
			ESLMidiOut port = new ESLMidiOut(esl, cfg, this);
			eslOutPorts.add(port);
			return port;
		} catch(IllegalArgumentException e) {
			// name already exists
			return null;
		}
	}

	public NetworkMidiOut addNetworkMidiOutPort(String name) {
		try {
			NetworkMidiPortConfig cfg = conf.addNetworkMidiPort(name);
			NetworkMidiOut port = new NetworkMidiOut(cfg, this);
			networkOutPorts.add(port);
			return port;
		} catch(IllegalArgumentException e) {
			// name already exists
			return null;
		}
	}

	public void delete(ESLMidiOut port) {
		eslOutPorts.remove(port);
		getESLPortConfig(port).delete();
	}

	public void delete(NetworkMidiOut port) {
		networkOutPorts.remove(port);
		getNetworkPortConfig(port).delete();
	}

	public MidiInPort[] getInputs() {
		return inputs;
	}

	public MidiOutPort[] getOutputs() {
		MidiOutPort[] outs = new MidiOutPort[outputs.length + networkOutPorts.size()];
		System.arraycopy(outputs, 0, outs, 0, outputs.length);
		for(int i = 0; i < networkOutPorts.size(); i++) {
			outs[outputs.length + i] = networkOutPorts.get(i);
		}
		return outs;
	}

	public ESLMidiOut[] getESLOutputs() {
		return eslOutPorts.toArray(new ESLMidiOut[eslOutPorts.size()]);
	}

	public MidiOutPort[] getOutputPorts() {
		MidiOutPort[] outs = new MidiOutPort[outputs.length + networkOutPorts.size() + eslOutPorts.size()];
		System.arraycopy(outputs, 0, outs, 0, outputs.length);
		for(int i = 0; i < networkOutPorts.size(); i++) {
			outs[outputs.length + i] = networkOutPorts.get(i);
		}
		for(int i = 0; i < eslOutPorts.size(); i++) {
			outs[outputs.length + networkOutPorts.size() + i] = eslOutPorts.get(i);
		}
		return outs;
	}

	public static List<Info> getMIDIDevices(boolean input) throws MidiUnavailableException {
		List<Info> devices = new ArrayList<>();
		for(Info info : MidiSystem.getMidiDeviceInfo()) {
			try {
				MidiDevice device = MidiSystem.getMidiDevice(info);
				int maxin = device.getMaxTransmitters();
				int maxout = device.getMaxReceivers();
				if(input && maxin == 0) {
					continue;
				} else if(!input && maxout == 0) {
					continue;
				}
				devices.add(info);
			} catch(Throwable t) {
				log.log(Levels.ERROR, "Error reading device: " + t.getMessage(), t);
				throw t;
			}
		}

		return devices;
	}

	public void addAllBusReceiver(MidiReceiver receiver) {
		allBusReceivers.add(receiver);
	}

	public void removeAllBusReceiver(MidiReceiver receiver) {
		allBusReceivers.remove(receiver);
	}

	public void send(int input, ShortMessage msg) {
		// ignore active sensing
		if(msg.getStatus() == ShortMessage.ACTIVE_SENSING) {
			return;
		}

		byte[] data = msg.getMessage();
		log.log(Levels.DEBUG, () -> "MIDI received: " + IntStream.range(0, data.length)
				.map(i -> Byte.toUnsignedInt(data[i]))
				.mapToObj(x -> HexFormatter.tohex(x, 2))
				.collect(Collectors.joining(" ")));

		if(inputs[input].isAll()) {
			for(MidiReceiver receiver : allBusReceivers) {
				receiver.receive(msg.getStatus(), msg.getData1(), msg.getData2());
			}
		}
	}
}
