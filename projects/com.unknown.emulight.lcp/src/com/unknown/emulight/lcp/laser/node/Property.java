package com.unknown.emulight.lcp.laser.node;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import com.unknown.math.g3d.Vec3;
import com.unknown.xml.dom.Element;

public class Property<T> implements Cloneable {
	private final String name;
	private final Class<T> type;
	private final boolean isStatic;
	private final T minimum;
	private final T maximum;
	private final T defaultValue;
	private boolean automation = true;
	private NavigableMap<Integer, T> values = new TreeMap<>();

	private final PhaseIntegrator<T> integrator;

	public Property(String name, Class<T> clazz) {
		if(clazz == null) {
			throw new IllegalArgumentException("clazz is null");
		}
		if(!clazz.equals(String.class) && !clazz.equals(Boolean.class) && !clazz.equals(CachedImage.class)) {
			throw new IllegalArgumentException("minimum/maximum is required");
		}
		this.name = name;
		this.type = clazz;
		this.isStatic = false;
		this.minimum = null;
		this.maximum = null;
		this.defaultValue = null;
		this.integrator = null;
	}

	public Property(String name, Class<T> clazz, boolean isStatic) {
		if(clazz == null) {
			throw new IllegalArgumentException("clazz is null");
		}
		if(!clazz.equals(String.class) && !clazz.equals(Boolean.class) && !clazz.equals(CachedImage.class)) {
			throw new IllegalArgumentException("minimum/maximum is required");
		}
		this.name = name;
		this.type = clazz;
		this.isStatic = isStatic;
		this.minimum = null;
		this.maximum = null;
		this.defaultValue = null;
		this.integrator = null;
	}

	public Property(String name, T value) {
		this(name, value, true);
	}

	@SuppressWarnings("unchecked")
	public Property(String name, T value, boolean automation) {
		if(value == null) {
			throw new IllegalArgumentException("value is null");
		}
		if(!(value instanceof String) && !(value instanceof Boolean) && !(value instanceof Color3)) {
			throw new IllegalArgumentException("minimum/maximum is required");
		}
		this.name = name;
		values.put(0, value);
		this.type = (Class<T>) value.getClass();
		this.isStatic = false;
		this.minimum = null;
		this.maximum = null;
		this.defaultValue = value;
		this.automation = automation;
		this.integrator = null;
	}

	public Property(String name, T value, T minimum, T maximum) {
		this(name, value, minimum, maximum, true, null);
	}

	public Property(String name, T value, T minimum, T maximum, PhaseIntegrator<T> integrator) {
		this(name, value, minimum, maximum, true, integrator);
	}

	public Property(String name, T value, T minimum, T maximum, boolean automation) {
		this(name, value, minimum, maximum, automation, null);
	}

	@SuppressWarnings("unchecked")
	public Property(String name, T value, T minimum, T maximum, boolean automation, PhaseIntegrator<T> integrator) {
		if(value == null) {
			throw new IllegalArgumentException("value is null");
		}
		if(minimum == null) {
			throw new IllegalArgumentException("minimum is null");
		}
		if(maximum == null) {
			throw new IllegalArgumentException("maximum is null");
		}

		this.name = name;
		values.put(0, value);
		this.type = (Class<T>) value.getClass();
		this.isStatic = false;
		this.minimum = minimum;
		this.maximum = maximum;
		this.defaultValue = value;
		this.automation = automation;
		this.integrator = integrator;

		if(integrator != null) {
			integrator.setProperty(this);
			integrator.recompute();
		}
	}

	public String getName() {
		return name;
	}

	public T getValue(int time) {
		if(isStatic || !automation) {
			return values.get(0);
		} else {
			return getInterpolatedValue(time);
		}
	}

	public T getRawValue(int time) {
		if(isStatic || !automation) {
			return values.get(0);
		} else {
			return values.get(time);
		}
	}

	public T getInterpolatedValue(int time) {
		Entry<Integer, T> floor = values.floorEntry(time);
		Entry<Integer, T> ceil = values.ceilingEntry(time);
		if(floor == null && ceil == null) {
			return null;
		} else if(floor != null && ceil == null) {
			// only a smaller value available
			return floor.getValue();
		} else if(floor == null && ceil != null) {
			// only a bigger value available
			return ceil.getValue();
		} else if(floor.getKey().equals(ceil.getKey())) {
			// exact match
			return floor.getValue();
		} else {
			// interpolate
			return PropertyInterpolator.interpolate(type, floor.getKey(), floor.getValue(), ceil.getKey(),
					ceil.getValue(), time);
		}
	}

	public void setValue(int time, T value) {
		if(isStatic || !automation) {
			values.put(0, value);
		} else {
			values.put(time, value);
		}
		if(integrator != null) {
			integrator.recompute();
		}
	}

	public void unsetValue(int time) {
		if(values.size() > 1) {
			values.remove(time);
			if(integrator != null) {
				integrator.recompute();
			}
		}
	}

