package com.unknown.emulight.lcp.project;

import com.unknown.xml.dom.Element;

public class PartContainer<T extends AbstractPart> {
	private final T part;

	private final long time;
	private long trimStart;
	private long length;

	private final Track<T> track;

	public PartContainer(Track<T> track, T part, long time) {
		this.track = track;
		this.part = part;
		this.time = time;
		this.length = part.getLength();
	}

	public T getPart() {
		return part;
	}

	public long getTime() {
		return time;
	}

	public long getTrimStart() {
		return trimStart;
	}

	public void setTrimStart(long trimStart) {
		this.trimStart = trimStart;
	}

	public long getLength() {
		return length;
	}

	public void setLength(long length) {
		this.length = length;
	}

	public long getStart() {
		return time;
	}

	public long getEnd() {
		return time - trimStart + length;
	}

	public boolean contains(long t) {
		return t >= getStart() && t < getEnd();
	}

	public boolean containsEvent(long localTime) {
		return localTime >= trimStart && localTime < length;
	}

	public long getLocalTime(long t) {
		return t - getStart() + trimStart;
	}

	PartContainer<T> moveInternal(long newTime) {
		PartContainer<T> container = new PartContainer<>(track, part, newTime);
		container.trimStart = trimStart;
		container.length = length;
		return container;
	}

	public PartContainer<T> move(long newTime) {
		return track.movePart(newTime, this);
	}

	public PartContainer<T> clone(long newTime) {
		return track.clonePart(newTime, this);
	}

	public PartContainer<T> link(long newTime) {
		return track.linkPart(newTime, this);
	}

	public PartContainer<T> copyAt(long newTime) {
		return moveInternal(newTime);
	}

	PartContainer<T> cloneAt(long newTime) {
		@SuppressWarnings("unchecked")
		PartContainer<T> container = new PartContainer<>(track, (T) part.clone(), newTime);
		container.trimStart = trimStart;
		container.length = length;
		return container;
	}

	public void delete() {
		track.removePart(this);
	}

	public Track<T> getTrack() {
		return track;
	}

	public void read(Element xml) {
		trimStart = Long.parseLong(xml.getAttribute("trimStart"));
		length = Long.parseLong(xml.getAttribute("length"));
	}

	public void write(Element xml) {
		xml.addAttribute("trimStart", Long.toString(trimStart));
		xml.addAttribute("length", Long.toString(length));
	}
}
