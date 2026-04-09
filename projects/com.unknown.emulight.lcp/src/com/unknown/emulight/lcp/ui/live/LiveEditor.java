package com.unknown.emulight.lcp.ui.live;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

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

@SuppressWarnings("serial")
public class LiveEditor extends JPanel {
	private final static int SPINNER_WIDTH = 75;

	private final Project project;
	private final CueList cueList;

	public LiveEditor(Project project) {
		this.project = project;

		setLayout(new BorderLayout());

		JPanel options = new JPanel(new FlowLayout(FlowLayout.CENTER));

		JSpinner bpm = new JSpinner(new SpinnerNumberModel(120.0, 1.0, 600.0, 1.0));
		Dimension bpmSize = bpm.getMinimumSize();
		Dimension bpmMinsz = new Dimension(SPINNER_WIDTH, bpmSize.height);
		bpm.setMinimumSize(bpmMinsz);
		bpm.setPreferredSize(bpmMinsz);

		options.add(new JLabel("BPM:"));
		options.add(bpm);
		add(BorderLayout.NORTH, options);

		cueList = new CueList(project.getCuePool());
		add(BorderLayout.CENTER, cueList);

		bpm.addChangeListener(e -> {
			double d = (double) bpm.getValue();
			cueList.setBPM(d);
		});

		JButton transferButton = new JButton("Transfer from Tracks");
		transferButton.addActionListener(e -> transferFromTracks());

		JButton deleteAll = new JButton("Delete All");
		deleteAll.addActionListener(e -> deleteAll());

		JPanel buttons = new JPanel(new FlowLayout());
		buttons.add(transferButton);
		buttons.add(deleteAll);

		add(BorderLayout.SOUTH, buttons);
	}

	public void transferFromTracks() {
		Set<AbstractPart> uniqueParts = new HashSet<>();
		Map<AbstractPart, List<PartContainer<?>>> map = new HashMap<>();
		for(Track<?> track : project.getTracks()) {
			for(PartContainer<?> container : track.getParts()) {
				uniqueParts.add(container.getPart());
				List<PartContainer<?>> containers = map.get(container.getPart());
				if(containers == null) {
					containers = new ArrayList<>();
					map.put(container.getPart(), containers);
				}
				containers.add(container);
			}
		}

		CuePool pool = project.getCuePool();
		Set<AbstractPart> existingParts = new HashSet<>();
		for(Cue<?> cue : pool.getCues()) {
			existingParts.add(cue.getPart());
		}

		// TODO: update this later, once more part types become available
		uniqueParts.stream().filter(part -> !existingParts.contains(part)).forEach(part -> {
			if(part instanceof LaserPart) {
				LaserPart laserPart = (LaserPart) part;
				List<PartContainer<?>> containers = map.get(laserPart);
				int color = containers.get(0).getTrack().getColor();

				LaserCue cue = new LaserCue(project, laserPart);
				cue.setColor(color);
				long maxlen = 0;
				for(PartContainer<?> container : containers) {
					long len = container.getLength();
					if(len > maxlen) {
						maxlen = len;
					}

					LaserReference laser = ((LaserTrack) container.getTrack()).getLaserReference();
					if(laser != null) {
						cue.addLaser(laser);
					}
				}
				cue.setLength((int) maxlen);

				pool.addCue(cue);
			}
		});

		cueList.repaint();
	}

	public void deleteAll() {
		project.getCuePool().clear();
		cueList.repaint();
	}
}
