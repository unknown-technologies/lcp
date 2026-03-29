package com.unknown.emulight.lcp.ui.midi;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent.Cause;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;

import com.unknown.emulight.lcp.event.SequencerListener;
import com.unknown.emulight.lcp.project.PartContainer;
import com.unknown.emulight.lcp.project.Project;
import com.unknown.emulight.lcp.project.TempoTrack;
import com.unknown.emulight.lcp.sequencer.TempoPart;
import com.unknown.emulight.lcp.ui.laser.Callback;
import com.unknown.math.g3d.Mtx44;
import com.unknown.math.g3d.Vec2;
import com.unknown.math.g3d.Vec3;
import com.unknown.util.ui.ADM3AFont;

@SuppressWarnings("serial")
public class TempoPartEditorDialog extends JFrame {
	private static final int MENU_MODIFIER = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

	private static final String TITLE = "Tempo Editor";

	private static final double BPM_MIN = 0.0;
	private static final double BPM_MAX = 600.0;

	private static final int TEXT_PADDING = 5;
	private static final int PADDING = 8;
	private static final int START_X = 2 * PADDING + 7 * ADM3AFont.WIDTH + 1;
	private static final int START_Y = PADDING + ADM3AFont.HEIGHT;

	private static final Color BACKGROUND = new Color(0x25282B);
	private static final Color GRID_BORDER = new Color(0x595B5D);
	private static final Color GRID_COLOR = new Color(0x36393B);
	private static final Color GRID_BAR = new Color(0x77797A);
	private static final Color GRID_BEAT = new Color(0x595B5D);
	private static final Color GRID_SUBFRAME = new Color(0x36393B);
	private static final Color INFO_TEXT = new Color(0x00FFFF);

	private static final Color TRANSPARENT = new Color(0, true);
	private static final Color OVERLAY_BACKGROUND = new Color(BACKGROUND.getRed(), BACKGROUND.getGreen(),
			BACKGROUND.getBlue(), 192);
	private static final Color TEXT_COLOR = Color.CYAN;

	private static final Color LINE_COLOR = Color.LIGHT_GRAY;

	private static final int POINT_SIZE = 4;

	private TempoPart part;
	private Editor editor;
	private PropertyPanel propertyPanel;

	private Vec3 translation = new Vec3(0.0, 0.0, 0.0);
	private double iscaleY = 0;
	private double timeScale = 0.25;
	private double scaleY = 1.0;

	private int mouseX = 0;
	private int mouseY = 0;

	private Axis cachedAxis = null;
	private int cachedWidth = 0;
	private int cachedHeight = 0;

	private long selectedTime = 0;

	private final List<Updater> updater = new ArrayList<>();
	private boolean inCallback = false;
	private boolean bypassEvents = false;

	private final Callback update;

	private boolean drawOverlay = false;

	private double defaultTimeScale = 0.25;

	private int signatureNumerator = 4;
	private int signatureDenominator = 4;
	private int ppq = 96;

	private double grid = ppq / 4.0;
	private int division = 1;

	private int defaultDivision = 16;

	private long partStartTime;

	private final Project project;
	private final TempoTrack track;

