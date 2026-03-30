package com.unknown.emulight.lcp.laser.node.plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.logging.Logger;

import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;

public class CustomNodePluginRegistry {
	private static final Logger log = Trace.create(CustomNodePluginRegistry.class);

	private static CustomNodePluginRegistry instance;

	private final ServiceLoader<CustomNodePlugin> loader = ServiceLoader.load(CustomNodePlugin.class);

	private final Map<String, CustomNodePlugin> plugins = new HashMap<>();

	public static CustomNodePluginRegistry get() {
		if(instance == null) {
			instance = new CustomNodePluginRegistry();
			instance.load();
		}
		return instance;
	}

	public void load() {
		Iterator<CustomNodePlugin> iterator = loader.iterator();
		while(iterator.hasNext()) {
			CustomNodePlugin plugin = iterator.next();
			if(plugins.containsKey(plugin.getId())) {
				log.log(Levels.WARNING, "Duplicate custom node plugins found for ID " + plugin.getId());
			}
			plugins.put(plugin.getId(), plugin);
		}
	}

	public CustomNodePlugin getPlugin(String id) {
		return plugins.get(id);
	}

	public List<CustomNodePlugin> getPlugins() {
		return new ArrayList<>(plugins.values());
	}
}
