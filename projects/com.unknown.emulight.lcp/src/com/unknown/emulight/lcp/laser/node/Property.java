package com.unknown.emulight.lcp.laser.node;

public class Property<T> implements Cloneable {
	private final String name;
	private final Class<T> type;
	private T value;

	public Property(String name, Class<T> clazz) {
		this.name = name;
		this.value = null;
		this.type = clazz;
	}

	@SuppressWarnings("unchecked")
	public Property(String name, T value) {
		this.name = name;
		this.value = value;
		this.type = (Class<T>) value.getClass();
	}

	public String getName() {
		return name;
	}

	public T getValue() {
		return value;
	}

	public void setValue(T value) {
		this.value = value;
	}

	public Class<T> getType() {
		return type;
	}

	@Override
	public Property<T> clone() {
		Property<T> prop = new Property<>(name, type);
		prop.value = value;
		return prop;
	}
}
