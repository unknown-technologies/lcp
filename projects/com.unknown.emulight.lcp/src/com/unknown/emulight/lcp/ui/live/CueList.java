package com.unknown.emulight.lcp.ui.live;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.unknown.audio.analysis.MIDINames;
import com.unknown.emulight.lcp.laser.LaserCue;
import com.unknown.emulight.lcp.laser.LaserCue.LaserRef;
import com.unknown.emulight.lcp.live.Cue;
import com.unknown.emulight.lcp.live.CuePool;
import com.unknown.emulight.lcp.live.TriggerKey;
import com.unknown.emulight.lcp.ui.UIUtils;
import com.unknown.emulight.lcp.ui.resources.icons.project.tracktype.TrackIcons;
import com.unknown.util.ui.ADM3AFont;

@SuppressWarnings("serial")
public class CueList extends JComponent {
	private static final int TILE_SIZE = 100;
	private static final int TILE_PADDING = 5;
	private static final Color TILE_BORDER = Color.BLACK;
	private static final Color TRANSPARENT = new Color(0, true);

	private final CuePool pool;

	public CueList(CuePool pool) {
		this.pool = pool;

		setBorder(UIUtils.border("Cues"));

		MouseController controller = new MouseController();
		addMouseListener(controller);

		setToolTipText("");
	}

	public void setBPM(double bpm) {
		pool.setBPM(bpm);
	}

	public double getBPM() {
		return pool.getBPM();
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		int width = getWidth();
		int height = getHeight();

		Insets insets = getInsets();

		int minX = insets.left;
		int maxX = width - insets.right;
		int minY = insets.top;
		int maxY = height - insets.bottom;

		int px = minX;
		int py = minY;

		for(Cue<?> cue : pool.getCues()) {
			g.setColor(TILE_BORDER);
			g.drawRect(px, py, TILE_SIZE, TILE_SIZE);

			Color cueColor = cue.getProject().getColor(cue.getColor());
			Color textColor = UIUtils.getTextColor(cueColor);

			g.setColor(cueColor);
			g.fillRect(px + 1, py + 1, TILE_SIZE - 1, TILE_SIZE - 1);

			try {
				String typeName;
				switch(cue.getType()) {
				case Cue.LASER:
					typeName = TrackIcons.LASER;
					break;
				case Cue.DMX:
					typeName = TrackIcons.DMX;
					break;
				case Cue.MIDI:
					typeName = TrackIcons.MIDI;
					break;
				default:
					typeName = null;
					break;
				}
				if(typeName != null) {
					g.drawImage(TrackIcons.get(typeName), px + 1, py + 1, this);
				}
			} catch(IOException e) {
				// swallow
			}

			TriggerKey key = pool.getTriggerKey(cue);
			if(key == null) {
				String msg = "<no key>";
				int x = px + TILE_SIZE - TILE_PADDING - msg.length() * ADM3AFont.WIDTH;
				int y = py + TILE_PADDING + ADM3AFont.HEIGHT;
				ADM3AFont.render(g, x, y, textColor, TRANSPARENT, msg);
			} else {
				String msg = MIDINames.getNoteName(key.getKey());
				int x = px + TILE_SIZE - TILE_PADDING - msg.length() * ADM3AFont.WIDTH;
				int y = py + TILE_PADDING + ADM3AFont.HEIGHT;
				ADM3AFont.render(g, x, y, textColor, TRANSPARENT, msg);
			}

			int x = px + TILE_PADDING;
			int y = py + TILE_PADDING;
			int sz = TILE_SIZE - 2 * TILE_PADDING;

			String name = cue.getName();
			if(name != null) {
				int w = ADM3AFont.WIDTH * name.length();
				if(w > sz) {
					// truncate
					int maxlen = sz / ADM3AFont.WIDTH;
					name = name.substring(0, maxlen);
					w = ADM3AFont.WIDTH * name.length();
				}

				int textX = x + (sz - w) / 2;
				int textY = y + (sz + ADM3AFont.HEIGHT) / 2;

				ADM3AFont.render(g, textX, textY, textColor, TRANSPARENT, name);
			}

			switch(cue.getType()) {
			case Cue.LASER: {
				LaserCue laserCue = (LaserCue) cue;
				Set<LaserRef> lasers = laserCue.getLasers();
				String info;
				if(lasers.isEmpty()) {
					info = "<No lasers>";
				} else if(lasers.size() == 1) {
					info = lasers.iterator().next().getName();
				} else {
					info = lasers.size() + " lasers";
				}
				ADM3AFont.render(g, x, y + sz, textColor, TRANSPARENT, info);
				break;
			}
			}

			px += TILE_SIZE;
			if(px + TILE_SIZE >= maxX) {
				px = minX;
				py += TILE_SIZE;
				if(py > maxY) {
					break;
				}
			}
		}
	}

