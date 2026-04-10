package com.unknown.emulight.lcp.live;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.unknown.emulight.lcp.event.TargetListener;
import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;

public abstract class Target {
	private static final Logger log = Trace.create(Target.class);

	private final String name;

	private final List<TargetListener> listeners = new ArrayList<>();

	protected Target(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void addTargetListener(TargetListener listener) {
		synchronized(listeners) {
			listeners.add(listener);
		}
	}

	public void removeTargetListener(TargetListener listener) {
		synchronized(listeners) {
			listeners.remove(listener);
		}
	}

	protected void fireValueChanged() {
		synchronized(listeners) {
			for(TargetListener listener : listeners) {
				try {
					listener.changed(this);
				} catch(Throwable t) {
					log.log(Levels.ERROR, "Failed to execute target listener: " + t.getMessage(),
							t);
				}
			}
		}
	}

	public abstract double getDefault();

	protected abstract double get();

	protected abstract void set(double value);

	public double getValue() {
		return get();
	}

	public void setValue(double value) {
		set(value);
		fireValueChanged();
	}
}
