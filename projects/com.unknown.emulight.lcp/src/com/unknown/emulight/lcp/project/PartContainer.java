package com.unknown.emulight.lcp.project;

import java.io.IOException;

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

	PartContainer<T> moveInternal(long newTime, Track<T> target) {
		PartContainer<T> container = new PartContainer<>(target, part, newTime);
		container.trimStart = trimStart;
		container.length = length;
		return container;
	}

	public PartContainer<T> move(long newTime) {
		return track.movePart(newTime, this);
	}

	public PartContainer<T> move(long newTime, Track<T> target) {
		track.removePart(this);
		return target.linkPart(newTime, this);
	}

	public PartContainer<T> clone(long newTime) {
		return track.clonePart(newTime, this);
	}

	public PartContainer<T> clone(long newTime, Track<T> target) {
		return target.clonePart(newTime, this);
	}

	public PartContainer<T> link(long newTime) {
		return track.linkPart(newTime, this);
	}

	public PartContainer<T> link(long newTime, Track<T> target) {
		return target.linkPart(newTime, this);
	}

	public PartContainer<T> copyAt(long newTime) {
		return moveInternal(newTime, track);
	}

	public PartContainer<T> copyAt(long newTime, Track<T> target) {
		return moveInternal(newTime, target);
	}

	PartContainer<T> cloneAt(long newTime) {
		return cloneAt(newTime, track);
	}

	PartContainer<T> cloneAt(long newTime, Track<T> target) {
		@SuppressWarnings("unchecked")
		PartContainer<T> container = new PartContainer<>(target, (T) part.clone(), newTime);
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

	public void read(Element xml) throws IOException {
		try {
			trimStart = Long.parseLong(xml.getAttribute("trimStart", "0"));
		} catch(NumberFormatException e) {
			throw new IOException("invalid trimStart");
		}
		try {
			length = Long.parseLong(xml.getAttribute("length"));
		} catch(NumberFormatException e) {
			throw new IOException("invalid length");
		}
	}

	public void write(Element xml) {
		xml.addAttribute("trimStart", Long.toString(trimStart));
		xml.addAttribute("length", Long.toString(length));
	}
}
