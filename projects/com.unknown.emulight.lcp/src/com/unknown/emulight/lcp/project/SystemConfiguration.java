package com.unknown.emulight.lcp.project;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.UIManager;

import com.unknown.emulight.lcp.event.ConfigChangeListener;
import com.unknown.net.shownet.InterfaceId;
import com.unknown.net.shownet.LaserInfo;
import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;
import com.unknown.xml.dom.Element;
import com.unknown.xml.dom.XMLReader;

public class SystemConfiguration {
	private static final Logger log = Trace.create(SystemConfiguration.class);

	public static final String SERIAL_LINE = "serial-line";
	public static final String WALLPAPER = "wallpaper";
	public static final String OUTLINE_DRAG_MODE = "outline-drag-mode";
	public static final String FULLSCREEN = "fullscreen";
	public static final String LOOKANDFEEL = "look-and-feel";
	public static final String WINDOW_DECORATIONS = "window-decorations";

	private String serialLine;
	private String wallpaper;
	private boolean outlineDrag = false;
	private boolean fullscreen = false;
	private LookAndFeel laf = LookAndFeel.MOTIF;
	private boolean windowDecorations = true;

	private Map<MidiPortId, MidiPortConfig> midiPortConfig = new HashMap<>();
	private Map<String, ESLMidiPortConfig> eslMidiPortConfig = new HashMap<>();

	private Set<LaserAddress> laserAddresses = new HashSet<>();
	private Map<String, LaserConfig> lasers = new HashMap<>();
	private Map<InterfaceId, LaserConfig> lasersLookup = new HashMap<>();

	private List<ConfigChangeListener> listeners = new ArrayList<>();

	public SystemConfiguration() {
		wallpaper = null;

		try {
			read();
		} catch(FileNotFoundException | NoSuchFileException e) {
			log.log(Levels.WARNING, "Config file " + getConfigPath() + " not found");
		} catch(IOException e) {
			log.log(Levels.ERROR, "Failed to read config file " + getConfigPath(), e);
		}
	}

	public void close() {
		try {
			write();
		} catch(IOException e) {
			log.log(Levels.ERROR, "Failed to write config file " + getConfigPath(), e);
		}
	}

	private static boolean same(String a, String b) {
		return (a == b) || (a != null && a.equals(b));
	}

	public String getSerialLine() {
		return serialLine;
	}

	public void setSerialLine(String serialLine) {
		if(same(serialLine, this.serialLine)) {
			return;
		}

		this.serialLine = serialLine;
		fireConfigChangedEvent(SERIAL_LINE, serialLine);
	}

	public String getWallpaper() {
		return wallpaper;
	}

	public void setWallpaper(String wallpaper) {
		if(same(wallpaper, this.wallpaper)) {
			return;
		}

		this.wallpaper = wallpaper;
		fireConfigChangedEvent(WALLPAPER, wallpaper);
	}

	public boolean isOutlineDragMode() {
		return outlineDrag;
	}

	public void setOutlineDragMode(boolean outlineDrag) {
		if(this.outlineDrag != outlineDrag) {
			this.outlineDrag = outlineDrag;
			fireConfigChangedEvent(OUTLINE_DRAG_MODE, Boolean.toString(outlineDrag));
		}
	}

	public boolean isFullscreen() {
		return fullscreen;
	}

	public void setFullscreen(boolean fullscreen) {
		if(this.fullscreen != fullscreen) {
			this.fullscreen = fullscreen;
			fireConfigChangedEvent(FULLSCREEN, Boolean.toString(fullscreen));
		}
	}

	public LookAndFeel getLookAndFeel() {
		return laf;
	}

	public void setLookAndFeel(LookAndFeel laf) {
		if(this.laf != laf) {
			this.laf = laf;
			fireConfigChangedEvent(LOOKANDFEEL, laf.toString());
		}
	}

	public boolean getWindowDecorations() {
		return windowDecorations;
	}

	public void setWindowDecorations(boolean windowDecorations) {
		if(this.windowDecorations != windowDecorations) {
			this.windowDecorations = windowDecorations;
			fireConfigChangedEvent(WINDOW_DECORATIONS, Boolean.toString(windowDecorations));
		}
	}

