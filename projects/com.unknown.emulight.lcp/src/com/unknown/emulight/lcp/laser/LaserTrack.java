package com.unknown.emulight.lcp.laser;

import java.io.IOException;
import java.util.List;

import com.unknown.emulight.lcp.project.PartContainer;
import com.unknown.emulight.lcp.project.Project;
import com.unknown.emulight.lcp.project.Track;
import com.unknown.math.g3d.Mtx44;
import com.unknown.net.shownet.Laser;
import com.unknown.net.shownet.Point;
import com.unknown.xml.dom.Element;

public class LaserTrack extends Track<LaserPart> {
	private LaserReference laser;

	private Mtx44 projection = new Mtx44();

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

	public Mtx44 getProjection() {
		return projection;
	}

	public void setProjection(Mtx44 projection) {
		this.projection = projection;
	}

	public void render(long time) throws IOException {
		if(laser == null) {
			return;
		}

		PartContainer<LaserPart> ref = getFloorPart(time);
		if(ref == null) {
			return;
		}

		LaserPart part = ref.getPart();

		long t = time - ref.getTime();
		if(t < ref.getLength()) {
			Laser l = laser.get();
			if(l != null) {
				if(t > part.getLength()) {
					if(part.isLoop()) {
						t %= part.getLength();
					} else {
						t = part.getLength() - 1;
					}
				}

				double vol = getVolume();
				List<Point> points = part.render((int) t, projection, Mtx44.scale(vol, vol, vol));
				l.sendFrame(points, part.getSpeed());
			}
		}
	}

	@Override
	protected void readTrack(Element xml) throws IOException {
		String name = xml.getAttribute("laser");
		if(name != null) {
			laser = getProject().getSystem().getLaser(name);
		}
	}

	@Override
	protected void writeTrack(Element xml) {
		if(laser != null) {
			xml.addAttribute("laser", laser.getName());
		}
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
