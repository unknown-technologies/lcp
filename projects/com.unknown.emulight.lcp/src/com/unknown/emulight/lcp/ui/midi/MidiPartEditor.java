package com.unknown.emulight.lcp.ui.midi;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent.Cause;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

import com.unknown.audio.analysis.MIDINames;
import com.unknown.emulight.lcp.sequencer.MidiPart;
import com.unknown.emulight.lcp.sequencer.Note;
import com.unknown.emulight.lcp.ui.event.PreviewListener;
import com.unknown.emulight.lcp.ui.laser.Callback;
import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;
import com.unknown.util.ui.ADM3AFont;

@SuppressWarnings("serial")
public class MidiPartEditor extends JComponent {
	private static final Logger log = Trace.create(MidiPartEditor.class);

	private static final Color BACKGROUND = new Color(0x25282B);
	private static final Color KEY_WHITE = new Color(0x25282B);
	private static final Color KEY_BLACK = new Color(0x212326);
	private static final Color GRID_PRIMARY = new Color(0x595B5D);
	private static final Color GRID_SECONDARY = new Color(0x36393B);
	private static final Color GRID_BAR = new Color(0x77797A);
	private static final Color GRID_BEAT = new Color(0x595B5D);
	private static final Color GRID_SUBFRAME = new Color(0x36393B);
	private static final Color NOTE_1 = new Color(0x2382E3);
	private static final Color NOTE_127 = new Color(0xE42222);

	private static final Color INFO_TEXT = new Color(0x595B5D);
	private static final Color NOTE_NAME = new Color(0xFFFFFF);

	private static final Color SELECTION_BORDER = new Color(0xE0E0E0);
	private static final Color SELECTION_FILL = new Color(0x40E0E0E0, true);

	private static final Color TRANSPARENT = new Color(0, 0, 0, 0);

	private static final int CELL_HEIGHT = 20;

	private static final int RESIZE_AREA = 5;

	private double timeScale = 0.25;

	private double defaultTimeScale = 0.25;

	private int offsetX;
	private int offsetY;

	private int signatureNumerator = 4;
	private int signatureDenominator = 4;
	private int ppq = 96;

	private int grid = ppq;
	private int division = 1;

	private int defaultDivision = 1;

	private Set<Note> selection;
	private Rectangle selectionRectangle;

	private MidiPart part;

	private MouseController mouse;

	private List<PreviewListener> listeners;

	private long partStartTime;

	private final Callback updater;

	public MidiPartEditor(MidiPart part, Callback callback) {
		this.part = part;
		this.updater = callback;

		ppq = part.getPPQ();
		partStartTime = 0;

		setDefaultTimeScale(timeScale * 96.0 / ppq);

		selection = new HashSet<>();

		setBackground(BACKGROUND);
		setForeground(Color.WHITE);

		// setMinimumSize(new Dimension(1, 128 * CELL_HEIGHT));
		// setSize(new Dimension(1, 128 * CELL_HEIGHT));

		mouse = new MouseController();
		addMouseListener(mouse);
		addMouseMotionListener(mouse);
		addMouseWheelListener(mouse);

		setFocusable(true);
		setFocusTraversalKeysEnabled(false);

		addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
				// nothing
			}

