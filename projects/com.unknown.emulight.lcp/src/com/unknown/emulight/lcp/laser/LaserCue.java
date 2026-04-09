package com.unknown.emulight.lcp.laser;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.unknown.emulight.lcp.laser.node.Node;
import com.unknown.emulight.lcp.live.Cue;
import com.unknown.emulight.lcp.project.EmulightSystem;
import com.unknown.emulight.lcp.project.PartPool;
import com.unknown.emulight.lcp.project.Project;
import com.unknown.emulight.lcp.project.SystemConfiguration.LaserConfig;
import com.unknown.net.shownet.Laser;
import com.unknown.xml.dom.Element;

public class LaserCue extends Cue<LaserPart> {
	private Set<LaserRef> lasers = new HashSet<>();

	public LaserCue(Project project, LaserPart part) {
		super(LASER, project, part);
	}

	public Node getRootNode() {
		return getPart().getRoot();
	}

	public void addLaser(LaserReference laser) {
		lasers.add(new LaserRef(project.getSystem(), laser));
	}

	public void removeLaser(LaserReference laser) {
		lasers.remove(laser);
	}

	public Set<LaserRef> getLasers() {
		return Collections.unmodifiableSet(lasers);
	}

	@Override
	public void play(double bpm) {
		LaserProcessor processor = project.getSystem().getLaserProcessor();
		for(LaserRef ref : lasers) {
			Laser laser = ref.get();
			if(laser != null) {
				processor.setCurrentClip(laser, part, bpm, project.getPPQ(), length, ref.mirrorX,
						ref.mirrorY);
			}
		}
	}

	@Override
	public void stop() {
		LaserProcessor processor = project.getSystem().getLaserProcessor();
		for(LaserReference ref : lasers) {
			Laser laser = ref.get();
			if(laser != null) {
				LaserPart current = processor.getCurrentClip(laser);
				if(current == part) {
					processor.clearCurrentClip(laser);
				}
			}
		}
	}

	@Override
	public void read(Element xml) throws IOException {
		super.read(xml);

		EmulightSystem sys = project.getSystem();
		lasers.clear();

		for(Element e : xml.getChildren()) {
			if(e.name.equals("laser")) {
				String name = e.getAttribute("name");
				boolean mirrorX = Boolean.parseBoolean(e.getAttribute("mirror-x", "false"));
				boolean mirrorY = Boolean.parseBoolean(e.getAttribute("mirror-y", "false"));
				LaserRef ref = new LaserRef(sys, name);
				ref.mirrorX = mirrorX;
				ref.mirrorY = mirrorY;
				lasers.add(ref);
			}
		}
	}

	@Override
	public Element write(PartPool pool) {
		Element xml = super.write(pool);
		for(LaserRef laser : lasers) {
			Element laserxml = new Element("laser");
			laserxml.addAttribute("name", laser.getName());
			laserxml.addAttribute("mirror-x", Boolean.toString(laser.mirrorX));
			laserxml.addAttribute("mirror-y", Boolean.toString(laser.mirrorY));
			xml.addChild(laserxml);
		}
		return xml;
	}

	public static class LaserRef extends LaserReference {
		private boolean mirrorX = false;
		private boolean mirrorY = false;

		private LaserRef(EmulightSystem sys, LaserReference ref) {
			super(sys, ref.getName());
		}

		public LaserRef(EmulightSystem sys, LaserConfig cfg) {
			super(sys, cfg);
		}

		public LaserRef(EmulightSystem sys, String name) {
			super(sys, name);
		}

		public boolean isMirrorX() {
			return mirrorX;
		}

		public void setMirrorX(boolean mirror) {
			mirrorX = mirror;
		}

		public boolean isMirrorY() {
			return mirrorY;
		}

		public void setMirrorY(boolean mirror) {
			mirrorY = mirror;
		}
	}
}