	public TempoPartEditorDialog(PartContainer<TempoPart> container, Callback update) {
		super(TITLE);
		this.project = container.getTrack().getProject();
		this.track = (TempoTrack) container.getTrack();
		this.partStartTime = container.getTime();
		this.update = update;

		ppq = project.getPPQ();

		editor = new Editor();
		propertyPanel = new PropertyPanel();

		setLayout(new BorderLayout());
		add(BorderLayout.NORTH, propertyPanel);
		add(BorderLayout.CENTER, editor);

		setSize(640, 480);
		setLocationRelativeTo(container.getTrack().getProject().getSystem().getMainWindow());

		setDefaultTimeScale(timeScale * 384.0 / ppq);

		Timer timer = new Timer(50, e -> editor.repaint());
		timer.setRepeats(true);

		SequencerListener listener = new SequencerListener() {
			public void playbackStarted() {
				timer.start();
			}

			public void playbackStopped() {
				timer.stop();
				editor.repaint();
			}

			public void positionChanged(long tick) {
				editor.repaint();
			}
		};

		project.getSequencer().addListener(listener);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				project.getSequencer().removeListener(listener);
			}
		});

		KeyStroke quitKey = KeyStroke.getKeyStroke(KeyEvent.VK_Q, MENU_MODIFIER);
		KeyStroke escKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		editor.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(quitKey, quitKey);
		editor.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escKey, quitKey);
		editor.getActionMap().put(quitKey, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				project.getSequencer().removeListener(listener);
				dispose();
			}
		});

		setPart(container.getPart());

		setDivision(defaultDivision);
		setTimeScale(defaultTimeScale);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}

	public long getStartTime() {
		return partStartTime;
	}

	public void setStartTime(long startTime) {
		this.partStartTime = startTime;
		editor.repaint();
	}

	public void setDefaultTimeScale(double scale) {
		defaultTimeScale = scale;
	}

	public void setTimeScale(double scale) {
		timeScale = scale;
		editor.repaint();
	}

	public void setDivision(int division) {
		this.division = division;
		grid = ppq / 4.0 / division;
		((SpinnerNumberModel) propertyPanel.time.getModel()).setStepSize(Math.round(grid));
		editor.repaint();
	}

	public void setPart(TempoPart part) {
		// reference comparison is intentional
		if(part != this.part) {
			iscaleY = 0;
			scaleY = 1.0;
			translation = new Vec3(0.0, 0.0, 0.0);
			setTimeScale(defaultTimeScale);
		}

		this.part = part;
		cachedAxis = null;
		update();
		propertyPanel.change();
	}

	public void update() {
		if(part == null) {
			return;
		}

		setTitle(TITLE + ": " + part.getName());

		ppq = project.getPPQ();
		grid = ppq / 4.0 / division;

		editor.repaint();
	}

	protected void updateView() {
		editor.repaint();
		update.callback();
	}

	public void updateFields() {
		update.callback();
		if(inCallback) {
			return;
		}

		for(Updater u : updater) {
			u.update();
		}
	}

	private Mtx44 getTransformation(int height) {
		return Mtx44.scale(timeScale, -scaleY, 1.0).transApply(translation.x, translation.y + height,
				translation.z);
	}

	protected void setValue(long time, double value) {
		part.setTempo(time, value);
		track.recompute();
		updateFields();
		repaint();
	}

	@FunctionalInterface
	private interface Updater {
		void update();
	}

	private static class Axis {
		public final double min;
		public final double max;
		public final int divisions;
		public final Mtx44 mtx;

		public Axis(Mtx44 mtx, double min, double max, int divisions) {
			this.mtx = mtx;
			this.min = min;
			this.max = max;
			this.divisions = divisions;
		}
	}

	private class PropertyPanel extends JPanel {
		private final static int SPINNER_WIDTH = 75;

		private JSpinner time;

		public PropertyPanel() {
			super(new FlowLayout(FlowLayout.LEFT));
		}

		public void change() {
			removeAll();
			updater.clear();
			if(part == null) {
				revalidate();
				repaint();
				return;
			}

			add(new JLabel("Time:"));
			time = new JSpinner(new SpinnerNumberModel(selectedTime, 0L, (long) Integer.MAX_VALUE,
					(Number) Math.round(grid)));
			time.addChangeListener(e -> {
				if(!bypassEvents) {
					long t = (long) time.getValue();
					double bpm = part.getTempo(selectedTime);
					part.deleteTempo(selectedTime);
					part.setTempo(t, bpm);
					track.recompute();
					selectedTime = t;
					updateView();
				}
			});
			updater.add(() -> {
				try {
					bypassEvents = true;
					time.setValue(selectedTime);
				} finally {
					bypassEvents = false;
				}
			});
			add(time);

			add(new JLabel("Value:"));
			JSpinner number = new JSpinner(
					new SpinnerNumberModel(part.getTempo(selectedTime), BPM_MIN, BPM_MAX, 1.0));
			number.addChangeListener(e -> {
				if(!bypassEvents) {
					setValue(selectedTime, (double) number.getValue());
					updateView();
				}
			});
			updater.add(() -> {
				try {
					bypassEvents = true;
					number.setValue(part.getTempo(selectedTime));
				} finally {
					bypassEvents = false;
				}
			});
			Dimension size = number.getMinimumSize();
			Dimension minsz = new Dimension(SPINNER_WIDTH, size.height);
			number.setMinimumSize(minsz);
			number.setPreferredSize(minsz);
			add(number);

			revalidate();
			repaint();
		}
	}

	private class Editor extends JComponent {
		public Editor() {
			setBackground(BACKGROUND);
			setOpaque(true);
			setDoubleBuffered(true);
			MouseController mouse = new MouseController();
			addMouseListener(mouse);
			addMouseMotionListener(mouse);
			addMouseWheelListener(mouse);

			setFocusable(true);
			setRequestFocusEnabled(true);
			setFocusTraversalKeysEnabled(false);
		}

		@Override
		protected void paintComponent(Graphics g) {
			int width = getWidth();
			int height = getHeight();

			g.setColor(getBackground());
			g.fillRect(0, 0, width, height);

			if(part == null) {
				ADM3AFont.render(g, TEXT_PADDING, TEXT_PADDING + ADM3AFont.HEIGHT, TEXT_COLOR,
						TRANSPARENT, "No part selected");
				return;
			}

			String[] overlay = new String[] {
					"Points: " + part.getTempoMap().size()
			};

			int startX = START_X - 1;
			int startY = START_Y - 1;

			// render grid
			g.setColor(GRID_BORDER);
			g.drawLine(startX, height - startY, width, height - startY);
			g.drawLine(startX, height - startY, startX, 0);

			startX++;
			startY++;

			int h = height - startY;
			Graphics gx = g.create(startX, 0, width - startX, h + 1);

			Axis axis = getAxis(width, height);

			if(axis != null) {
				Mtx44 mtx = axis.mtx;

				gx.setColor(GRID_COLOR);

				// render Y axis and labels
				int lenMin = (int) Math.ceil(Math.log10(Math.abs(axis.min)));
				int lenMax = (int) Math.ceil(Math.log10(Math.abs(axis.max)));
				boolean neg = false;
				if(axis.min < 0) {
					lenMin++;
					neg = true;
				}
				if(axis.max < 0) {
					lenMax++;
					neg = true;
				}
				int len = lenMin > lenMax ? lenMin : lenMax;
				String format;
				if(neg) {
					if(len > 3) {
						format = "% 6.2f";
					} else if(len == 2) {
						format = "% 6.3f";
					} else {
						format = "% 6.4f";
					}
				} else {
					if(len > 3) {
						format = "%7.2f";
					} else if(len == 3) {
						format = "%7.3f";
					} else if(len == 2) {
						format = "%7.4f";
					} else {
						format = "%7.5f";
					}
				}

				Vec2 range = getRange(width, height);
				double stepSize = (axis.max - axis.min) / axis.divisions;
				int min = (int) Math.round((range.x - axis.min) / stepSize);
				int max = (int) Math.round((range.y - axis.min) / stepSize);
				if(min < 0) {
					min = 0;
				}
				if(max > axis.divisions) {
					max = axis.divisions;
				}
				for(int i = min; i <= max; i++) {
					double value = axis.min + stepSize * i;
					Vec3 p = mtx.mult(new Vec3(0, value, 0));
					int py = (int) Math.round(p.y);

					gx.drawLine(0, py, width, py);

					String s = String.format(format, value);
					ADM3AFont.render(g, PADDING, py + ADM3AFont.HEIGHT / 2, TEXT_COLOR, TRANSPARENT,
							s);
				}
			}

			// draw vertical grid
			int offsetX = (int) Math.round(-translation.x);

			int contentWidth = width - startX;
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
				int posX = (int) Math.round(i * autogrid * timeScale -
						(offsetX + partStartTime * timeScale) % (autogrid * timeScale));
				int off = (int) ((offsetX + partStartTime * timeScale) / (autogrid * timeScale));
				int n = i + off;

				if(posX < 0) {
					continue;
				}

				posX += startX;

				int bar = n / (signatureNumerator * signatureDenominator * autodivision) + 1;
				int beat = (n / (signatureDenominator * autodivision)) % signatureNumerator + 1;

				String text = null;

				if(n % (signatureNumerator * signatureDenominator * autodivision) == 0) {
					g.setColor(GRID_BAR);
					text = Integer.toString(bar);
				} else if(n % (signatureDenominator * autodivision) == 0) {
					if(!drawBeat) {
						continue;
					}
					g.setColor(GRID_BEAT);
					if(drawBeatText) {
						text = bar + "." + beat;
					}
				} else {
					if(!drawGrid) {
						continue;
					}
					g.setColor(GRID_SUBFRAME);
				}

				g.drawLine(posX, 0, posX, height - startY);

				// draw beat/bar IDs
				if(text != null) {
					ADM3AFont.render(g, posX, height - startY + ADM3AFont.HEIGHT + 2, INFO_TEXT,
							TRANSPARENT, text);
				}
			}

			// draw cursor BEFORE the data points, otherwise points below the cursor are invisible
			int cursor = getPixel(project.getSequencer().getTick() - partStartTime);
			if(cursor >= START_X && cursor <= width) {
				g.setColor(Color.BLACK);
				g.drawLine(cursor, 0, cursor, height - START_Y);
				g.setColor(Color.WHITE);
				g.drawLine(cursor - 1, 0, cursor - 1, height - START_Y);
				g.drawLine(cursor + 1, 0, cursor + 1, height - START_Y);
			}

			Mtx44 mtx = axis != null ? axis.mtx : getTransformation(h);

			drawFloat(gx, mtx);

			gx.dispose();

			renderOverlayRight(g, 0, overlay);

			if(drawOverlay) {
				Vec3 mouse = getMouse(width, height);
				if(mouse != null) {
					String[] postext = {
							"Position: time=" + mouse.x + ", bpm=" + mouse.y
					};
					renderOverlay(g, startX + 2, 0, postext);
				}
			}
		}

		private int getPixel(long time) {
			return (int) Math.round((time * timeScale) + translation.x + START_X);
		}

		private void renderOverlay(Graphics g, int x, int y, String[] text) {
			int maxlen = 0;
			for(String line : text) {
				int len = line.length();
				if(len > maxlen) {
					maxlen = len;
				}
			}

			g.setColor(OVERLAY_BACKGROUND);
			g.fillRect(x, y, maxlen * ADM3AFont.WIDTH + 6, text.length * ADM3AFont.HEIGHT + 6);

			for(int i = 0; i < text.length; i++) {
				ADM3AFont.render(g, x + 3, y + 1 + (i + 1) * ADM3AFont.HEIGHT, TEXT_COLOR, TRANSPARENT,
						text[i]);
			}
		}

		private void renderOverlayRight(Graphics g, int y, String[] text) {
			int maxlen = 0;
			for(String line : text) {
				int len = line.length();
				if(len > maxlen) {
					maxlen = len;
				}
			}

			int x = getWidth() - (maxlen * ADM3AFont.WIDTH + 9);

			g.setColor(OVERLAY_BACKGROUND);
			g.fillRect(x, y, maxlen * ADM3AFont.WIDTH + 6, text.length * ADM3AFont.HEIGHT + 6);

			for(int i = 0; i < text.length; i++) {
				ADM3AFont.render(g, x + 3, y + 1 + (i + 1) * ADM3AFont.HEIGHT, TEXT_COLOR, TRANSPARENT,
						text[i]);
			}
		}

		private Vec3 getMouse(int width, int height) {
			return getMouse(width, height, mouseX, mouseY);
		}

		private Vec3 getMouse(int width, int height, int x, int y) {
			Axis axis = getAxis(width, height);
			double posX = (x - START_X - translation.x) / timeScale;
			double posY = (height - START_Y - y + translation.y) / scaleY;
			double range = axis.max - axis.min;
			double scale = range / (height - START_Y - PADDING);
			double py = posY * scale;
			posY = axis.min + py;
			return new Vec3(posX, posY, 0);
		}

		private Vec2 getRange(int width, int height) {
			Axis axis = getAxis(width, height);
			double h = height - START_Y - PADDING;
			double range = axis.max - axis.min;
			double pyMax = axis.min + range / scaleY + translation.y / scaleY * range / h;
			double pyMin = axis.min + translation.y / scaleY * range / h;
			return new Vec2(pyMin, pyMax);
		}

		private Axis getAxis(int width, int height) {
			if(cachedAxis == null || cachedWidth != width || cachedHeight != height) {
				cachedWidth = width;
				cachedHeight = height;
				cachedAxis = getAxisFloat(height, BPM_MIN, BPM_MAX);
			}
			return cachedAxis;
		}

		private Axis getAxisFloat(int height, double min, double max) {
			int h = height - START_Y - PADDING;
			Mtx44 transform = getTransformation(height - START_Y);

			double range = max - min;
			double yscale = h / range;
			Mtx44 mtx = transform.applyScale(1.0, yscale, 1.0).applyTrans(0, -min, 0);

			int maxlines = (int) Math.round(h / (2 * ADM3AFont.HEIGHT) * scaleY);
			int rounded = (maxlines / 5) * 5;
			if(rounded < 2) {
				rounded = 2;
			}

			return new Axis(mtx, min, max, rounded);
		}

		private void drawFloat(Graphics g, Mtx44 mtx) {
			NavigableMap<Long, Double> values = part.getTempoMap();

			List<Vec3> points = new ArrayList<>();

			g.setColor(LINE_COLOR);
			double last = values.firstEntry().getValue();
			for(Entry<Long, Double> entry : values.sequencedEntrySet()) {
				long time = entry.getKey();
				double value = entry.getValue();

				if(value != last) {
					points.add(new Vec3(time, last, 0.0));
					last = value;
				}
				points.add(new Vec3(time, value, 0.0));

				Vec3 point = mtx.mult(new Vec3(time, value, 0.0));
				int x = (int) Math.round(point.x);
				int y = (int) Math.round(point.y);
				int px = x - POINT_SIZE / 2;
				int py = y - POINT_SIZE / 2;
				g.fillOval(px, py, POINT_SIZE, POINT_SIZE);
			}

			int[] x = new int[points.size()];
			int[] y = new int[points.size()];
			for(int i = 0; i < points.size(); i++) {
				Vec3 point = mtx.mult(points.get(i));
				x[i] = (int) Math.round(point.x);
				y[i] = (int) Math.round(point.y);
			}

			g.drawPolyline(x, y, points.size());
		}

		private class MouseController extends MouseAdapter {
			private Vec3 startTranslate;
			private int startX;
			private int startY;
			private Vec3 startMouse;

			private double getValue(double val) {
				Axis axis = getAxis(getWidth(), getHeight());

				double value = val;
				if(value < axis.min) {
					value = axis.min;
				} else if(value > axis.max) {
					value = axis.max;
				}

				return value;
			}

			private void addPoint(Vec3 point) {
				Axis axis = getAxis(getWidth(), getHeight());
				if(axis == null) {
					return;
				}

				int time = (int) Math.round(point.x);
				if(time < 0) {
					time = 0;
				}

				double value = point.y;

				double next = getValue(value);
				setValue(time, next);
				selectedTime = time;

				updateFields();
			}

			private void removePoint(Vec3 point) {
				long t = Math.round(point.x);
				NavigableMap<Long, ?> values = part.getTempoMap();
				Long floor = values.floorKey(t);
				Long ceil = values.ceilingKey(t);
				if(floor == null && ceil != null) {
					if(values.size() > 1) {
						part.deleteTempo(ceil);
						selectedTime = values.ceilingKey(t);
						track.recompute();
						updateFields();
						repaint();
					}
				} else if(floor != null && ceil == null) {
					if(values.size() > 1) {
						part.deleteTempo(floor);
						selectedTime = values.floorKey(t);
						track.recompute();
						updateFields();
						repaint();
					}
				} else if(floor != null && ceil != null) {
					// which one is closer?
					long dstFloor = Math.abs(t - floor);
					long dstCeil = Math.abs(t - ceil);
					if(dstFloor < dstCeil) {
						part.deleteTempo(floor);
						selectedTime = ceil;
						track.recompute();
						updateFields();
						repaint();
					} else {
						part.deleteTempo(ceil);
						selectedTime = floor;
						track.recompute();
						updateFields();
						repaint();
					}
				}
			}

			private void selectPoint(Vec3 point) {
				Axis axis = getAxis(getWidth(), getHeight());
				if(axis == null) {
					return;
				}

				long time = Math.round(point.x);
				if(time < 0) {
					time = 0;
				}

				NavigableMap<Long, Double> values = part.getTempoMap();
				Long floor = values.floorKey(time);
				Long ceil = values.ceilingKey(time);
				if(floor == null && ceil != null) {
					time = ceil;
				} else if(floor != null && ceil == null) {
					time = floor;
				} else if(floor != null && ceil != null) {
					// which one is closer?
					long dstFloor = Math.abs(time - floor);
					long dstCeil = Math.abs(time - ceil);
					if(dstFloor < dstCeil) {
						time = floor;
					} else {
						time = ceil;
					}
				} else {
					assert floor == null && ceil == null;
					return;
				}

				selectedTime = time;
				updateFields();
			}

			private long getTime(Vec3 mouse) {
				long time = Math.round(mouse.x);
				NavigableMap<Long, ?> values = part.getTempoMap();
				Long floor = values.floorKey(time);
				Long ceil = values.ceilingKey(time);
				if(floor != null && ceil == null) {
					return floor;
				} else if(floor == null && ceil != null) {
					return ceil;
				} else if(floor == null && ceil == null) {
					throw new IllegalStateException("no tempo points available");
				} else {
					long dstFloor = Math.abs(time - floor);
					long dstCeil = Math.abs(time - ceil);
					if(dstFloor < dstCeil) {
						return floor;
					} else {
						return ceil;
					}
				}
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				mouseX = startX;
				mouseY = startY;
				Vec3 mouse = getMouse(getWidth(), getHeight());
				if(mouse == null) {
					return;
				}
				if(e.getButton() == MouseEvent.BUTTON1) {
					if((e.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) != 0) {
						addPoint(mouse);
					} else {
						selectPoint(mouse);
					}
				} else if(e.getButton() == MouseEvent.BUTTON3) {
					if((e.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) != 0) {
						removePoint(mouse);
					} else {
						JMenuItem reset = new JMenuItem("Reset");
						reset.setMnemonic('R');
						reset.addActionListener(ev -> {
							setTimeScale(defaultTimeScale);
							setDivision(defaultDivision);
							translation = new Vec3(0, 0, 0);
							scaleY = 1.0;
							iscaleY = 0;
							cachedAxis = null;
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

						JRadioButtonMenuItem div32 = new JRadioButtonMenuItem("Division: 32",
								division == 32);
						div32.setMnemonic('3');
						div32.addActionListener(ev -> setDivision(32));
						divisionGroup.add(div32);

						JPopupMenu menu = new JPopupMenu();
						menu.add(reset);
						menu.addSeparator();
						menu.add(div1);
						menu.add(div2);
						menu.add(div4);
						menu.add(div8);
						menu.add(div16);
						menu.add(div32);

						menu.show(e.getComponent(), e.getX(), e.getY());
					}
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {
				requestFocusInWindow(Cause.MOUSE_EVENT);

				startX = e.getX();
				startY = e.getY();
				startTranslate = translation;
				mouseX = startX;
				mouseY = startY;
				startMouse = getMouse(getWidth(), getHeight());
				if(startMouse == null) {
					return;
				}
				selectedTime = getTime(startMouse);

				if(e.getModifiersEx() == MouseEvent.BUTTON1_DOWN_MASK) {
					// only left button click
					selectPoint(startMouse);
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				// TODO Auto-generated method stub
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				mouseX = e.getX();
				mouseY = e.getY();

				if(e.getModifiersEx() == MouseEvent.BUTTON2_DOWN_MASK) {
					// middle button, no modifiers
					Vec3 start = new Vec3(startX, startY, 0);
					Vec3 end = new Vec3(e.getX(), e.getY(), 0);
					Vec3 diff = end.sub(start);
					translation = startTranslate.add(diff);
					double tx = translation.x;
					double ty = translation.y;
					if(tx > 0.0) {
						tx = 0.0;
					}
					if(iscaleY == 0 || ty < 0.0) {
						ty = 0.0;
					}

					translation = new Vec3(tx, ty, 0.0);
					cachedAxis = null;

					int width = getWidth();
					int height = getHeight();
					Axis axis = getAxis(width, height);
					if(axis != null) {
						double posY = (height - START_Y - PADDING + translation.y) / scaleY;
						double range = axis.max - axis.min;
						double scale = range / (height - START_Y - PADDING);
						double value = axis.min + posY * scale;
						if(value > axis.max) {
							// limit it
							ty = (height - START_Y - PADDING) * (scaleY - 1.0);
							translation = new Vec3(tx, ty, 0.0);
							cachedAxis = null;
						}
					}

					repaint();
				} else if(e.getModifiersEx() == MouseEvent.BUTTON1_DOWN_MASK) {
					// left button, no modifiers
					Vec3 mouse = getMouse(getWidth(), getHeight(), mouseX, mouseY);
					if(mouse == null) {
						return;
					}
					long startTime = selectedTime;
					long time = Math.round(mouse.x);

					NavigableMap<Long, ?> values = part.getTempoMap();

					// startTime is exactly on the start point
					Long lower = values.lowerKey(startTime);
					Long higher = values.higherKey(startTime);

					if(lower == null && higher != null) {
						// before first point
						if(time >= higher) {
							time = higher - 1;
						}
						if(time < 0) {
							time = 0;
						}
					} else if(lower != null && higher == null) {
						// after last point
						if(time <= lower) {
							time = lower + 1;
						}
					} else if(lower == null && higher == null) {
						if(values.size() == 1) {
							// only one point and we selected it
							if(time < 0) {
								time = 0;
							}
						} else {
							return;
						}
					} else {
						// point is between two other points
						if(time <= lower) {
							time = lower + 1;
						} else if(time >= higher) {
							time = higher - 1;
						}

						if(time >= higher || time <= lower) {
							// exactly two adjacent points; cannot move anything
							return;
						}
					}

					// always perform the update since the value might have changed
					setValue(time, getValue(mouse.y));
					if(time != selectedTime) {
						part.deleteTempo(selectedTime);
						selectedTime = time;
						track.recompute();
					}
					updateFields();
					repaint();
				}
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				mouseX = e.getX();
				mouseY = e.getY();
				repaint();
			}

			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				int notches = e.getWheelRotation();

				mouseX = e.getX();
				mouseY = e.getY();
				if((e.getModifiersEx() & MouseWheelEvent.CTRL_DOWN_MASK) != 0) {
					double posX = (mouseX - START_X - translation.x) / timeScale;

					double div = Math.pow(2.0, -notches);

					double factor = 384.0 / ppq;
					if(timeScale * div < factor * (1.0 / 64)) {
						return;
					} else if(timeScale * div > factor * 64) {
						return;
					}

					double tx = mouseX - START_X - posX * timeScale * div;
					if(tx > 0.0) {
						tx = 0.0;
					}

					translation = new Vec3(tx, translation.y, translation.z);
					cachedAxis = null;

					setTimeScale(timeScale * div);
				} else if((e.getModifiersEx() & MouseWheelEvent.SHIFT_DOWN_MASK) != 0) {
					// scroll X
					double ty = translation.x - notches * (grid / 2.0) * timeScale;
					if(ty > 0) {
						ty = 0;
					}
					translation = new Vec3(ty, translation.y, translation.z);
					cachedAxis = null;
					repaint();
				} else if(e.getModifiersEx() == 0) {
					double height = getHeight();
					double posY = (height - START_Y - mouseY + translation.y) / scaleY;

					iscaleY -= notches;
					if(iscaleY <= 0) {
						iscaleY = 0;
					}
					scaleY = Math.pow(2.0, iscaleY / 2.0);

					double ty = posY * scaleY - height + START_Y + mouseY;
					if(iscaleY <= 0) {
						ty = 0.0;
					}

					translation = new Vec3(translation.x, ty, translation.z);
					cachedAxis = null;
					repaint();
				}
			}
		}
	}
}