	private Cue<?> getCue(int x, int y) {
		int width = getWidth();
		int height = getHeight();

		Insets insets = getInsets();

		int minX = insets.left;
		int maxX = width - insets.right;
		int minY = insets.top;
		int maxY = height - insets.bottom;

		if(x < minX || x >= maxX) {
			return null;
		}
		if(y < minY || y >= maxY) {
			return null;
		}

		int cuesPerLine = (maxX - minX) / TILE_SIZE;
		int px = (x - minX) / TILE_SIZE;
		int py = (y - minY) / TILE_SIZE;

		int id = py * cuesPerLine + px;
		if(id >= 0 && id < pool.getCues().size()) {
			return pool.getCue(id);
		} else {
			return null;
		}
	}

	@Override
	public String getToolTipText(MouseEvent e) {
		Cue<?> cue = getCue(e.getX(), e.getY());
		if(cue != null) {
			return "Type: " + cue.getTypeName();
		} else {
			return null;
		}
	}

	private class MouseController extends MouseAdapter {
		@Override
		public void mousePressed(MouseEvent e) {
			Cue<?> cue = getCue(e.getX(), e.getY());
			if(cue == null) {
				return;
			}

			if(e.getButton() == MouseEvent.BUTTON1) {
				cue.play(getBPM());
			} else if(e.getButton() == MouseEvent.BUTTON3) {
				JMenuItem playCue = new JMenuItem("Play");
				playCue.setMnemonic('P');
				playCue.addActionListener(ev -> cue.play(getBPM()));

				JMenuItem stopCue = new JMenuItem("Stop");
				stopCue.setMnemonic('S');
				stopCue.addActionListener(ev -> cue.stop());

				JMenuItem cueProperties = new JMenuItem("Properties...");
				cueProperties.setMnemonic('r');
				cueProperties.addActionListener(ev -> {
					CueEditor editor = CueEditor.show(cue.getProject().getSystem(), cue);
					editor.setLocationRelativeTo(CueList.this);
					editor.setVisible(true);
				});

				JMenuItem setColor = new JMenuItem("Set color...");
				setColor.setMnemonic('c');
				setColor.addActionListener(ev -> {
					Color color = UIUtils.showPaletteColorChooser(CueList.this,
							"Cue color...", cue.getProject().getColor(cue.getColor()),
							cue.getProject().getPalette());
					if(color != null) {
						int idx = cue.getProject().getPalette().getColorIndex(color);
						if(idx != -1) {
							cue.setColor(idx);
						}
						repaint();
					}
				});

				JMenuItem setTriggerKey = new JMenuItem("Set trigger key...");
				setTriggerKey.setMnemonic('t');
				setTriggerKey.addActionListener(ev -> {
					CueTriggerLearnDialog dlg = new CueTriggerLearnDialog(pool, cue);
					dlg.setLocationRelativeTo(CueList.this);
					dlg.setVisible(true);
					repaint();
				});

				JMenuItem removeCue = new JMenuItem("Remove");
				removeCue.setMnemonic('R');
				removeCue.addActionListener(ev -> {
					pool.removeCue(cue);
					repaint();
				});

				JPopupMenu menu = new JPopupMenu();
				menu.add(playCue);
				menu.add(stopCue);
				menu.addSeparator();
				menu.add(setColor);
				menu.add(cueProperties);
				menu.add(setTriggerKey);
				menu.addSeparator();
				menu.add(removeCue);

				menu.show(e.getComponent(), e.getX(), e.getY());
			}
		}
	}
}
