package com.unknown.emulight.lcp.ui.laser;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import com.unknown.emulight.lcp.event.ConfigChangeListener;
import com.unknown.emulight.lcp.laser.LaserCue;
import com.unknown.emulight.lcp.laser.LaserCue.LaserRef;
import com.unknown.emulight.lcp.laser.LaserReference;
import com.unknown.emulight.lcp.project.EmulightSystem;
import com.unknown.emulight.lcp.project.SystemConfiguration.LaserConfig;
import com.unknown.emulight.lcp.project.SystemConfiguration.MidiPortConfig;
import com.unknown.emulight.lcp.ui.UIUtils;
import com.unknown.emulight.lcp.ui.live.CueEditor;
import com.unknown.util.ui.ExtendedTableModel;
import com.unknown.util.ui.LabeledPairLayout;
import com.unknown.util.ui.MixedTable;

@SuppressWarnings("serial")
public class LaserCueEditor extends CueEditor implements ConfigChangeListener {
	private final LaserCue cue;
	private LaserConfig[] ports;
	private LaserModel model;

	public LaserCueEditor(EmulightSystem sys, LaserCue cue) {
		super(sys, cue);

		this.cue = cue;

		sys.getConfig().addConfigChangeListener(this);

		JTextField name = new JTextField(cue.getName());
		name.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				String text = name.getText().trim();
				if(text.length() == 0) {
					text = null;
				}
				cue.setName(text);
			}
		});

		JSpinner length = new JSpinner(new SpinnerNumberModel(cue.getLength(), 0, Integer.MAX_VALUE,
				cue.getProject().getPPQ()));
		length.addChangeListener(e -> {
			int len = (int) length.getValue();
			cue.setLength(len);
		});
		length.setToolTipText("Length of the cue in ticks. Zero means infinitely long.");

		JCheckBox toggleTrigger = new JCheckBox();
		toggleTrigger.setSelected(cue.isToggleTrigger());
		toggleTrigger.addChangeListener(e -> cue.setToggleTrigger(toggleTrigger.isSelected()));
		toggleTrigger.setToolTipText("When enabled, the MIDI trigger key toggles playback. When disabled, " +
				"the MIDI trigger key always starts playback.");

		ports = new LaserConfig[0];

		JPanel controls = new JPanel(new LabeledPairLayout());
		controls.setBorder(UIUtils.border("Cue Properties"));
		controls.add(LabeledPairLayout.LABEL, new JLabel("Name:"));
		controls.add(LabeledPairLayout.COMPONENT, name);
		controls.add(LabeledPairLayout.LABEL, new JLabel("Color:"));
		controls.add(LabeledPairLayout.COMPONENT, createColorBox());
		controls.add(LabeledPairLayout.LABEL, new JLabel("Length:"));
		controls.add(LabeledPairLayout.COMPONENT, length);
		controls.add(LabeledPairLayout.LABEL, new JLabel("Toggle Trigger:"));
		controls.add(LabeledPairLayout.COMPONENT, toggleTrigger);

		JTable laserTable = new MixedTable(model = new LaserModel());

		JPanel buttons = new JPanel(new FlowLayout());
		JButton close = new JButton("Close");
		close.addActionListener(e -> close());
		buttons.add(close);

		setLayout(new BorderLayout());
		add(BorderLayout.NORTH, controls);
		add(BorderLayout.CENTER, new JScrollPane(laserTable));
		add(BorderLayout.SOUTH, buttons);

		refreshLasers();

		pack();
	}

	private void refreshLasers() {
		Collection<LaserConfig> configuredLasers = cue.getProject().getSystem().getConfig().getLasers();
		List<LaserConfig> activeLasers = new ArrayList<>();
		for(LaserConfig p : configuredLasers) {
			if(p.isActive()) {
				activeLasers.add(p);
			}
		}
		LaserConfig[] lasers = activeLasers.toArray(new LaserConfig[activeLasers.size()]);
		Arrays.sort(lasers, (a, b) -> a.getName().compareTo(b.getName()));
		ports = lasers;
		model.update();
	}

	@Override
	protected void destroy() {
		// remove listeners
		sys.getConfig().removeConfigChangeListener(this);
	}

	@Override
	public void configChanged(String key, String value) {
		// nothing
	}

	@Override
	public void laserChanged(LaserConfig laser) {
		refreshLasers();
	}

	@Override
	public void midiPortChanged(MidiPortConfig p) {
		// nothing
	}

	private class LaserModel extends ExtendedTableModel {
		@Override
		public int getColumnAlignment(int col) {
			return SwingConstants.LEFT;
		}

		@Override
		public String getColumnName(int col) {
			switch(col) {
			case 0:
				return "Name";
			case 1:
				return "Assigned";
			case 2:
				return "Mirror X";
			case 3:
				return "Mirror Y";
			default:
				return null;
			}
		}

		@Override
		public boolean isCellEditable(int row, int col) {
			return col > 0;
		}

		@Override
		public int getRowCount() {
			return ports.length;
		}

		@Override
		public int getColumnCount() {
			return 4;
		}

		@Override
		public Object getValueAt(int row, int col) {
			LaserConfig port = ports[row];
			if(col == 0) {
				return port.getName();
			}

			if(col == 1) {
				for(LaserReference ref : cue.getLasers()) {
					if(ref.getName().equals(port.getName())) {
						return true;
					}
				}
				return false;
			} else if(col == 2) {
				LaserRef ref = get(port);
				if(ref != null) {
					return ref.isMirrorX();
				} else {
					return false;
				}
			} else if(col == 3) {
				LaserRef ref = get(port);
				if(ref != null) {
					return ref.isMirrorY();
				} else {
					return false;
				}
			}

			return false;
		}

		private LaserRef get(LaserConfig cfg) {
			Optional<LaserRef> optional = cue.getLasers().stream()
					.filter(ref -> ref.getName().equals(cfg.getName()))
					.findFirst();
			if(optional.isPresent()) {
				return optional.get();
			} else {
				return null;
			}
		}

		@Override
		public void setValueAt(Object value, int row, int col) {
			LaserConfig port = ports[row];
			boolean val = (boolean) value;
			if(col == 1) {
				if(!val) {
					LaserRef ref = get(port);
					if(ref != null) {
						cue.removeLaser(ref);
					}
				} else {
					cue.addLaser(new LaserReference(cue.getProject().getSystem(), port));
				}
			} else if(col == 2) {
				LaserRef ref = get(port);
				if(ref != null) {
					ref.setMirrorX(val);
				}
			} else if(col == 3) {
				LaserRef ref = get(port);
				if(ref != null) {
					ref.setMirrorY(val);
				}
			}
		}

		public void update() {
			fireTableDataChanged();
		}
	}
}
