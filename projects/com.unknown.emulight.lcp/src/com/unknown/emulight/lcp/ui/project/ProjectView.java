package com.unknown.emulight.lcp.ui.project;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusEvent.Cause;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.Timer;
import javax.swing.UIManager;

import com.unknown.emulight.lcp.audio.AudioData;
import com.unknown.emulight.lcp.audio.AudioPart;
import com.unknown.emulight.lcp.audio.AudioPeakMap;
import com.unknown.emulight.lcp.audio.AudioTrack;
import com.unknown.emulight.lcp.event.ProjectListener;
import com.unknown.emulight.lcp.event.SequencerListener;
import com.unknown.emulight.lcp.event.TrackListener;
import com.unknown.emulight.lcp.laser.LaserPart;
import com.unknown.emulight.lcp.laser.LaserTrack;
import com.unknown.emulight.lcp.project.AbstractPart;
import com.unknown.emulight.lcp.project.PartContainer;
import com.unknown.emulight.lcp.project.Project;
import com.unknown.emulight.lcp.project.TempoTrack;
import com.unknown.emulight.lcp.project.TempoTrack.TempoCheckpoint;
import com.unknown.emulight.lcp.project.Track;
import com.unknown.emulight.lcp.sequencer.MidiPart;
import com.unknown.emulight.lcp.sequencer.MidiTrack;
import com.unknown.emulight.lcp.sequencer.Note;
import com.unknown.emulight.lcp.sequencer.TempoChange;
import com.unknown.emulight.lcp.sequencer.TempoPart;
import com.unknown.emulight.lcp.ui.UIUtils;
import com.unknown.emulight.lcp.ui.audio.AudioPartEditorDialog;
import com.unknown.emulight.lcp.ui.event.GridChangeListener;
import com.unknown.emulight.lcp.ui.event.PartSelectionListener;
import com.unknown.emulight.lcp.ui.laser.LaserPartEditorDialog;
import com.unknown.emulight.lcp.ui.midi.MidiPartEditorDialog;
import com.unknown.emulight.lcp.ui.midi.TempoPartEditorDialog;
import com.unknown.emulight.lcp.ui.resources.icons.project.tracktype.TrackIcons;
import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;
import com.unknown.util.ui.ADM3AFont;

@SuppressWarnings("serial")
public class ProjectView extends JComponent {
	private static final Logger log = Trace.create(ProjectView.class);

	private static final Color PALETTE[] = {
			new Color(142, 160, 178)
	};

	private static final int BORDER = 5;

	private static final int RULER_Y = BORDER;
	private static final int RULER_HEIGHT = 25;

	private static final int START_X = BORDER;
	private static final int START_Y = RULER_Y + RULER_HEIGHT;
	private static final int LINE_HEIGHT = 42;

	private static final int HEADER_WIDTH = 300;
	private static final int HEADER_SPACE = 7;
	private static final int CONTENT_X = START_X + HEADER_WIDTH + 15;

	private static final Color TRACK_SEPARATOR = new Color(113, 115, 117);
	private static final Color TRACK_BACKGROUND = new Color(37, 40, 43);
	private static final Color TRACK_SELECTED = new Color(233, 233, 233);

	private static final Color HEADER_SEPARATOR = new Color(99, 104, 109);

	private static final Color GRID_BAR = new Color(0x77797A);
	private static final Color GRID_BEAT = new Color(0x595B5D);
	private static final Color GRID_SUBFRAME = new Color(0x36393B);

	private static final Color BEAT_GRID_TEXT = new Color(0xFFFFFF);

	private static final Color TRANSPARENT = new Color(0, 0, 0, 0);

	private final Project project;

	private int selected = -1;

	private PartContainer<?> selectedPart = null;
	private PartContainer<?> tempPart = null;
	private PartContainer<?> hiddenPart = null;

	private final Map<Track<?>, TrackEditor> trackEditors = new HashMap<>();

	private double timeScale = 0.25;

	private int offsetX;
	private int offsetY;

	private int signatureNumerator = 4;
	private int signatureDenominator = 4;
	private int ppq = 96;

	private double grid = ppq / 4.0;
	private int division = 1;

	private final TrackListener trackListener = e -> repaint();
	private final ProjectListener projectListener = new ProjectListener() {
		@Override
		public void propertyChanged(String key) {
			repaint();
		}

		@Override
		public void trackAdded(Track<?> track) {
			track.addTrackListener(trackListener);
			repaint();
		}

		@Override
		public void trackRemoved(Track<?> track) {
			track.removeTrackListener(trackListener);

			// remove track editor if there is one
			TrackEditor editor = trackEditors.remove(track);
			if(editor != null) {
				editor.dispose();
				editor.destroy();
			}
			repaint();
		}
	};

	private final List<PartSelectionListener> selectionListeners = new ArrayList<>();
	private final List<GridChangeListener> gridListeners = new ArrayList<>();

