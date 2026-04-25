package com.unknown.emulight.lcp.ui.live;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.unknown.emulight.lcp.laser.LaserCue;
import com.unknown.emulight.lcp.laser.LaserPart;
import com.unknown.emulight.lcp.laser.LaserReference;
import com.unknown.emulight.lcp.laser.LaserTrack;
import com.unknown.emulight.lcp.live.Cue;
import com.unknown.emulight.lcp.live.CuePool;
import com.unknown.emulight.lcp.project.AbstractPart;
import com.unknown.emulight.lcp.project.PartContainer;
import com.unknown.emulight.lcp.project.Project;
import com.unknown.emulight.lcp.project.Track;

public class LivePartTransfer {
	public static boolean canTransfer(PartContainer<?> container) {
		switch(container.getTrack().getType()) {
		case Track.LASER:
			return true;
		default:
			return false;
		}
	}

	public static void addPart(PartContainer<?> container, CuePool pool) {
		Project project = pool.getProject();
		AbstractPart part = container.getPart();
		if(part instanceof LaserPart) {
			LaserPart laserPart = (LaserPart) part;
			int color = container.getTrack().getColor();

			LaserCue cue = new LaserCue(project, laserPart);
			cue.setColor(color);

			cue.setLength((int) container.getLength());

			LaserReference laser = ((LaserTrack) container.getTrack()).getLaserReference();
			if(laser != null) {
				cue.addLaser(laser);
			}

			pool.addCue(cue);
		}
	}

	public static void addParts(Project project, Set<PartContainer<?>> parts) {
		Set<AbstractPart> uniqueParts = new HashSet<>();
		Map<AbstractPart, List<PartContainer<?>>> map = new HashMap<>();
		for(PartContainer<?> container : parts) {
			uniqueParts.add(container.getPart());
			List<PartContainer<?>> containers = map.get(container.getPart());
			if(containers == null) {
				containers = new ArrayList<>();
				map.put(container.getPart(), containers);
			}
			containers.add(container);
		}

		CuePool pool = project.getCuePool();
		Set<AbstractPart> existingParts = new HashSet<>();
		for(Cue<?> cue : pool.getCues()) {
			existingParts.add(cue.getPart());
		}

		// TODO: update this later, once more part types become available
		Set<CueInfo> cues = new HashSet<>();
		uniqueParts.stream().filter(part -> !existingParts.contains(part)).forEach(part -> {
			if(part instanceof LaserPart) {
				LaserPart laserPart = (LaserPart) part;
				List<PartContainer<?>> containers = map.get(laserPart);
				int color = containers.get(0).getTrack().getColor();

				LaserCue cue = new LaserCue(project, laserPart);
				cue.setColor(color);

				long maxlen = 0;
				long firstTick = Long.MAX_VALUE;
				for(PartContainer<?> container : containers) {
					long len = container.getLength();
					if(len > maxlen) {
						maxlen = len;
					}

					long tick = container.getStart();
					if(tick < firstTick) {
						firstTick = tick;
					}

					LaserReference laser = ((LaserTrack) container.getTrack()).getLaserReference();
					if(laser != null) {
						cue.addLaser(laser);
					}
				}

				cue.setLength((int) maxlen);
				cues.add(new CueInfo(cue, firstTick));
			}
		});

		// now add the cues sorted by the first part container's time
		cues.stream().sorted((a, b) -> Long.compare(a.firstTick, b.firstTick))
				.forEach(info -> pool.addCue(info.cue));
	}

	private static class CueInfo {
		private final Cue<?> cue;
		private final long firstTick;

		private CueInfo(Cue<?> cue, long firstTick) {
			this.cue = cue;
			this.firstTick = firstTick;
		}
	}

	public static void transferFromTracks(Project project) {
		Set<PartContainer<?>> parts = new HashSet<>();
		for(Track<?> track : project.getTracks()) {
			for(PartContainer<?> container : track.getParts()) {
				parts.add(container);
			}
		}

		addParts(project, parts);
	}
}