	public NavigableMap<Integer, T> getValues() {
		return Collections.unmodifiableNavigableMap(values);
	}

	public int getCount() {
		return values.size();
	}

	public Class<T> getType() {
		return type;
	}

	public boolean isStatic() {
		return isStatic;
	}

	public boolean isAutomation() {
		return automation;
	}

	public void setAutomation(boolean automation) {
		this.automation = automation;
	}

	public T getMinimum() {
		return minimum;
	}

	public T getMaximum() {
		return maximum;
	}

	public void clear() {
		values.clear();
		values.put(0, defaultValue);
	}

	public PhaseIntegrator<T> getIntegrator() {
		return integrator;
	}

	public void copy(Property<T> other) {
		other.values.clear();
		other.values.putAll(values);
	}

	@Override
	public Property<T> clone() {
		Property<T> prop = new Property<>(name, type, isStatic);
		prop.values.putAll(values);
		return prop;
	}

	public Element write() {
		Element xml = new Element("property");
		xml.addAttribute("name", name);
		xml.addAttribute("type", type.getSimpleName());
		xml.addAttribute("automation", automation ? "true" : "false");
		for(Entry<Integer, T> entry : values.sequencedEntrySet()) {
			int time = entry.getKey();
			T value = entry.getValue();
			Element point = new Element("point");
			point.addAttribute("time", Integer.toString(time));
			if(type.equals(Vec3.class)) {
				Vec3 vec = (Vec3) value;
				point.addAttribute("x", Double.toString(vec.x));
				point.addAttribute("y", Double.toString(vec.y));
				point.addAttribute("z", Double.toString(vec.z));
			} else if(type.equals(Color3.class)) {
				Color3 color = (Color3) value;
				point.addAttribute("r", Double.toString(color.getRed()));
				point.addAttribute("g", Double.toString(color.getGreen()));
				point.addAttribute("b", Double.toString(color.getBlue()));
			} else if(type.equals(CachedImage.class)) {
				CachedImage image = (CachedImage) value;
				File file = image.getFile();
				point.addAttribute("file", file.toString());
			} else {
				point.addAttribute("value", value.toString());
			}
			xml.addChild(point);
		}
		return xml;
	}

	@SuppressWarnings("unchecked")
	public void read(Element xml) throws IOException {
		if(!xml.name.equals("property")) {
			throw new IOException("not a property");
		}

		if(!name.equals(xml.getAttribute("name"))) {
			throw new IOException("wrong property");
		}

		if(!type.getSimpleName().equals(xml.getAttribute("type"))) {
			throw new IOException("wrong type");
		}

		automation = xml.getAttribute("automation", "true").equals("true");
		values.clear();
		for(Element point : xml.getChildren()) {
			if(!point.name.equals("point")) {
				continue;
			}

			int time;
			try {
				time = Integer.parseInt(point.getAttribute("time"));
			} catch(NumberFormatException e) {
				throw new IOException("invalid time: " + point.getAttribute("time"), e);
			}

			if(type.equals(Vec3.class)) {
				try {
					double x = Double.parseDouble(point.getAttribute("x"));
					double y = Double.parseDouble(point.getAttribute("y"));
					double z = Double.parseDouble(point.getAttribute("z"));
					values.put(time, (T) new Vec3(x, y, z));
				} catch(NumberFormatException e) {
					throw new IOException("invalid coordinate: " + e.getMessage(), e);
				}
			} else if(type.equals(Color3.class)) {
				try {
					double r = Double.parseDouble(point.getAttribute("r"));
					double g = Double.parseDouble(point.getAttribute("g"));
					double b = Double.parseDouble(point.getAttribute("b"));
					values.put(time, (T) new Color3(r, g, b));
				} catch(NumberFormatException e) {
					throw new IOException("invalid color: " + e.getMessage(), e);
				}
			} else if(type.equals(Double.class)) {
				try {
					double value = Double.parseDouble(point.getAttribute("value"));
					values.put(time, (T) (Double) value);
				} catch(NumberFormatException e) {
					throw new IOException("invalid value: " + e.getMessage(), e);
				}
			} else if(type.equals(Integer.class)) {
				try {
					int value = Integer.parseInt(point.getAttribute("value"));
					values.put(time, (T) (Integer) value);
				} catch(NumberFormatException e) {
					throw new IOException("invalid value: " + e.getMessage(), e);
				}
			} else if(type.equals(Boolean.class)) {
				boolean value = Boolean.parseBoolean(point.getAttribute("value"));
				values.put(time, (T) (Boolean) value);
			} else if(type.equals(String.class)) {
				String value = point.getAttribute("value");
				values.put(time, (T) value);
			} else if(type.equals(CachedImage.class)) {
				String filename = point.getAttribute("file");
				File file = new File(filename);
				CachedImage image = new CachedImage(file);
				values.put(time, (T) image);
			} else {
				throw new IOException("Unknown type " + type.getSimpleName());
			}
		}
		if(integrator != null) {
			integrator.recompute();
		}
	}
}