	public MidiPortConfig getMidiPort(String name, boolean input) {
		ESLMidiPortConfig eslcfg = eslMidiPortConfig.get(name);
		if(eslcfg != null) {
			return eslcfg;
		}

		MidiPortId id = new MidiPortId(name, input);
		MidiPortConfig cfg = midiPortConfig.get(id);
		if(cfg == null) {
			cfg = new MidiPortConfig(this);
			midiPortConfig.put(id, cfg);
		}
		return cfg;
	}

	public ESLMidiPortConfig getESLMidiPort(String name) {
		ESLMidiPortConfig cfg = eslMidiPortConfig.get(name);
		if(cfg == null) {
			cfg = new ESLMidiPortConfig(this, name);
			eslMidiPortConfig.put(name, cfg);
		}
		return cfg;
	}

	public ESLMidiPortConfig addESLMidiPort(String name) {
		ESLMidiPortConfig cfg = eslMidiPortConfig.get(name);
		if(cfg != null) {
			throw new IllegalArgumentException("name already exists");
		} else {
			cfg = new ESLMidiPortConfig(this, name);
			eslMidiPortConfig.put(name, cfg);
			return cfg;
		}
	}

	public Collection<ESLMidiPortConfig> getESLMidiPorts() {
		return Collections.unmodifiableCollection(eslMidiPortConfig.values());
	}

	public LaserConfig getLaser(String name) {
		return lasers.get(name);
	}

	public LaserConfig getLaser(InterfaceId id) {
		return lasersLookup.get(id);
	}

	public LaserConfig addLaser(String name, InterfaceId id) {
		LaserConfig cfg = lasersLookup.get(id);
		if(cfg == null) {
			if(lasers.containsKey(name)) {
				throw new IllegalArgumentException("laser with given name already exists");
			}
			cfg = new LaserConfig(this, name, id);
			lasers.put(name, cfg);
			lasersLookup.put(id, cfg);
		} else if(lasers.get(name) != cfg) { // reference comparison is intentional
			throw new IllegalArgumentException("laser already exists");
		}
		return cfg;
	}

	public void setDiscoveredLasers(Collection<LaserInfo> discovered) {
		Set<InterfaceId> all = new HashSet<>(lasersLookup.keySet());
		for(LaserInfo laser : discovered) {
			InterfaceId id = laser.getInterfaceId();
			all.remove(id);
			LaserConfig cfg = lasersLookup.get(id);
			if(cfg == null) {
				addLaser(id.toString(), id);
			}
		}

		// clean up lasers which disappeared and have no explicit config attached
		for(InterfaceId id : all) {
			LaserConfig cfg = lasersLookup.get(id);
			if(!cfg.isActive() && cfg.getName().equals(id.toString())) {
				lasersLookup.remove(id);
				lasers.remove(cfg.getName());
			}
		}
	}

	public Collection<LaserConfig> getLasers() {
		return Collections.unmodifiableCollection(lasers.values());
	}

	public LaserAddress addLaserAddress(String hostname) {
		try {
			InetAddress address = InetAddress.getByName(hostname);
			return addLaserAddress(hostname, address);
		} catch(UnknownHostException e) {
			log.info("Failed to resolve hostname " + hostname);
			return addLaserAddress(hostname, null);
		}
	}

	public LaserAddress addLaserAddress(String hostname, InetAddress address) {
		LaserAddress result = new LaserAddress(hostname, address);
		laserAddresses.add(result);
		return result;
	}

	public void removeLaserAddress(String address) {
		laserAddresses.remove(new LaserAddress(address, null));
	}

	public Set<LaserAddress> getLaserAddresses() {
		return Collections.unmodifiableSet(laserAddresses);
	}

	protected boolean rename(ESLMidiPortConfig port, String name) {
		// check for empty name
		if(name == null || name.length() == 0) {
			return false;
		}

		// check if nothing was renamed
		if(port.getAlias().equals(name)) {
			return true;
		}

		// check for name collisions
		if(eslMidiPortConfig.containsKey(name)) {
			return false;
		}

		for(MidiPortConfig cfg : midiPortConfig.values()) {
			if(cfg.getAlias() != null && cfg.getAlias().equals(name)) {
				return false;
			}
		}

		// update in port map
		eslMidiPortConfig.remove(port.getAlias());
		eslMidiPortConfig.put(name, port);

		return true;
	}

