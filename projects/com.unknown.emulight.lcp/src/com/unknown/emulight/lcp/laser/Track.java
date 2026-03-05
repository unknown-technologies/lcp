package com.unknown.emulight.lcp.laser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import com.unknown.math.g3d.Mtx44;
import com.unknown.net.shownet.Laser;
import com.unknown.net.shownet.Point;

public class Track {
	private final Project project;

	private LaserReference laser;

	private String name;
	private Mtx44 projection = new Mtx44();
	private NavigableMap<Integer, Clip> clips = new TreeMap<>();

	public Track(Project project, LaserReference laser) {
		this.project = project;
		this.laser = laser;
	}

	public Project getProject() {
		return project;
	}

	public LaserReference getLaserReference() {
		return laser;
	}

	public void setLaserReference(LaserReference laser) {
		this.laser = laser;
	}

	public Laser getLaser() {
		return laser.get();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Mtx44 getProjection() {
		return projection;
	}

	public void setProjection(Mtx44 projection) {
		this.projection = projection;
	}

	public List<Clip> getClips() {
		return new ArrayList<>(clips.sequencedValues());
	}

	public void addClip(int time, Clip clip) {
		clips.put(time, clip);
	}

	public void removeClip(int time) {
		clips.remove(time);
	}

	public void render(int time) throws IOException {
		Entry<Integer, Clip> entry = clips.floorEntry(time);
		if(entry == null) {
			return;
		}

		Clip clip = entry.getValue();

		int t = time - entry.getKey();
		List<Point> points = clip.render(t, projection, new Mtx44());
		Laser l = laser.get();
		if(l != null) {
			l.sendFrame(points, 1000);
		}
	}
}
