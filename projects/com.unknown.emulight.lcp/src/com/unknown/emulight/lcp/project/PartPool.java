package com.unknown.emulight.lcp.project;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class PartPool {
	private final Map<AbstractPart, PartInfo> uniqueParts = new HashMap<>();
	private final Map<Integer, AbstractPart> partRef = new HashMap<>();

	private int nextId = 0;

	public void add(AbstractPart part, int type) {
		if(!uniqueParts.containsKey(part)) {
			uniqueParts.put(part, new PartInfo(nextId, type));
			partRef.put(nextId, part);
			nextId++;
		}
	}

	public int getId(AbstractPart part) {
		PartInfo info = uniqueParts.get(part);
		if(info == null) {
			throw new NoSuchElementException("part not found");
		}
		return info.id;
	}

	public AbstractPart get(int id) {
		return partRef.get(id);
	}

	public boolean contains(AbstractPart part) {
		return uniqueParts.containsKey(part);
	}

	public int size() {
		return uniqueParts.size();
	}

	public Map<AbstractPart, PartInfo> getUniqueParts() {
		return Collections.unmodifiableMap(uniqueParts);
	}

	public static class PartInfo {
		private final int id;
		private final int type;

		private PartInfo(int id, int type) {
			this.id = id;
			this.type = type;
		}

		public int getId() {
			return id;
		}

		public int getType() {
			return type;
		}
	}
}