	protected boolean rename(LaserConfig laser, String name) {
		// check for empty name
		if(name == null || name.length() == 0) {
			return false;
		}

		// check if nothing was renamed
		if(laser.getName().equals(name)) {
			return true;
		}

		// check for name collisions
		if(lasers.containsKey(name)) {
			return false;
		}

		// update laser map
		lasers.remove(laser.getName());
		lasers.put(name, laser);

		return true;
	}

	protected void delete(ESLMidiPortConfig port) {
		eslMidiPortConfig.remove(port.getAlias());
	}

	protected void delete(LaserConfig laser) {
		lasers.remove(laser.getName());
	}

	public void addConfigChangeListener(ConfigChangeListener listener) {
		listeners.add(listener);
	}

	public void removeConfigChangeListener(ConfigChangeListener listener) {
		listeners.remove(listener);
	}

	protected void fireConfigChangedEvent(String key, String value) {
		for(ConfigChangeListener l : listeners) {
			try {
				l.configChanged(key, value);
			} catch(Throwable t) {
				log.log(Levels.ERROR, "Failed to process config change listener", t);
			}
		}
	}

	protected void fireMidiPortChangedEvent(MidiPortConfig port) {
		for(ConfigChangeListener l : listeners) {
			try {
				l.midiPortChanged(port);
			} catch(Throwable t) {
				log.log(Levels.ERROR, "Failed to process config change listener", t);
			}
		}
	}

	protected void fireLaserChangedEvent(LaserConfig laser) {
		for(ConfigChangeListener l : listeners) {
			try {
				l.laserChanged(laser);
			} catch(Throwable t) {
				log.log(Levels.ERROR, "Failed to process config change listener", t);
			}
		}
	}

	public static Path getConfigPath() {
		String home = System.getProperty("user.home");
		Path path = Paths.get(home, ".emulightrc");
		return path;
	}

