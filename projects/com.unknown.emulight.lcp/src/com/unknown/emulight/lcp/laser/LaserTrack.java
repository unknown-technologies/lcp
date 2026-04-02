package com.unknown.emulight.lcp.laser;

import java.io.IOException;

import com.unknown.emulight.lcp.project.Project;
import com.unknown.emulight.lcp.project.Track;
import com.unknown.math.g3d.Mtx44;
import com.unknown.net.shownet.Laser;
import com.unknown.xml.dom.Element;

public class LaserTrack extends Track<LaserPart> {
	private LaserReference laser;

	private Mtx44 projection = new Mtx44();
	private Mtx44 trackProjection = new Mtx44();

	private boolean mirrorX = false;
	private boolean mirrorY = false;

	public LaserTrack(Project project, String name) {
		this(project, name, null);
	}

	public LaserTrack(Project project, String name, String laserName) {
		super(Track.LASER, project, name);
		this.laser = project.getSystem().getLaser(laserName);
	}

	public LaserReference getLaserReference() {
		return laser;
	}

	public void setLaserReference(LaserReference laser) {
		this.laser = laser;
	}

	public Laser getLaser() {
		if(laser == null) {
			return null;
		} else {
			return laser.get();
		}
	}

	private void updateProjection() {
		trackProjection = projection.scaleApply(mirrorX ? -1 : 1, mirrorY ? -1 : 1, 1);
	}

	public Mtx44 getProjection() {
		return trackProjection;
	}

	public void setProjection(Mtx44 projection) {
		this.projection = projection;
		updateProjection();
	}

	public boolean isMirrorX() {
		return mirrorX;
	}

	public void setMirrorX(boolean mirror) {
		mirrorX = mirror;
		updateProjection();
	}

	public boolean isMirrorY() {
		return mirrorY;
	}

	public void setMirrorY(boolean mirror) {
		mirrorY = mirror;
		updateProjection();
	}

	@Override
	protected void readTrack(Element xml) throws IOException {
		String name = xml.getAttribute("laser");
		if(name != null) {
			laser = getProject().getSystem().getLaser(name);
		}
		mirrorX = Boolean.parseBoolean(xml.getAttribute("mirror-x", "false"));
		mirrorY = Boolean.parseBoolean(xml.getAttribute("mirror-y", "false"));
		updateProjection();
	}

	@Override
	protected void writeTrack(Element xml) {
		if(laser != null) {
			xml.addAttribute("laser", laser.getName());
		}
		xml.addAttribute("mirror-x", Boolean.toString(mirrorX));
		xml.addAttribute("mirror-y", Boolean.toString(mirrorY));
	}

	@Override
	protected LaserPart createPart() {
		return new LaserPart(getProject());
	}

	@Override
	public LaserTrack clone() {
		LaserTrack track = new LaserTrack(getProject(), getName());
		copy(track);
		track.setLaserReference(laser);
		return track;
	}
}