	public ProjectView(Project project) {
		this.project = project;

		setDoubleBuffered(true);

		ppq = project.getPPQ();

		setTimeScale(timeScale * (384.0 / ppq));
		setDivision(1);

		project.addProjectListener(projectListener);
		for(Track<?> track : project.getTracks()) {
			track.addTrackListener(trackListener);
		}

		Timer timer = new Timer(50, e -> repaint());
		timer.setRepeats(true);

		project.getSequencer().addListener(new SequencerListener() {
			@Override
			public void playbackStarted() {
				timer.start();
			}

			@Override
			public void playbackStopped() {
				timer.stop();
				repaint();
			}

			@Override
			public void positionChanged(long tick) {
				repaint();
			}
		});

		MouseController mouse = new MouseController();
		addMouseListener(mouse);
		addMouseMotionListener(mouse);
		addMouseWheelListener(mouse);

		setFocusable(true);
		setRequestFocusEnabled(true);
		setFocusTraversalKeysEnabled(false);

		addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
				// nothing
			}

			@Override
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_DELETE) {
					deleteSelectedPart();
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
				// nothing
			}
		});
	}

	public void addPartSelectionListener(PartSelectionListener listener) {
		selectionListeners.add(listener);
	}

	public void removePartSelectionListener(PartSelectionListener listener) {
		selectionListeners.remove(listener);
	}

	public void addGridChangeListener(GridChangeListener listener) {
		gridListeners.add(listener);
	}

	public void removeGridChangeListener(GridChangeListener listener) {
		gridListeners.remove(listener);
	}

	public void setTimeScale(double scale) {
		timeScale = scale;
		repaint();
	}

	public void setDivision(int division) {
		this.division = division;
		grid = ppq / 4.0 / division;
		for(GridChangeListener listener : gridListeners) {
			try {
				listener.gridChanged(grid);
			} catch(Throwable t) {
				log.log(Levels.ERROR, "Failed to run grid change listener: " + t.getMessage(), t);
			}
		}
		repaint();
	}

	public double getGrid() {
		return grid;
	}

	public void setOffsetX(int off) {
		offsetX = off;
		if(offsetX < 0) {
			offsetX = 0;
		}

		repaint();
	}

	public void setOffsetY(int off) {
		offsetY = off;

		int tracks = project.getTracks().size();
		int visible = (getHeight() - START_Y - BORDER) / LINE_HEIGHT;

		if(offsetY < 0) {
			offsetY = 0;
		} else if(offsetY >= (tracks - visible)) {
			offsetY = tracks - visible;
			if(offsetY < 0) {
				offsetY = 0;
			}
		}

		repaint();
	}

	public void deleteSelectedPart() {
		if(selectedPart != null) {
			selectedPart.delete();
			setSelectedPart(null);
		}
	}

	public long getTime() {
		return project.getSequencer().getTick();
	}

	public PartContainer<?> getSelectedPart() {
		return selectedPart;
	}

	public void setSelectedPart(PartContainer<?> part) {
		selectedPart = part;
		for(PartSelectionListener listener : selectionListeners) {
			try {
				listener.selectionChanged(part);
			} catch(Throwable t) {
				log.log(Levels.ERROR, "Failed to execute selection listener: " + t.getMessage(), t);
			}
		}
		repaint();
	}

	private boolean updateSelection(int px, int py, boolean processButtons) {
		List<Track<?>> tracks = project.getTracks();
		int x = START_X;
		int y = START_Y;
		int i = 0;
		int newSelection = selected;
		boolean clickedOnTrack = false;
		for(Track<?> track : tracks) {
			if(offsetY > i) {
				i++;
				continue;
			}

			if(py >= y && py <= (y + 41)) {
				newSelection = i;
				clickedOnTrack = true;
			} else {
				y += LINE_HEIGHT;
				i++;
				continue;
			}

			// process buttons
			if(!processButtons) {
				y += LINE_HEIGHT;
				i++;
				continue;
			}

			int posX = x + 47;
			int posY = y + 3;
			for(TrackControl ctl : getTrackControls(track)) {
				if(ctl.isIntegrated()) {
					if(posX < x + 95) {
						posX = x + 95;
					}
					posY = y + 22;
					if(px >= posX && px <= (posX + ctl.getWidth()) && py >= posY &&
							py <= (posY + ctl.getHeight())) {
						if(ctl.click(px - posX, py - posY, px, py)) {
							newSelection = selected;
							clickedOnTrack = false;
							repaint();
						}
						return clickedOnTrack;
					}
					posX += ctl.getWidth() + 2;
				} else {
					if(px >= posX && px <= (posX + ctl.getWidth()) && py >= posY &&
							py <= (posY + ctl.getHeight())) {
						if(ctl.click(px - posX, py - posY, px, py)) {
							newSelection = selected;
							clickedOnTrack = false;
							repaint();
						}
						return clickedOnTrack;
					}
					posY += ctl.getHeight() + 1;
				}
			}

			y += LINE_HEIGHT;
			i++;
		}

		if(newSelection != selected) {
			if(selected >= 0 && selected < tracks.size()) {
				tracks.get(selected).setRecordingArmed(false);
			}
			selected = newSelection;
			if(selected >= 0 && selected < tracks.size()) {
				tracks.get(selected).setRecordingArmed(true);
			}
			repaint();
		}

		return clickedOnTrack;
	}

	public void cleanup() {
		for(Track<?> track : project.getTracks()) {
			track.removeTrackListener(trackListener);
		}

		project.removeProjectListener(projectListener);
	}

	private <T extends AbstractPart> Iterable<PartContainer<T>> getParts(Track<T> track) {
		if(tempPart != null && tempPart.getTrack() == track) {
			return () -> {
				return new Iterator<>() {
					boolean last = true;
					final Iterator<PartContainer<T>> it = track.getParts().iterator();

					public boolean hasNext() {
						boolean next = it.hasNext();
						if(next) {
							return true;
						} else {
							return last;
						}
					}

					@SuppressWarnings("unchecked")
					public PartContainer<T> next() {
						if(it.hasNext()) {
							return it.next();
						} else if(last) {
							last = false;
							return (PartContainer<T>) tempPart;
						} else {
							throw new NoSuchElementException();
						}
					}
				};
			};
		} else {
			return track.getParts();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		int width = getWidth();
		int height = getHeight();

		int contentWidth = width - CONTENT_X - BORDER;

		// draw background
		g.setColor(TRACK_BACKGROUND);
		g.fillRect(0, 0, width, height);

		List<Track<?>> tracks = project.getTracks();

		// draw vertical grid
		double autogrid = grid;
		int autodivision = division;

		boolean drawGrid = true;
		boolean drawBeat = true;
		boolean drawBeatText = true;

		for(int i = 0; i < 6; i++) {
			int cellCount = (int) Math.round(contentWidth / (autogrid * timeScale)) + 2;

			drawGrid = true;
			drawBeat = true;
			drawBeatText = true;

			int gridCellSize = contentWidth / cellCount;
			int beatCellSize = contentWidth * signatureDenominator / cellCount;

			if(gridCellSize < 5) {
				drawGrid = false;
			}

			if(beatCellSize < 15) {
				drawBeat = false;
			} else if(beatCellSize < 30) {
				drawBeatText = false;
			}

			if((!drawBeat || !drawBeatText || !drawGrid) && autodivision >= 2) {
				autogrid *= 2;
				autodivision /= 2;
			} else {
				break;
			}
		}

		int cellCount = (int) Math.round(contentWidth / (autogrid * timeScale)) + 2;

		for(int i = 0; i < cellCount; i++) {
			int posX = (int) Math.round(i * autogrid * timeScale - offsetX % (autogrid * timeScale));
			int off = (int) (offsetX / (autogrid * timeScale));
			int n = i + off;

			if(posX < 0) {
				continue;
			}

			posX += CONTENT_X;

			if(n % (signatureNumerator * signatureDenominator * autodivision) == 0) {
				g.setColor(GRID_BAR);
			} else if(n % (signatureDenominator * autodivision) == 0) {
				if(!drawBeat) {
					continue;
				}
				g.setColor(GRID_BEAT);
			} else {
				if(!drawGrid) {
					continue;
				}
				g.setColor(GRID_SUBFRAME);
			}

			g.drawLine(posX, RULER_Y, posX, height - BORDER);
		}

		// draw beat/bar IDs
		for(int i = 0; i < cellCount; i++) {
			int posX = (int) Math.round(i * autogrid * timeScale - offsetX % (autogrid * timeScale));
			int off = (int) (offsetX / (autogrid * timeScale));
			int n = i + off;

			if(posX < 0) {
				continue;
			}

			posX += CONTENT_X;

			int bar = n / (signatureNumerator * signatureDenominator * autodivision) + 1;
			int beat = (n / (signatureDenominator * autodivision)) % signatureNumerator + 1;

			String text;
			if(n % (signatureNumerator * signatureDenominator * autodivision) == 0) {
				text = Integer.toString(bar);
			} else if(n % (signatureDenominator * autodivision) == 0) {
				text = bar + "." + beat;
				if(!drawBeat || !drawBeatText) {
					continue;
				}
			} else {
				continue;
			}

			ADM3AFont.render(g, posX + 3, RULER_Y + RULER_HEIGHT - 4, BEAT_GRID_TEXT, TRANSPARENT, text);
		}

		// draw top grid line
		g.setColor(TRACK_SEPARATOR);
		g.drawLine(START_X - 1, START_Y - 1, width - BORDER, START_Y - 1);

		int x = START_X;
		int y = START_Y;
		int i = 0;

		// draw track header
		for(Track<?> track : tracks) {
			if(offsetY > i) {
				i++;
				continue;
			}

			// draw track background if selected
			if(selected == i) {
				int limit = HEADER_WIDTH - 40;
				g.setColor(TRACK_SELECTED);
				g.fillRect(x + 40, y, limit, 41);
			}

			// draw filled box
			g.setColor(getColor(track.getColor()));
			g.fillRect(x, y, 40, 41);

			try {
				g.drawImage(TrackIcons.get(track.getType()), x, y, this);
			} catch(IOException e) {
				// swallow
			}

			// draw buttons
			int px = x + 47;
			int py = y + 3;
			for(TrackControl ctl : getTrackControls(track)) {
				if(ctl.isIntegrated()) {
					if(px < x + 95) {
						px = x + 95;
					}
					py = y + 22;
					ctl.paint(g, px, py, this);
					px += ctl.getWidth() + 2;
				} else {
					ctl.paint(g, px, py, this);
					py += ctl.getHeight() + 1;
				}
			}

			Font font = UIManager.getFont("InternalFrame.titleFont");
			FontMetrics metrics = getFontMetrics(font);
			g.setFont(font);
			g.setColor(selected == i ? Color.BLACK : Color.WHITE);
			py = y + 2 + (17 / 2) + (metrics.getAscent() / 2);
			g.drawString(track.getName(), x + 98, py);

			// draw grid line
			g.setColor(TRACK_SEPARATOR);
			g.drawLine(x, y + 41, width - 5, y + 41);

			// draw track parts
			Color trackColor = project.getColor(track.getColor());
			Color trackOutline = new Color(trackColor.getRed() / 2, trackColor.getGreen() / 2,
					trackColor.getBlue() / 2);
			Color trackText = UIUtils.getTextColor(trackColor);

			for(PartContainer<?> part : getParts(track)) {
				if(part == hiddenPart) {
					continue;
				}

				int posX = (int) Math.round(part.getStart() * timeScale) - offsetX;
				int length = (int) Math.round(part.getLength() * timeScale);
				if(posX < 0) {
					length += posX;
					posX = 0;
				}

				if(length <= 0) {
					continue;
				}

				if(posX + length >= contentWidth) {
					length = contentWidth - posX;
				}

				if(length > 0) {
					Color color = trackColor;
					Color outline = trackOutline;
					Color text = trackText;
					if(part == selectedPart || part == tempPart) {
						color = trackOutline;
						outline = trackColor;
						text = UIUtils.getTextColor(color);
					}

					int startX = CONTENT_X + posX;
					g.setColor(color);
					g.fillRect(startX + 1, y + 2, length - 1, LINE_HEIGHT - 5);

					g.setColor(outline);
					g.drawRect(startX, y + 1, length, LINE_HEIGHT - 4);

					switch(track.getType()) {
					case Track.TEMPO:
						drawTempoTrack(g, (TempoTrack) track, (PartContainer<TempoPart>) part,
								outline, startX + 1, y + 2, length - 1,
								LINE_HEIGHT - 5);
						break;
					case Track.MIDI:
						drawMidiTrack(g, (PartContainer<MidiPart>) part, outline, startX + 1,
								y + 2, length - 1, LINE_HEIGHT - 5);
						break;
					case Track.AUDIO:
						drawAudioTrack(g, (PartContainer<AudioPart>) part, outline, startX + 1,
								y + 2, length - 1, LINE_HEIGHT - 5);
						break;
					}

					String name = part.getPart().getName();
					if(name != null && length > name.length() * ADM3AFont.WIDTH + 10) {
						g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(),
								128));
						g.fillRect(startX + 3, y + 3, name.length() * ADM3AFont.WIDTH + 4,
								ADM3AFont.HEIGHT + 4);
						ADM3AFont.render(g, startX + 5, y + 5 + ADM3AFont.HEIGHT, text,
								TRANSPARENT, name);
					}

					// render shared part indicator
					if(part.getPart().getRefCount() > 1) {
						int ex = startX + length - 1 - 2;
						int sxShort = ex - 4;
						int sxLong = ex - 8;
						int sx = -1;
						int len = 0;
						if(sxLong > startX + 1) {
							sx = sxLong;
							len = 8;
						} else if(sxShort > startX + 1) {
							sx = sxShort;
							len = 4;
						}
						if(len != 0) {
							g.setColor(outline);
							g.fillRect(sx, y + 4, len, 2);
							g.fillRect(sx, y + 8, len, 2);
						}
					}
				}
			}

			y += LINE_HEIGHT;
			i++;
		}

		// draw separator between header and content
		g.setColor(HEADER_SEPARATOR);
		g.drawLine(START_X + HEADER_WIDTH, RULER_Y, START_X + HEADER_WIDTH, height - BORDER);

		g.setColor(TRACK_BACKGROUND);
		g.fillRect(START_X + HEADER_WIDTH + 1, 0, HEADER_SPACE, height);

		// clear bottom
		g.setColor(TRACK_BACKGROUND);
		g.fillRect(0, height - BORDER + 1, width, BORDER);

		int cursor = getPixel(project.getSequencer().getTick());
		if(cursor >= CONTENT_X && cursor <= width - BORDER) {
			g.setColor(Color.BLACK);
			g.drawLine(cursor, BORDER, cursor, height - BORDER);
			g.setColor(Color.WHITE);
			g.drawLine(cursor - 1, BORDER, cursor - 1, height - BORDER);
			g.drawLine(cursor + 1, BORDER, cursor + 1, height - BORDER);
		}
	}

	private int getPixel(long time) {
		return (int) Math.round((time * timeScale) - offsetX + CONTENT_X);
	}

	private long getTime(int x) {
		return (long) ((x - CONTENT_X + offsetX) / timeScale);
	}

	private long quantizeTime(long time) {
		return Math.round(Math.round(time / (grid * 1.0)) * grid);
	}

	private void drawTempoTrack(Graphics g, TempoTrack track, PartContainer<TempoPart> part, Color color, int x,
			int y, int length, int height) {
		int minX = x;
		int maxX = x + length;

		// compute bounds
		double minTempo = track.getTempo(0);
		double maxTempo = minTempo;

		for(TempoChange change : part.getPart().getTempoChanges()) {
			if(!part.containsEvent(change.getTime())) {
				continue;
			}

			double tempo = change.getTempo();
			if(tempo < minTempo) {
				minTempo = tempo;
			}
			if(tempo > maxTempo) {
				maxTempo = tempo;
			}
		}

		double offset = minTempo;
		double scale = (height - 4) / (maxTempo - minTempo);

		if(scale == 0) {
			scale = 1;
			offset = offset - (height - 4) / 2.0;
		}

		// draw tempo changes
		int lastX = -1;
		int lastY = -1;
		int finalX = -1;
		int finalY = -1;
		int beforeY = -1;

		g.setColor(color);

		long t = part.getTime();
		for(TempoChange change : part.getPart().getTempoChanges()) {
			if(!part.containsEvent(change.getTime())) {
				continue;
			}

			int pos = (int) Math.round((change.getTempo() - offset) * scale);
			int py = y + height - 2 - pos;

			int pixel = getPixel(t + change.getTime());
			if(pixel >= minX && pixel <= maxX) {
				g.drawOval(pixel - 1, py - 1, 2, 2);

				if(lastX != -1) {
					int startX = lastX;
					if(startX < minX) {
						startX = minX;
					}
					g.drawLine(startX, lastY, pixel, lastY);
					g.drawLine(pixel, lastY, pixel, py);

					finalX = pixel;
					finalY = py;
				}
			} else if(pixel < minX) {
				beforeY = py;
			}

			lastX = pixel;
			lastY = py;
		}

		if(finalX != -1 && finalY != -1) {
			// last event is visible
			g.drawLine(finalX, finalY, maxX - 1, finalY);
		} else if(beforeY != -1) {
			// last event is not visible
			g.drawLine(minX, beforeY, maxX - 1, beforeY);
		}
	}

	private void drawMidiTrack(Graphics g, PartContainer<MidiPart> part, Color color, int x, int y, int length,
			int height) {
		int minX = x;
		int maxX = x + length;

		// compute bounds
		int minKey = 128;
		int maxKey = 0;

		for(Note note : part.getPart().getNotes()) {
			if(!part.containsEvent(note.getTime())) {
				continue;
			}

			int key = note.getKey();
			if(key < minKey) {
				minKey = key;
			}
			if(key > maxKey) {
				maxKey = key;
			}
		}

		// draw notes
		g.setColor(color);
		double offset = minKey;
		double scale = (double) (height - 4) / (maxKey - minKey);

		if(maxKey == minKey) {
			scale = 1;
			offset = offset - (height - 4) / 2.0;
		}

		long t = part.getTime();
		for(Note note : part.getPart().getNotes()) {
			if(!part.containsEvent(note.getTime())) {
				continue;
			}

			int key = note.getKey();
			int pos = (int) Math.round((key - offset) * scale);
			int py = y + height - 2 - pos;

			int pxStart = getPixel(t + note.getTime());
			int pxEnd = getPixel(t + note.getEnd());
			if(pxStart <= maxX && pxEnd >= minX) {
				// draw note
				if(pxStart < minX) {
					pxStart = minX;
				}
				if(pxEnd > maxX) {
					pxEnd = maxX;
				}
				g.drawLine(pxStart, py, pxEnd, py);
			}
		}
	}

	private void drawAudioTrack(Graphics g, PartContainer<AudioPart> part, Color color, int x, int y, int length,
			int height) {
		// TODO: do something with the Trim Start
		long t = getTime(x);
		long tend = getTime(x + length - 1);

		AudioPart audio = part.getPart();
		AudioData data = audio.getData();

		if(data == null) {
			return;
		}

		AudioPeakMap peakMap = data.getPeakMap();

		TempoTrack tempo = project.getTempoTrack();
		NavigableMap<Long, TempoCheckpoint> checkpoints = tempo.getTempoCheckpoints();

		Entry<Long, TempoCheckpoint> entry = checkpoints.floorEntry(t);
		if(entry == null) {
			// something went wrong
			return;
		}

		long nsecPart = tempo.getTime(part.getTime());
		long nsecStart = tempo.getTime(t);
		long nsecEnd = tempo.getTime(tend);

		g.setColor(color);
		int cy = y + height / 2;
		float scale = height / 2.0f;

		float[] min = new float[length];
		float[] max = new float[length];

		long nsecT = nsecStart;
		for(int i = 0; i < length; i++) {
			TempoCheckpoint checkpoint = entry.getValue();
			Entry<Long, TempoCheckpoint> nextEntry = checkpoints.higherEntry(entry.getKey());
			if(nextEntry == null) {
				// this is the last checkpoint, finish everything here
				long sampleStart = (nsecT - nsecPart) * data.getSampleRate() / 1_000_000_000;
				long sampleEnd = (nsecEnd - nsecPart) * data.getSampleRate() / 1_000_000_000;
				int block = length - i;
				if(block == 0) {
					block = 1;
				} else if(block < 0) {
					throw new AssertionError("block=" + block);
				}
				if(sampleStart < 0) {
					throw new AssertionError("sampleStart=" + sampleStart);
				}
				if(sampleEnd < 0) {
					throw new AssertionError("sampleEnd=" + sampleEnd);
				}
				if(sampleEnd > data.getSampleCount()) {
					sampleEnd = data.getSampleCount();

					// adjust block
					long endTime = (sampleEnd * 1_000_000_000) / data.getSampleRate() + nsecPart;
					long endTick = checkpoint.getTick(endTime);
					int endPixel = getPixel(endTick);
					block = endPixel - x;
					if(block > length - i) {
						block = length - i;
					} else if(block <= 0) {
						block = 1;
					}
				}
				peakMap.getWaveform(min, max, (int) sampleStart, (int) sampleEnd, i, block);
				break;
			} else {
				TempoCheckpoint nextCheckpoint = nextEntry.getValue();
				int nextPixel = getPixel(nextCheckpoint.getTick());
				int block = nextPixel - x - i;
				if(block > length) {
					block = length;
				} else if(block <= 0) {
					nsecT = checkpoint.getTime(nextCheckpoint.getTick());
					entry = nextEntry;
					i = nextPixel - x;
					continue;
				}

				long nsecE = checkpoint.getTime(nextCheckpoint.getTick());
				long sampleStart = (nsecT - nsecPart) * data.getSampleRate() / 1000_000_000;
				long sampleEnd = (nsecE - nsecPart) * data.getSampleRate() / 1000_000_000;
				if(sampleEnd > data.getSampleCount()) {
					sampleEnd = data.getSampleCount();

					// adjust block
					long endTime = (sampleEnd * 1_000_000_000) / data.getSampleRate() + nsecPart;
					long endTick = checkpoint.getTick(endTime);
					int endPixel = getPixel(endTick);
					block = endPixel - x;
					if(block > length - i) {
						block = length - i;
					} else if(block <= 0) {
						block = 0;
					}
				}
				peakMap.getWaveform(min, max, (int) sampleStart, (int) sampleEnd, i, block);

				nsecT = nsecE;
				entry = nextEntry;
				i = nextPixel - x;
			}
		}

		for(int i = 0; i < length; i++) {
			float minY = min[i] * scale;
			float maxY = max[i] * scale;
			g.drawLine(x + i, Math.round(cy + minY), x + i, Math.round(cy + maxY));
		}
	}

	private List<TrackControl> getTrackControls(Track<?> track) {
		switch(track.getType()) {
		case Track.MIDI:
			return List.of(new TrackControlMuteSolo(this, track),
					new TrackControlRecordMonitor(this, track), new TrackControlEdit(this, track),
					new TrackControlChannel(this, (MidiTrack) track));
		case Track.SAMPLER:
			return List.of(new TrackControlMuteSolo(this, track),
					new TrackControlRecordMonitor(this, track),
					new TrackControlEditInstrument(this, track));
		case Track.AUDIO:
			return List.of(new TrackControlMuteSolo(this, track),
					new TrackControlRecordMonitor(this, track), new TrackControlEdit(this, track));
		case Track.TEMPO:
			return List.of(new TrackControlActiveLock(this, track),
					new TrackControlTempo(this, (TempoTrack) track));
		case Track.SIGNATURE:
			return List.of(new TrackControlActiveLock(this, track));
		case Track.LASER:
			return List.of(new TrackControlMuteSolo(this, track),
					new TrackControlEdit(this, track));
		case Track.DMX:
			return List.of(new TrackControlMuteSolo(this, track),
					new TrackControlEdit(this, track));
		default:
			return List.of(new TrackControlMuteSolo(this, track),
					new TrackControlRecordMonitor(this, track));
		}
	}

	public Color getColor(int index) {
		if(index >= PALETTE.length) {
			return PALETTE[0];
		} else {
			return PALETTE[index];
		}
	}

	public void showTrackEditor(Track<?> track) {
		if(isTrackEditorOpen(track)) {
			TrackEditor editor = trackEditors.get(track);
			editor.dispose();
			repaint();
			return;
		}
		TrackEditor editor = trackEditors.get(track);
		if(editor == null) {
			editor = TrackEditor.show(project.getSystem(), track);
			if(editor != null) {
				trackEditors.put(track, editor);
				editor.addWindowListener(new WindowAdapter() {
					@Override
					public void windowClosed(WindowEvent e) {
						repaint();
					}
				});
				editor.setLocationRelativeTo(this);
				editor.setVisible(true);
			}
		} else if(editor != null) {
			editor.setVisible(true);
		}
	}

	public boolean isTrackEditorOpen(Track<?> track) {
		return trackEditors.containsKey(track) && trackEditors.get(track).isVisible();
	}

	private class MouseController implements MouseListener, MouseMotionListener, MouseWheelListener {
		private int startX;
		private int startY;

		private int startOffsetX;
		private int startOffsetY;

		private long partTime;

		private boolean createPart;

		@Override
		public void mouseMoved(MouseEvent e) {
			// TODO Auto-generated method stub
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			if((e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0) {
				// left mouse button
				if(startY < START_Y) {
					long time = getTime(e.getX());
					long t = quantizeTime(time < 0 ? 0 : time);
					project.setTick(t);
					repaint();
				}

				if(createPart && selectedPart != null) {
					// resize selected part
					long startTime = selectedPart.getStart();
					long endTime = quantizeTime(getTime(e.getX()));
					long dt = endTime - startTime;

					if(dt > 0) {
						selectedPart.setLength(dt);
						repaint();
					}
				} else if(tempPart != null) {
					// move selected part
					long startTime = getTime(startX);
					long endTime = getTime(e.getX());
					long dt = quantizeTime(endTime - startTime);

					tempPart = tempPart.copyAt(partTime + dt);

					if(e.getModifiersEx() == MouseEvent.BUTTON1_DOWN_MASK) {
						hiddenPart = selectedPart;
					} else {
						hiddenPart = null;
					}

					repaint();
				}
			} else if((e.getModifiersEx() & MouseEvent.BUTTON2_DOWN_MASK) != 0) {
				// middle mouse button
				int dx = e.getX() - startX;
				int dy = e.getY() - startY;

				setOffsetX(startOffsetX - dx);
				setOffsetY(startOffsetY - dy / LINE_HEIGHT);

				repaint();
			}
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			if(e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
				// double-clicked onto something
				if(selectedPart == null) {
					return;
				}

				// double-clicking onto the track header should not open the part editor
				if(e.getX() < CONTENT_X) {
					return;
				}

				if(selectedPart.getPart() instanceof MidiPart) {
					// open a MIDI part editor
					@SuppressWarnings("unchecked")
					PartContainer<MidiPart> container = (PartContainer<MidiPart>) selectedPart;
					MidiPartEditorDialog dlg = new MidiPartEditorDialog(container, () -> repaint());
					dlg.setLocationRelativeTo(ProjectView.this);
					dlg.setVisible(true);
				} else if(selectedPart.getPart() instanceof LaserPart) {
					// open a laser part editor
					@SuppressWarnings("unchecked")
					PartContainer<LaserPart> container = (PartContainer<LaserPart>) selectedPart;
					LaserPartEditorDialog dlg = new LaserPartEditorDialog(container);
					dlg.setLocationRelativeTo(ProjectView.this);
					dlg.setVisible(true);
				} else if(selectedPart.getPart() instanceof TempoPart) {
					// open a tempo part editor
					@SuppressWarnings("unchecked")
					PartContainer<TempoPart> container = (PartContainer<TempoPart>) selectedPart;
					TempoPartEditorDialog dlg = new TempoPartEditorDialog(container,
							() -> repaint());
					dlg.setLocationRelativeTo(ProjectView.this);
					dlg.setVisible(true);
				} else if(selectedPart.getPart() instanceof AudioPart) {
					// open a tempo part editor
					@SuppressWarnings("unchecked")
					PartContainer<AudioPart> container = (PartContainer<AudioPart>) selectedPart;
					AudioPartEditorDialog dlg = new AudioPartEditorDialog(container,
							() -> repaint());
					dlg.setLocationRelativeTo(ProjectView.this);
					dlg.setVisible(true);
				}
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {
			requestFocusInWindow(Cause.MOUSE_EVENT);

			// update state
			startX = e.getX();
			startY = e.getY();

			startOffsetX = offsetX;
			startOffsetY = offsetY;

			createPart = false;

			int px = e.getX();
			int py = e.getY();

			long time = getTime(px);

			if(e.getButton() == MouseEvent.BUTTON1) {
				if(startY < START_Y) {
					long t = quantizeTime(time < 0 ? 0 : time);
					project.setTick(t);
					repaint();
				}

				if(updateSelection(px, py, true)) {
					// clicked onto a track
					if((e.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) != 0) {
						// draw a new part
						createPart = true;
						Track<?> track = project.getTrack(selected);
						long start = quantizeTime(time);
						setSelectedPart(track.createPart(start, Math.round(grid)));
						repaint();
					} else if(px > CONTENT_X && px <= getWidth() - BORDER) {
						// clicked on some part?
						boolean wasSelected = selectedPart != null;
						PartContainer<?> selection = null;
						Track<?> track = project.getTrack(selected);
						for(PartContainer<?> part : track.getParts()) {
							if(part.contains(time)) {
								// found the part
								selection = part;
								break;
							}
						}

						setSelectedPart(selection);

						if(selectedPart != null) {
							partTime = selectedPart.getTime();
						}

						tempPart = selectedPart;

						// also repaint if selection was cleared
						if(wasSelected && selectedPart == null) {
							repaint();
						}
					}
				}
			} else if(e.getButton() == MouseEvent.BUTTON3) {
				boolean clickedOnTrack = updateSelection(px, py, false);

				// can this track be deleted?
				boolean permanentTrack = false;
				List<Track<?>> tracks = project.getTracks();
				if(selected >= 0 && selected < tracks.size()) {
					permanentTrack = tracks.get(selected).isPermanent();
				}

				// right mouse, show context menu
				JMenu addTrack = new JMenu("Add Track");
				addTrack.setMnemonic('A');
				JMenuItem addAudioTrack = new JMenuItem("Audio");
				addAudioTrack.addActionListener(
						ev -> project.addTrack(new AudioTrack(project, "Untitled")));
				addAudioTrack.setMnemonic('A');
				addTrack.add(addAudioTrack);
				JMenuItem addMidiTrack = new JMenuItem("MIDI");
				addMidiTrack.addActionListener(
						ev -> project.addTrack(new MidiTrack(project, "Untitled")));
				addMidiTrack.setMnemonic('M');
				addTrack.add(addMidiTrack);
				addTrack.addSeparator();
				JMenuItem addLaserTrack = new JMenuItem("Laser");
				addLaserTrack.addActionListener(
						ev -> project.addTrack(new LaserTrack(project, "Untitled")));
				addLaserTrack.setMnemonic('L');
				addTrack.add(addLaserTrack);

				JMenuItem duplicateTrack = new JMenuItem("Duplicate Track");
				duplicateTrack.setMnemonic('D');
				duplicateTrack.setEnabled(clickedOnTrack && !permanentTrack);
				duplicateTrack.addActionListener(
						ev -> project.duplicateTrack(project.getTrack(selected)));

				JMenuItem removeTrack = new JMenuItem("Remove Track");
				removeTrack.setMnemonic('R');
				removeTrack.setEnabled(clickedOnTrack && !permanentTrack);
				removeTrack.addActionListener(ev -> project.removeTrack(project.getTrack(selected)));

				ButtonGroup divisionGroup = new ButtonGroup();

				JRadioButtonMenuItem div1 = new JRadioButtonMenuItem("Division: 1", division == 1);
				div1.setMnemonic('1');
				div1.addActionListener(ev -> setDivision(1));
				divisionGroup.add(div1);

				JRadioButtonMenuItem div2 = new JRadioButtonMenuItem("Division: 2", division == 2);
				div2.setMnemonic('2');
				div2.addActionListener(ev -> setDivision(2));
				divisionGroup.add(div2);

				JRadioButtonMenuItem div4 = new JRadioButtonMenuItem("Division: 4", division == 4);
				div4.setMnemonic('4');
				div4.addActionListener(ev -> setDivision(4));
				divisionGroup.add(div4);

				JRadioButtonMenuItem div8 = new JRadioButtonMenuItem("Division: 8", division == 8);
				div8.setMnemonic('8');
				div8.addActionListener(ev -> setDivision(8));
				divisionGroup.add(div8);

				JRadioButtonMenuItem div16 = new JRadioButtonMenuItem("Division: 16", division == 16);
				div16.setMnemonic('6');
				div16.addActionListener(ev -> setDivision(16));
				divisionGroup.add(div16);

				JRadioButtonMenuItem div32 = new JRadioButtonMenuItem("Division: 32", division == 32);
				div32.setMnemonic('3');
				div32.addActionListener(ev -> setDivision(32));
				divisionGroup.add(div32);

				JMenu divisionMenu = new JMenu("Division");
				divisionMenu.add(div1);
				divisionMenu.add(div2);
				divisionMenu.add(div4);
				divisionMenu.add(div8);
				divisionMenu.add(div16);
				divisionMenu.add(div32);

				JPopupMenu menu = new JPopupMenu();
				menu.add(addTrack);
				menu.add(duplicateTrack);
				menu.add(removeTrack);
				menu.addSeparator();
				menu.add(divisionMenu);

				menu.show(e.getComponent(), e.getX(), e.getY());
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			if(e.getButton() == MouseEvent.BUTTON1) {
				if(tempPart != null) {
					// move/copy selected part
					long startTime = getTime(startX);
					long endTime = getTime(e.getX());
					long dt = quantizeTime(endTime - startTime);

					if((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0) {
						if((e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0) {
							setSelectedPart(selectedPart.link(partTime + dt));
						} else {
							setSelectedPart(selectedPart.clone(partTime + dt));
						}
					} else if(selectedPart != null) {
						setSelectedPart(selectedPart.move(partTime + dt));
					}
				}
			}

			tempPart = null;
			hiddenPart = null;
			repaint();
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			// TODO Auto-generated method stub
		}

		@Override
		public void mouseExited(MouseEvent e) {
			// TODO Auto-generated method stub
		}

		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			int notches = e.getWheelRotation();

			if((e.getModifiersEx() & MouseWheelEvent.CTRL_DOWN_MASK) != 0) {
				// zoom
				int px = e.getX() - CONTENT_X;
				if(px < 0) {
					return;
				}

				double div = Math.pow(2.0, -notches);

				double factor = 384.0 / ppq;
				if(timeScale * div < factor * (1.0 / 64)) {
					return;
				} else if(timeScale * div > factor * 16) {
					return;
				}

				// adjust time offset
				long time = (long) ((px + offsetX) / timeScale);
				offsetX = (int) ((time * timeScale * div) - px);
				if(offsetX < 0) {
					offsetX = 0;
				}

				// set new time scale
				setTimeScale(timeScale * div);
			} else if((e.getModifiersEx() & MouseWheelEvent.SHIFT_DOWN_MASK) != 0) {
				// scroll X
				setOffsetX(offsetX + (int) Math.round(notches * grid * timeScale));
			} else {
				// scroll Y
				setOffsetY(offsetY + notches);
			}
		}
	}
}