	public void write() throws IOException {
		Element xml = new Element("emulight-settings");

		Element pcif = new Element("pcif");
		if(serialLine != null) {
			pcif.addAttribute("port", serialLine);
		}
		xml.addChild(pcif);

		Element desktop = new Element("desktop");
		desktop.addAttribute("outline-drag-mode", Boolean.toString(outlineDrag));
		desktop.addAttribute("fullscreen", Boolean.toString(fullscreen));
		desktop.addAttribute("look-and-feel", laf.toString());
		desktop.addAttribute("window-decorations", Boolean.toString(windowDecorations));
		desktop.addChild(new Element("wallpaper", wallpaper));
		xml.addChild(desktop);

		Element midi = new Element("midi");
		for(Entry<MidiPortId, MidiPortConfig> entry : midiPortConfig.entrySet()) {
			MidiPortId id = entry.getKey();
			MidiPortConfig cfg = entry.getValue();

			if(!cfg.isDefault()) {
				Element port = new Element("port");
				port.addAttribute("name", id.getName());
				port.addAttribute("type", id.isInput() ? "input" : "output");
				if(cfg.getAlias() != null) {
					port.addAttribute("alias", cfg.getAlias());
				}
				port.addAttribute("active", Boolean.toString(cfg.isActive()));

				if(id.isInput()) {
					port.addAttribute("all-bus", Boolean.toString(cfg.isAll()));
				}

				midi.addChild(port);
			}
		}
		for(Entry<String, ESLMidiPortConfig> entry : eslMidiPortConfig.entrySet()) {
			String name = entry.getKey();
			ESLMidiPortConfig cfg = entry.getValue();

			Element port = new Element("esl");
			port.addAttribute("name", name);
			port.addAttribute("type", "output");
			port.addAttribute("active", Boolean.toString(cfg.isActive()));
			port.addAttribute("address", Integer.toString(cfg.getAddress()));
			port.addAttribute("port", Integer.toString(cfg.getPort()));

			midi.addChild(port);
		}
		xml.addChild(midi);

		Element laser = new Element("laser");
		for(LaserAddress addr : laserAddresses) {
			laser.addChild(new Element("address", addr.getHostname()));
		}

		for(Entry<String, LaserConfig> entry : lasers.entrySet()) {
			String name = entry.getKey();
			LaserConfig cfg = entry.getValue();

			if(cfg.getId() == null) {
				continue;
			}

			if(!cfg.isActive() && cfg.getName().equals(cfg.getId().toString())) {
				// nothing to save here
				continue;
			}

			Element ref = new Element("laser");
			ref.addAttribute("name", name);
			ref.addAttribute("id", cfg.getId().toString());
			ref.addAttribute("active", Boolean.toString(cfg.isActive()));
			laser.addChild(ref);
		}
		xml.addChild(laser);

		String data = xml.toString();

		Path path = getConfigPath();
		log.log(Levels.INFO, "Writing configuration to " + path);
		Files.writeString(path, data, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
	}

	public void read() throws IOException {
		Path path = getConfigPath();
		log.log(Levels.INFO, "Reading configuration from " + path);

		byte[] data = Files.readAllBytes(path);
		String cfg = new String(data, StandardCharsets.UTF_8);

		try {
			Element xml = XMLReader.read(cfg);
			parse(xml);
		} catch(ParseException e) {
			log.log(Levels.ERROR, "Failed to parse XML: " + e.getMessage(), e);
			throw new IOException("Failed to parse XML: " + e.getMessage(), e);
		}
	}

	private void parse(Element xml) throws IOException {
		if(!xml.name.equals("emulight-settings")) {
			throw new IOException("not an emulight settings file");
		}

		for(Element node : xml.getChildren()) {
			switch(node.name) {
			case "pcif":
				setSerialLine(node.getAttribute("port"));
				break;
			case "desktop":
				setOutlineDragMode(
						Boolean.parseBoolean(node.getAttribute("outline-drag-mode", "false")));
				setFullscreen(Boolean.parseBoolean(node.getAttribute("fullscreen", "false")));
				setLookAndFeel(LookAndFeel.valueOf(node.getAttribute("look-and-feel", "MOTIF")));
				setWindowDecorations(
						Boolean.parseBoolean(node.getAttribute("window-decorations", "true")));

				for(Element item : node.getChildren()) {
					switch(item.name) {
					case "wallpaper":
						setWallpaper(item.value);
						break;
					}
				}
				break;
			case "midi":
				for(Element item : node.getChildren()) {
					switch(item.name) {
					case "port": {
						String name = item.getAttribute("name");
						boolean input = item.getAttribute("type").equals("input");
						String alias = item.getAttribute("alias");
						boolean active = Boolean.parseBoolean(item.getAttribute("active"));
						boolean all = Boolean
								.parseBoolean(item.getAttribute("all-bus", "false"));

						MidiPortConfig cfg = getMidiPort(name, input);
						cfg.setAlias(alias);
						cfg.setActive(active);
						cfg.setAll(all);
						break;
					}
					case "esl": {
						String name = item.getAttribute("name");
						boolean active = Boolean.parseBoolean(item.getAttribute("active"));
						int address = Integer.parseInt(item.getAttribute("address"));
						int port = Integer.parseInt(item.getAttribute("port"));

						ESLMidiPortConfig cfg = getESLMidiPort(name);
						cfg.setActive(active);
						cfg.setAddress(address);
						cfg.setPort(port);
						break;
					}
					}
				}
				break;
			case "laser":
				for(Element item : node.getChildren()) {
					switch(item.name) {
					case "address": {
						addLaserAddress(item.value);
						break;
					}
					case "laser": {
						String name = item.getAttribute("name");
						InterfaceId id = new InterfaceId(item.getAttribute("id"));
						boolean active = Boolean.parseBoolean(item.getAttribute("active"));

						LaserConfig cfg = addLaser(name, id);
						cfg.setActive(active);
						break;
					}
					}
				}
				break;
			}
		}
	}

	private static class MidiPortId {
		private final String name;
		private final boolean input;

		public MidiPortId(String name, boolean input) {
			this.name = name;
			this.input = input;
		}

		public String getName() {
			return name;
		}

		public boolean isInput() {
			return input;
		}

		@Override
		public boolean equals(Object o) {
			if(o == null) {
				return false;
			}
			if(!(o instanceof MidiPortId)) {
				return false;
			}

			MidiPortId p = (MidiPortId) o;
			return name.equals(p.name) && input == p.input;
		}

		@Override
		public int hashCode() {
			return name.hashCode() ^ Boolean.hashCode(input);
		}
	}

	public static class MidiPortConfig {
		protected final SystemConfiguration cfg;

		protected boolean active;
		protected String alias;
		protected boolean all;

		protected MidiPortConfig(SystemConfiguration cfg) {
			this.cfg = cfg;
		}

		public String getAlias() {
			return alias;
		}

		public void setAlias(String alias) {
			this.alias = alias;
			cfg.fireMidiPortChangedEvent(this);
		}

		public boolean isActive() {
			return active;
		}

		public void setActive(boolean active) {
			this.active = active;
			cfg.fireMidiPortChangedEvent(this);
		}

		public boolean isDefault() {
			return alias == null && !active;
		}

		public boolean isAll() {
			return all;
		}

		public void setAll(boolean all) {
			this.all = all;
			cfg.fireMidiPortChangedEvent(this);
		}
	}

	public static class ESLMidiPortConfig extends MidiPortConfig {
		private int address;
		private int port;

		protected ESLMidiPortConfig(SystemConfiguration cfg, String name) {
			super(cfg);
			this.alias = name;
		}

		@Override
		public void setAlias(String alias) {
			if(cfg.rename(this, alias)) {
				this.alias = alias;
				cfg.fireMidiPortChangedEvent(this);
			} else {
				throw new IllegalArgumentException("invalid name");
			}
		}

		public int getAddress() {
			return address;
		}

		public void setAddress(int address) {
			this.address = address;
			cfg.fireMidiPortChangedEvent(this);
		}

		public int getPort() {
			return port;
		}

		public void setPort(int port) {
			this.port = port;
			cfg.fireMidiPortChangedEvent(this);
		}

		public void delete() {
			cfg.delete(this);
		}
	}

	public static class LaserConfig {
		private final SystemConfiguration cfg;

		private final InterfaceId id;
		private String name;
		protected boolean active;

		protected LaserConfig(SystemConfiguration cfg, String name, InterfaceId id) {
			this.cfg = cfg;
			this.name = name;
			this.id = id;
		}

		public InterfaceId getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			if(cfg.rename(this, name)) {
				this.name = name;
				cfg.fireLaserChangedEvent(this);
			} else {
				throw new IllegalArgumentException("invalid name");
			}
		}

		public boolean isActive() {
			return active;
		}

		public void setActive(boolean active) {
			this.active = active;
			cfg.fireLaserChangedEvent(this);
		}

		public void delete() {
			cfg.delete(this);
		}
	}

