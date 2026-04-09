package com.unknown.emulight.lcp.live;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CueMap {
	private final Map<TriggerKey, Cue<?>> cues = new HashMap<>();
	private final Map<Cue<?>, Set<TriggerKey>> inverse = new HashMap<>();

	public void setTrigger(TriggerKey key, Cue<?> cue) {
		cues.put(key, cue);
		Set<TriggerKey> keys = inverse.get(cue);
		if(keys == null) {
			keys = new HashSet<>();
			inverse.put(cue, keys);
		}
		keys.add(key);
	}

	public void removeTrigger(TriggerKey key) {
		Cue<?> cue = cues.remove(key);
		if(cue != null) {
			Set<TriggerKey> keys = inverse.get(cue);
			assert keys != null;
			keys.remove(key);
			if(keys.isEmpty()) {
				inverse.remove(cue);
			}
		}
	}

	public void removeCue(Cue<?> cue) {
		Set<TriggerKey> keys = inverse.remove(cue);
		if(keys != null) {
			for(TriggerKey key : keys) {
				cues.remove(key);
			}
		}
	}

	public Cue<?> getCue(TriggerKey key) {
		return cues.get(key);
	}

	public Set<TriggerKey> getTriggerKeys(Cue<?> cue) {
		Set<TriggerKey> keys = inverse.get(cue);
		if(keys != null) {
			return Collections.unmodifiableSet(keys);
		} else {
			return Collections.emptySet();
		}
	}

	public void clear() {
		cues.clear();
		inverse.clear();
	}
}
