package com.unknown.emulight.lcp.live;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.unknown.xml.dom.Element;

public class CueMap {
	private final Map<TriggerKey, Cue<?>> cues = new HashMap<>();
	private final Map<Cue<?>, TriggerKey> inverse = new HashMap<>();

	public void setTrigger(TriggerKey key, Cue<?> cue) {
		if(cues.containsKey(key)) {
			throw new IllegalArgumentException("key already assigned");
		}

		Cue<?> oldCue = cues.put(key, cue);
		inverse.put(cue, key);
		if(oldCue != null) {
			inverse.remove(oldCue);
		}
	}

	public void removeTrigger(TriggerKey key) {
		Cue<?> cue = cues.remove(key);
		if(cue != null) {
			TriggerKey cueKey = inverse.remove(cue);
			assert cueKey != null && cueKey.equals(key);
		}
	}

	public void removeCue(Cue<?> cue) {
		TriggerKey key = inverse.remove(cue);
		if(key != null) {
			cues.remove(key);
		}
	}

	public Cue<?> getCue(TriggerKey key) {
		return cues.get(key);
	}

	public TriggerKey getTriggerKey(Cue<?> cue) {
		return inverse.get(cue);
	}

	public void clear() {
		cues.clear();
		inverse.clear();
	}

	public void read(Element xml, Map<Integer, Cue<?>> pool) throws IOException {
		if(!xml.name.equals("cue-map")) {
			throw new IOException("not a cue-map");
		}

		cues.clear();
		inverse.clear();

		for(Element e : xml.getChildren()) {
			if(e.name.equals("trigger")) {
				int cueId;
				try {
					cueId = Integer.parseInt(e.getAttribute("cue"));
				} catch(NumberFormatException ex) {
					throw new IOException("invalid cue id");
				}

				int type = TriggerKey.getType(e.getAttribute("type", "note"));

				int channel;
				try {
					channel = Integer.parseInt(e.getAttribute("channel"));
				} catch(NumberFormatException ex) {
					throw new IOException("invalid channel");
				}

				int key;
				try {
					key = Integer.parseInt(e.getAttribute("key"));
				} catch(NumberFormatException ex) {
					throw new IOException("invalid key");
				}

				Cue<?> cue = pool.get(cueId);
				if(cue == null) {
					throw new IOException("no cue with id " + cueId + " found");
				}

				try {
					setTrigger(new TriggerKey(type, channel, key), cue);
				} catch(IllegalArgumentException ex) {
					throw new IOException(ex.getMessage());
				}
			}
		}
	}

	public Element write(Map<Cue<?>, Integer> ids) {
		Element xml = new Element("cue-map");
		for(Entry<TriggerKey, Cue<?>> entry : cues.entrySet()) {
			TriggerKey key = entry.getKey();
			Cue<?> cue = entry.getValue();
			int id = ids.get(cue);

			Element trigger = new Element("trigger");
			trigger.addAttribute("cue", Integer.toString(id));
			trigger.addAttribute("type", key.getTypeName());
			trigger.addAttribute("channel", Integer.toString(key.getChannel()));
			trigger.addAttribute("key", Integer.toString(key.getKey()));
			xml.addChild(trigger);
		}
		return xml;
	}
}