	public static class LaserAddress {
		public final String hostname;
		public final InetAddress address;

		public LaserAddress(String hostname, InetAddress address) {
			this.hostname = hostname;
			this.address = address;
		}

		public String getHostname() {
			return hostname;
		}

		public InetAddress getAddress() {
			return address;
		}

		@Override
		public boolean equals(Object o) {
			if(o == null) {
				return false;
			}

			if(!(o instanceof LaserAddress)) {
				return false;
			}

			LaserAddress a = (LaserAddress) o;
			return a.hostname.equals(hostname);
		}

		@Override
		public int hashCode() {
			return hostname.hashCode();
		}
	}

	public static enum LookAndFeel {
		// @formatter:off
		MOTIF("Motif", "com.unknown.plaf.motif.MotifLookAndFeel"),
		WINDOWS("Windows", "com.unknown.plaf.windows.classic.WindowsLookAndFeel"),
		METAL("Metal", "javax.swing.plaf.metal.MetalLookAndFeel"),
		SYSTEM("System", null);
		// @formatter:on

		private final String name;
		private final String clazz;

		private LookAndFeel(String name, String clazz) {
			this.name = name;
			this.clazz = clazz;
		}

		public String getName() {
			return name;
		}

		public String getUIClassName() {
			return clazz;
		}

		public String getUIClass() {
			if(clazz == null) {
				return UIManager.getSystemLookAndFeelClassName();
			} else {
				return clazz;
			}
		}
	}
}
