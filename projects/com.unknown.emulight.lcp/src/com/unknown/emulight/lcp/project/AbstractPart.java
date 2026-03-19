package com.unknown.emulight.lcp.project;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.unknown.xml.dom.Element;

public abstract class AbstractPart {
	private String name;
	private final Set<PartContainer<? extends AbstractPart>> references = new HashSet<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public abstract long getLength();

	public abstract void read(Element xml) throws IOException;

	public abstract void write(Element xml);

	@Override
	public abstract AbstractPart clone();

	protected void copy(AbstractPart other) {
		other.name = name;
	}

	void addReference(PartContainer<? extends AbstractPart> container) {
		references.add(container);
	}

	void removeReference(PartContainer<? extends AbstractPart> container) {
		references.remove(container);
	}

	public Set<PartContainer<? extends AbstractPart>> getReferences() {
		return Collections.unmodifiableSet(references);
	}

	public int getRefCount() {
		return references.size();
	}
}
