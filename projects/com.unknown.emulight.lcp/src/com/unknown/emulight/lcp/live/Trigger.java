package com.unknown.emulight.lcp.live;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.unknown.emulight.lcp.event.TriggerListener;
import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;

public abstract class Trigger {
	private static final Logger log = Trace.create(Trigger.class);

	private final String name;

	private boolean state;

	private final List<TriggerListener> listeners = new ArrayList<>();

	protected Trigger(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void addTriggerListener(TriggerListener listener) {
		synchronized(listeners) {
			listeners.add(listener);
		}
	}

	public void removeTriggerListener(TriggerListener listener) {
		synchronized(listeners) {
			listeners.remove(listener);
		}
	}

	protected void fireValueChanged() {
		synchronized(listeners) {
			for(TriggerListener listener : listeners) {
				try {
					listener.changed(this);
				} catch(Throwable t) {
					log.log(Levels.ERROR, "Failed to execute trigger listener: " + t.getMessage(),
							t);
				}
			}
		}
	}

	protected abstract void set(boolean state);

	public boolean getState() {
		return state;
	}

	public void setState(boolean state) {
		this.state = state;
		set(state);
	}

	public void trigger() {
		setState(true);
		setState(false);
	}
}