			@Override
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_DELETE) {
					deleteSelectedNotes();
				} else if(e.getKeyCode() == KeyEvent.VK_A &&
						(e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0) {
					selection.clear();
					selection.addAll(part.getNotes());
					repaint();
				} else if(e.getKeyCode() == KeyEvent.VK_UP) {
					boolean octave = (e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0;
					for(Note note : selection) {
						int key = note.getKey();
						if(octave) {
							key += 12;
						} else {
							key++;
						}
						if(key > 127) {
							key = 127;
						}
						note.setKey(key);
					}
					updater.callback();
					repaint();
				} else if(e.getKeyCode() == KeyEvent.VK_DOWN) {
					boolean octave = (e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0;
					for(Note note : selection) {
						int key = note.getKey();
						if(octave) {
							key -= 12;
						} else {
							key--;
						}
						if(key < 0) {
							key = 0;
						}
						note.setKey(key);
					}
					updater.callback();
					repaint();
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
				// nothing
			}
		});

		setDivision(defaultDivision);
		setTimeScale(defaultTimeScale);

		listeners = new ArrayList<>();

		addComponentListener(new ComponentAdapter() {
			private boolean initial = true;
			private Dimension size = null;

			@Override
			public void componentResized(ComponentEvent e) {
				if(initial) {
					autoZoom();
					initial = false;
					size = getSize();
				} else {
					rezoom(size);
					size = getSize();
				}
			}
		});
	}

	public void addPreviewListener(PreviewListener listener) {
		listeners.add(listener);
	}

	public void removePreviewListener(PreviewListener listener) {
		listeners.remove(listener);
	}

	protected void firePreviewPressed(Note note) {
		for(PreviewListener l : listeners) {
			try {
				l.pressed(note);
			} catch(Throwable t) {
				log.log(Levels.ERROR, "Failed to execute preview listener: " + t.getMessage(), t);
			}
		}
	}

	protected void firePreviewReleased(Note note) {
		for(PreviewListener l : listeners) {
			try {
				l.released(note);
			} catch(Throwable t) {
				log.log(Levels.ERROR, "Failed to execute preview listener: " + t.getMessage(), t);
			}
		}
	}

	public MidiPart getPart() {
		return part;
	}

	public void setPart(MidiPart part) {
		this.part = part;
		reload();
	}

	public long getStartTime() {
		return partStartTime;
	}

	public void setStartTime(long startTime) {
		this.partStartTime = startTime;
		repaint();
	}

	public void reload() {
		ppq = part.getPPQ();
		grid = ppq / division;

		selection.clear();
		mouse.reset();

		repaint();
	}

	public void autoZoom() {
		// find key range
		int minKey = 128;
		int maxKey = 0;

		for(Note note : part.getNotes()) {
			int key = note.getKey();
			if(key > maxKey) {
				maxKey = key;
			}
			if(key < minKey) {
				minKey = key;
			}
		}

		if(minKey > maxKey) {
			minKey = 60;
			maxKey = 60;
		}

		// now put minKey - maxKey into the middle of the window if possible
		int height = getHeight();
		int visibleKeys = height / CELL_HEIGHT;
		int center = (minKey + maxKey) / 2;
		int offset = center + (visibleKeys / 2);
		offset = 127 - offset;

		setOffsetY(offset * CELL_HEIGHT);
	}

	public void rezoom(Dimension oldSize) {
		int oldcy = oldSize.height / 2;
		int newcy = getHeight() / 2;
		int delta = newcy - oldcy;
		int offset = offsetY - delta;
		setOffsetY(offset);
	}

	public void setDefaultTimeScale(double scale) {
		defaultTimeScale = scale;
	}

	public void setTimeScale(double scale) {
		timeScale = scale;
		repaint();
	}

	public void setDivision(int division) {
		this.division = division;
		grid = ppq / division;
		repaint();
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

		int limitY = 128 * CELL_HEIGHT;
		if(offsetY < 0) {
			offsetY = 0;
		} else if(offsetY > limitY - getHeight()) {
			offsetY = limitY - getHeight();
		}

		repaint();
	}

	private static Color interpolate(Color x, Color y, float mix) {
		int r = Math.round(x.getRed() * (1.0f - mix) + y.getRed() * mix);
		int g = Math.round(x.getGreen() * (1.0f - mix) + y.getGreen() * mix);
		int b = Math.round(x.getBlue() * (1.0f - mix) + y.getBlue() * mix);
		return new Color(r, g, b);
	}

	private static boolean isWhiteKey(int key) {
		switch(key % 12) {
		case 0:
		case 2:
		case 4:
		case 5:
		case 7:
		case 9:
		case 11:
			return true;
		default:
			return false;
		}
	}

	private void deleteSelectedNotes() {
		for(Note note : selection) {
			part.removeNote(note);
		}
		if(!selection.isEmpty()) {
			updater.callback();
		}
		selection.clear();
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		int width = getWidth();
		int height = getHeight();

		// fill background
		g.setColor(BACKGROUND);
		g.fillRect(0, 0, width, height);

		for(int i = 0; i < 128; i++) {
			int startY = (127 - i) * CELL_HEIGHT - offsetY;
			int endY = startY + CELL_HEIGHT;

			// draw background
			if(isWhiteKey(i)) {
				g.setColor(KEY_WHITE);
			} else {
				g.setColor(KEY_BLACK);
			}
			g.fillRect(0, startY, width, CELL_HEIGHT - 1);

			if(i % 12 == 0) {
				g.setColor(GRID_PRIMARY);
			} else {
				g.setColor(GRID_SECONDARY);
			}
			g.drawLine(0, endY, width, endY);
		}

		// draw vertical grid
		int cellCount = (int) Math.round(width / (grid * timeScale)) + 2;
		for(int i = 0; i < cellCount; i++) {
			int posX = (int) Math.round(i * grid * timeScale -
					(offsetX + partStartTime * timeScale) % (grid * timeScale));
			int off = (int) ((offsetX + partStartTime * timeScale) / (grid * timeScale));
			int n = i + off;

			if(n % (signatureNumerator * signatureDenominator * division) == 0) {
				g.setColor(GRID_BAR);
			} else if(n % (signatureDenominator * division) == 0) {
				g.setColor(GRID_BEAT);
			} else {
				g.setColor(GRID_SUBFRAME);
			}

			g.drawLine(posX, 0, posX, height);
		}

		for(int i = 0; i < 128; i++) {
			int startY = (127 - i) * CELL_HEIGHT - offsetY;
			int endY = startY + CELL_HEIGHT;

			// draw key names
			int posY = endY - (CELL_HEIGHT - ADM3AFont.HEIGHT) / 2;
			ADM3AFont.render(g, 5, posY, INFO_TEXT, TRANSPARENT, MIDINames.getNoteName(i));
		}

		// draw beat/bar IDs
		for(int i = 0; i < cellCount; i++) {
			int posX = (int) Math.round(i * grid * timeScale -
					(offsetX + partStartTime * timeScale) % (grid * timeScale));
			int off = (int) ((offsetX + partStartTime * timeScale) / (grid * timeScale));
			int n = i + off;

			int bar = n / (signatureNumerator * signatureDenominator * division) + 1;
			int beat = (n / (signatureDenominator * division)) % signatureNumerator + 1;

			String text;
			if(n % (signatureNumerator * signatureDenominator * division) == 0) {
				text = Integer.toString(bar);
			} else if(n % (signatureDenominator * division) == 0) {
				text = bar + "." + beat;
			} else {
				continue;
			}

			ADM3AFont.render(g, posX + 3, ADM3AFont.HEIGHT + 3, INFO_TEXT, TRANSPARENT, text);
		}

		// render notes
		for(Note note : part.getNotes()) {
			int posX = (int) Math.round(note.getTime() * timeScale);
			int posY = (127 - note.getKey()) * CELL_HEIGHT;
			int length = (int) Math.round(note.getLength() * timeScale);

			// skip invisible notes
			if(posX - offsetX > width || posX - offsetX + length < 0) {
				continue;
			}

			Color color = interpolate(NOTE_1, NOTE_127, note.getVelocity() / 127.0f);
			int startX = posX - offsetX;
			int startY = posY - offsetY;

			if(selection.contains(note)) {
				g.setColor(new Color(color.getRed() / 4, color.getGreen() / 4, color.getBlue() / 4));
				g.fillRect(startX, startY + 1, length, CELL_HEIGHT - 1);

				// draw outline
				g.setColor(color);
				g.drawRect(startX, startY, length, CELL_HEIGHT);
			} else {
				// draw body
				g.setColor(color);
				g.fillRect(startX, startY + 1, length, CELL_HEIGHT - 1);

				// draw outline
				g.setColor(new Color(color.getRed() / 2, color.getGreen() / 2, color.getBlue() / 2));
				g.drawRect(startX, startY, length, CELL_HEIGHT);
			}

			// render note name if there is enough space
			String name = MIDINames.getNoteName(note.getKey());
			if(length > name.length() * ADM3AFont.WIDTH + 6) {
				int py = startY + (2 * CELL_HEIGHT - ADM3AFont.HEIGHT) / 2;
				ADM3AFont.render(g, startX + 5, py, NOTE_NAME, TRANSPARENT, name);
			}
		}

		if(selectionRectangle != null) {
			g.setColor(SELECTION_FILL);
			g.fillRect(selectionRectangle.x, selectionRectangle.y, selectionRectangle.width,
					selectionRectangle.height);
			g.setColor(SELECTION_BORDER);
			g.drawRect(selectionRectangle.x, selectionRectangle.y, selectionRectangle.width,
					selectionRectangle.height);
		}
	}

	private int getKey(int y) {
		return 127 - (y + offsetY) / CELL_HEIGHT;
	}

	private long getTime(int x) {
		return (long) ((x + offsetX) / timeScale);
	}

	private int getPixel(long time) {
		return (int) Math.round((time * timeScale) - offsetX);
	}

	private long quantizeTime(long time) {
		return Math.round(time / (grid * 1.0)) * grid;
	}

	private class MouseController implements MouseListener, MouseMotionListener, MouseWheelListener {
		private int startX;
		private int startY;

		private int startOffsetX;
		private int startOffsetY;

		private boolean newNote;
		private boolean resize;
		private boolean resizeEnd;
		private Note currentNote;

		private int lastLength = ppq;

		private Map<Note, Long> startTimes = new HashMap<>();
		private Map<Note, Integer> startLengths = new HashMap<>();
		private Map<Note, Integer> startKeys = new HashMap<>();

		private Note preview;

		private void setPreview(Note note) {
			if(preview != null) {
				firePreviewReleased(preview);
			}

			if(note == null) {
				preview = null;
				return;
			}

			preview = new Note(note.getTime(), note.getKey(), note.getVelocity(), note.getLength());
			if(note != null) {
				firePreviewPressed(note);
			}
		}

		private void reset() {
			newNote = false;
			resize = false;
			resizeEnd = false;
			currentNote = null;
			lastLength = ppq;
			startTimes.clear();
			startLengths.clear();
			startKeys.clear();
		}

		private void updateSelection() {
			startTimes.clear();
			startKeys.clear();
			for(Note note : selection) {
				startTimes.put(note, note.getTime());
				startLengths.put(note, note.getLength());
				startKeys.put(note, note.getKey());
			}
		}

		private void updateCursor(int x, int y) {
			long time = getTime(x);
			int key = getKey(y);
			boolean match = false;

			Collection<Note> notes = selection.isEmpty() ? part.getNotes() : selection;
			for(Note note : notes) {
				if(note.getKey() == key && note.containsTime(time)) {
					if(Math.abs(getPixel(note.getTime()) - x) < RESIZE_AREA) {
						// update mouse cursor
						setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
						return;
					} else if(Math.abs(getPixel(note.getTime() + note.getLength()) -
							x) < RESIZE_AREA) {
						// update mouse cursor
						setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
						return;
					} else {
						match = true;
					}
				}
			}

			if(match) {
				setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
			} else {
				setCursor(Cursor.getDefaultCursor());
			}
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			updateCursor(e.getX(), e.getY());
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			if((e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0) {
				if(newNote) {
					// new note: change length
					long time = getTime(e.getX());
					int length = (int) (time - currentNote.getTime());

					// quantize?
					if((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) == 0) {
						length = (int) quantizeTime(length);
					}

					if(length > 0) {
						currentNote.setLength(length);
						updater.callback();
						repaint();
					}
				} else if(resize) {
					long startTime = getTime(startX);
					long endTime = getTime(e.getX());
					long dt = endTime - startTime;

					// quantize?
					if((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) == 0) {
						dt = quantizeTime(dt);
					}

					if(resizeEnd) {
						for(Note note : selection) {
							int length = startLengths.get(note);
							if(length + dt > 0) {
								note.setLength(length + (int) dt);
							}
						}
					} else {
						for(Note note : selection) {
							long time = startTimes.get(note);
							int length = startLengths.get(note);
							note.setTime(time + dt);
							if(length - dt > 0) {
								note.setLength(length - (int) dt);
							}
						}
					}

					updater.callback();
					repaint();
				} else if(selectionRectangle != null ||
						(e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0) {
					int sx = startX;
					int sy = startY;
					int ex = e.getX();
					int ey = e.getY();

					// swap start/end if necessary
					if(ex < sx) {
						int tmp = ex;
						ex = sx;
						sx = tmp;
					}

					if(ey < sy) {
						int tmp = ey;
						ey = sy;
						sy = tmp;
					}

					// update rectangle
					int w = ex - sx;
					int h = ey - sy;
					selectionRectangle = new Rectangle(sx, sy, w, h);

					repaint();
				} else {
					// existing note: move

					// move time
					long startTime = getTime(startX);
					long endTime = getTime(e.getX());
					long dt = quantizeTime(endTime - startTime);

					long mint = Integer.MAX_VALUE;
					int minkey = 128;
					int maxkey = 0;
					for(Note note : selection) {
						long t = startTimes.get(note);
						if(t < mint) {
							mint = t;
						}
						int key = startKeys.get(note);
						if(key < minkey) {
							minkey = key;
						}
						if(key > maxkey) {
							maxkey = key;
						}
					}

					// clamp
					if(mint + dt < 0) {
						dt = -mint;
					}

					for(Note note : selection) {
						long t = startTimes.get(note);
						note.setTime(t + dt);
					}
					part.sort();

					// move key
					int startKey = getKey(startY);
					int endKey = getKey(e.getY());
					int dk = endKey - startKey;

					// clamp
					if(minkey + dk < 0) {
						dk = -minkey;
					}
					if(maxkey + dk > 127) {
						dk = 127 - maxkey;
					}

					for(Note note : selection) {
						int key = startKeys.get(note);
						note.setKey(key + dk);
					}

					// update preview
					if(selection.size() == 1) {
						setPreview(selection.iterator().next());
					}

					updater.callback();
					repaint();
				}
			} else if((e.getModifiersEx() & MouseEvent.BUTTON2_DOWN_MASK) != 0) {
				// middle mouse button
				int dx = e.getX() - startX;
				int dy = e.getY() - startY;

				setOffsetX(startOffsetX - dx);
				setOffsetY(startOffsetY - dy);

				repaint();
			} else if((e.getModifiersEx() & MouseEvent.BUTTON3_DOWN_MASK) != 0) {
				if((e.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) == 0) {
					return;
				}

				int x = e.getX();
				int y = e.getY();
				int key = getKey(y);
				long time = getTime(x);

				// remove note if there is any
				Set<Note> remove = new HashSet<>();
				for(Note note : part.getNotes()) {
					if(note.getKey() == key && note.containsTime(time)) {
						remove.add(note);
					}
				}

				for(Note note : remove) {
					selection.remove(note);
					part.removeNote(note);
				}

				if(!remove.isEmpty()) {
					updater.callback();
					repaint();
				}
			}
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			// nothing
		}

		@Override
		public void mousePressed(MouseEvent e) {
			requestFocusInWindow(Cause.MOUSE_EVENT);

			// update state
			startX = e.getX();
			startY = e.getY();

			startOffsetX = offsetX;
			startOffsetY = offsetY;

			currentNote = null;
			newNote = false;
			resize = false;

			selectionRectangle = null;

			int x = e.getX();
			int y = e.getY();
			int key = getKey(y);
			long time = getTime(x);

			if(e.getButton() == MouseEvent.BUTTON1) {
				// is there a note already? If yes, select it
				for(Note note : part.getNotes()) {
					if(note.getKey() == key && note.containsTime(time)) {
						resizeEnd = Math.abs(getPixel(note.getTime() + note.getLength()) -
								x) < RESIZE_AREA;
						if(Math.abs(getPixel(note.getTime()) - x) < RESIZE_AREA ||
								resizeEnd) {
							if(selection.isEmpty()) {
								resize = true;
								selection.add(note);
								updateCursor(x, y);
								updateSelection();
								repaint();
								return;
							} else if(selection.contains(note)) {
								resize = true;
								updateSelection();
								return;
							}
						}

						if((e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0) {
							if(selection.contains(note)) {
								selection.remove(note);
							} else {
								selection.add(note);
							}
						} else if(selection.contains(note)) {
							lastLength = note.getLength();
							setPreview(note);
							updateSelection();
							return;
						} else {
							selection.clear();
							selection.add(note);
							setPreview(note);
							lastLength = note.getLength();
						}
						updateSelection();
						repaint();
						return;
					}
				}

				if(!selection.isEmpty()) {
					if((e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0) {
						// editing the selection, don't clear it
						return;
					}

					// clear selection
					selection.clear();
					repaint();
				}

				// quantize to grid
				time = (time / grid) * grid;

				if((e.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) != 0) {
					// otherwise add new note
					currentNote = new Note(time, key, 100, lastLength);
					part.addNote(currentNote);
					setPreview(currentNote);
					newNote = true;
					updater.callback();
				} else {
					selectionRectangle = new Rectangle(x, y, 1, 1);
				}

				repaint();
			} else if(e.getButton() == MouseEvent.BUTTON3) {
				if((e.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) != 0) {
					// remove note if there is any
					Set<Note> remove = new HashSet<>();
					for(Note note : part.getNotes()) {
						if(note.getKey() == key && note.containsTime(time)) {
							remove.add(note);
						}
					}

					for(Note note : remove) {
						selection.remove(note);
						part.removeNote(note);
					}

					if(!remove.isEmpty()) {
						updater.callback();
						repaint();
					}
				} else {
					JMenuItem reset = new JMenuItem("Reset");
					reset.setMnemonic('R');
					reset.addActionListener(ev -> {
						setTimeScale(defaultTimeScale);
						setDivision(defaultDivision);
						offsetX = 0;
						offsetY = 0;
						repaint();
					});

					ButtonGroup divisionGroup = new ButtonGroup();

					JRadioButtonMenuItem div1 = new JRadioButtonMenuItem("Division: 1",
							division == 1);
					div1.setMnemonic('1');
					div1.addActionListener(ev -> setDivision(1));
					divisionGroup.add(div1);

					JRadioButtonMenuItem div2 = new JRadioButtonMenuItem("Division: 2",
							division == 2);
					div2.setMnemonic('2');
					div2.addActionListener(ev -> setDivision(2));
					divisionGroup.add(div2);

					JRadioButtonMenuItem div4 = new JRadioButtonMenuItem("Division: 4",
							division == 4);
					div4.setMnemonic('4');
					div4.addActionListener(ev -> setDivision(4));
					divisionGroup.add(div4);

					JRadioButtonMenuItem div8 = new JRadioButtonMenuItem("Division: 8",
							division == 8);
					div8.setMnemonic('8');
					div8.addActionListener(ev -> setDivision(8));
					divisionGroup.add(div8);

					JRadioButtonMenuItem div16 = new JRadioButtonMenuItem("Division: 16",
							division == 16);
					div16.setMnemonic('6');
					div16.addActionListener(ev -> setDivision(16));
					divisionGroup.add(div16);

					JPopupMenu menu = new JPopupMenu();
					menu.add(reset);
					menu.addSeparator();
					menu.add(div1);
					menu.add(div2);
					menu.add(div4);
					menu.add(div8);
					menu.add(div16);

					menu.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			setPreview(null);

			if(resize && !resizeEnd) {
				part.sort();
			}

			if(currentNote != null && newNote) {
				lastLength = currentNote.getLength();
			}

			currentNote = null;

			if(selectionRectangle != null) {
				int sx = startX;
				int sy = startY;
				int ex = e.getX();
				int ey = e.getY();

				// swap start/end if necessary
				if(ex < sx) {
					int tmp = ex;
					ex = sx;
					sx = tmp;
				}

				if(ey < sy) {
					int tmp = ey;
					ey = sy;
					sy = tmp;
				}

				// clear previous selection if not in "add" mode
				if((e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) == 0) {
					selection.clear();
				}

				// now find all notes within this rectangle
				int minKey = getKey(ey);
				int maxKey = getKey(sy);

				for(Note note : part.getNotes()) {
					int noteMinX = getPixel(note.getTime());
					int noteMaxX = getPixel(note.getEnd());
					if(noteMinX >= sx && noteMaxX <= ex && note.getKey() >= minKey &&
							note.getKey() <= maxKey) {
						selection.add(note);
					}
				}

				selectionRectangle = null;
				repaint();
			}
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			// ignored
		}

		@Override
		public void mouseExited(MouseEvent e) {
			// ignored
		}

		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			int notches = e.getWheelRotation();

			if((e.getModifiersEx() & MouseWheelEvent.CTRL_DOWN_MASK) != 0) {
				// zoom
				int px = e.getX();

				double div = Math.pow(2.0, -notches);

				double factor = 96.0 / ppq;
				if(timeScale * div < factor * (1.0 / 16)) {
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
				setOffsetY(offsetY + notches * CELL_HEIGHT);
			}
		}
	}
}
