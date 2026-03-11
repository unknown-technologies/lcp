package com.unknown.emulight.lcp.laser.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeListener;

import com.unknown.emulight.lcp.laser.node.Color3;
import com.unknown.emulight.lcp.laser.node.Property;
import com.unknown.math.g3d.Mtx44;
import com.unknown.math.g3d.Vec3;
import com.unknown.util.ui.ADM3AFont;

@SuppressWarnings("serial")
public class ClipPropertyAutomationEditor extends JDialog {
	private static final String TITLE = "Automation Editor";

	private static final int TEXT_PADDING = 5;
	private static final int PADDING = 5;
	private static final int START_X = 2 * PADDING + 5 * ADM3AFont.WIDTH + 1;
	private static final int START_Y = PADDING + ADM3AFont.HEIGHT;

	private Property<?> property;
	private Editor editor;
	private PropertyPanel propertyPanel;

	private Vec3 translation = new Vec3(0.0, 0.0, 0.0);
	private double iscaleX = 0;
	private double iscaleY = 0;
	private double scaleX = 1.0;
	private double scaleY = 1.0;

	private int mouseX = 0;
	private int mouseY = 0;

	private Axis cachedAxis = null;
	private int cachedWidth = 0;
	private int cachedHeight = 0;

	private int selectedTime = -1;
	private int currentChannel = 0;

	private final List<Updater> updater = new ArrayList<>();
	private boolean inCallback = false;
	private boolean bypassEvents = false;

	private final Callback update;

	private boolean drawOverlay = false;

	public ClipPropertyAutomationEditor(JFrame parent, Callback update) {
		super(parent, TITLE);
		this.update = update;

		editor = new Editor();
		propertyPanel = new PropertyPanel();

		setLayout(new BorderLayout());
		add(BorderLayout.NORTH, propertyPanel);
		add(BorderLayout.CENTER, editor);

		setSize(640, 480);
		setLocationRelativeTo(parent);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}

	public void setProperty(Property<?> property) {
		if(property != this.property) {
			// reference comparison is intentional
			iscaleX = 0;
			iscaleY = 0;
			scaleX = 1.0;
			scaleY = 1.0;
			translation = new Vec3(0.0, 0.0, 0.0);
		}

		this.property = property;
		cachedAxis = null;
		update();
		propertyPanel.change();
	}

	public void update() {
		if(property == null) {
			return;
		}

		setTitle(TITLE + ": " + property.getName());
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
		return Mtx44.scale(scaleX, -scaleY, 1.0).transApply(translation.x, translation.y + height,
				translation.z);
	}

	@SuppressWarnings("unchecked")
	protected <T> NavigableMap<Integer, T> getValues() {
		return (NavigableMap<Integer, T>) property.getValues();
	}

	@SuppressWarnings("unchecked")
	protected <T> T getMinimum() {
		return (T) property.getMinimum();
	}

	@SuppressWarnings("unchecked")
	protected <T> T getMaximum() {
		return (T) property.getMaximum();
	}

	protected <T> void setValue(int time, T value) {
		@SuppressWarnings("unchecked")
		Property<T> prop = (Property<T>) property;
		prop.setValue(time, value);
		updateFields();
		repaint();
	}

	protected <T> T getValue(int time) {
		@SuppressWarnings("unchecked")
		Property<T> prop = (Property<T>) property;
		return prop.getValue(time);
	}

	protected <T> T getInterpolatedValue(int time) {
		@SuppressWarnings("unchecked")
		Property<T> prop = (Property<T>) property;
		return prop.getInterpolatedValue(time);
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
		private JComboBox<String> channel;

		public PropertyPanel() {
			super(new FlowLayout(FlowLayout.LEFT));
		}

		public void change() {
			removeAll();
			channel = null;
			updater.clear();
			if(property == null) {
				revalidate();
				repaint();
				return;
			}

			if(property.getType().equals(String.class)) {
			} else if(property.getType().equals(Boolean.class)) {
			} else if(property.getType().equals(Integer.class)) {
			} else if(property.getType().equals(Double.class)) {
				add(new JLabel("Value:"));
				JSpinner number = new JSpinner(new SpinnerNumberModel(
						(double) property.getValue(selectedTime),
						(double) property.getMinimum(), (double) property.getMaximum(), 0.1));
				number.addChangeListener(e -> {
					if(!bypassEvents) {
						setValue(selectedTime, (double) number.getValue());
						updateView();
					}
				});
				updater.add(() -> {
					try {
						bypassEvents = true;
						number.setValue(property.getValue(selectedTime));
					} finally {
						bypassEvents = false;
					}
				});
				add(number);
			} else if(property.getType().equals(Vec3.class)) {
				add(new JLabel("Channel:"));
				channel = new JComboBox<>(new String[] { "X", "Y", "Z" });
				channel.addItemListener(e -> {
					int idx = channel.getSelectedIndex();
					if(idx != -1) {
						currentChannel = idx;
					} else {
						currentChannel = 0;
					}
				});
				currentChannel = 0;
				add(channel);

				Vec3 vec = getValue(selectedTime);
				Vec3 min = getMinimum();
				Vec3 max = getMaximum();
				JSpinner spinnerX = new JSpinner(new SpinnerNumberModel(vec.x, min.x, max.x, 0.1));
				JSpinner spinnerY = new JSpinner(new SpinnerNumberModel(vec.y, min.y, max.y, 0.1));
				JSpinner spinnerZ = new JSpinner(new SpinnerNumberModel(vec.z, min.z, max.z, 0.1));

				ChangeListener listener = e -> {
					if(!bypassEvents) {
						double x = (double) spinnerX.getValue();
						double y = (double) spinnerY.getValue();
						double z = (double) spinnerZ.getValue();
						setValue(selectedTime, new Vec3(x, y, z));
						updateView();
					}
				};
				spinnerX.addChangeListener(listener);
				spinnerY.addChangeListener(listener);
				spinnerZ.addChangeListener(listener);

				updater.add(() -> {
					try {
						bypassEvents = true;
						Vec3 v = getValue(selectedTime);
						spinnerX.setValue(v.x);
						spinnerY.setValue(v.y);
						spinnerZ.setValue(v.z);
					} finally {
						bypassEvents = false;
					}
				});

				add(new JLabel("X:"));
				add(spinnerX);
				add(new JLabel("Y:"));
				add(spinnerY);
				add(new JLabel("Z:"));
				add(spinnerZ);
			} else if(property.getType().equals(Color3.class)) {
				add(new JLabel("Channel:"));
				channel = new JComboBox<>(new String[] { "Red", "Green", "Blue" });
				channel.addItemListener(e -> {
					int idx = channel.getSelectedIndex();
					if(idx != -1) {
						currentChannel = idx;
					} else {
						currentChannel = 0;
					}
				});
				currentChannel = 0;
				add(channel);

				Color3 col = getValue(selectedTime);
				JSpinner spinnerR = new JSpinner(new SpinnerNumberModel(col.getRed(), 0.0, 1.0, 0.1));
				JSpinner spinnerG = new JSpinner(new SpinnerNumberModel(col.getGreen(), 0.0, 1.0, 0.1));
				JSpinner spinnerB = new JSpinner(new SpinnerNumberModel(col.getBlue(), 0.0, 1.0, 0.1));

				ChangeListener listener = e -> {
					if(!bypassEvents) {
						double r = (double) spinnerR.getValue();
						double g = (double) spinnerG.getValue();
						double b = (double) spinnerB.getValue();
						setValue(selectedTime, new Color3(r, g, b));
						updateView();
					}
				};
				spinnerR.addChangeListener(listener);
				spinnerG.addChangeListener(listener);
				spinnerB.addChangeListener(listener);

				updater.add(() -> {
					try {
						bypassEvents = true;
						Color3 c = getValue(selectedTime);
						spinnerR.setValue(c.getRed());
						spinnerG.setValue(c.getGreen());
						spinnerB.setValue(c.getBlue());
					} finally {
						bypassEvents = false;
					}
				});

				add(new JLabel("R:"));
				add(spinnerR);
				add(new JLabel("G:"));
				add(spinnerG);
				add(new JLabel("B:"));
				add(spinnerB);
			}

			revalidate();
			repaint();
		}

		public void setChannel(int ch) {
			if(channel != null) {
				channel.setSelectedIndex(ch);
			}
		}
	}

	private class Editor extends JComponent {
		private static final Color TRANSPARENT = new Color(0, true);
		private static final Color OVERLAY_BACKGROUND = new Color(0, 0, 0, 192);
		private static final Color TEXT_COLOR = Color.CYAN;

		private static final Color GRID_COLOR = Color.DARK_GRAY;
		private static final Color GRID_BORDER = Color.CYAN;

		private static final Color LINE_COLOR = Color.LIGHT_GRAY;

		private static final Color LINE_COLOR_X = Color.RED;
		private static final Color LINE_COLOR_Y = Color.GREEN;
		private static final Color LINE_COLOR_Z = Color.BLUE;

		private static final int POINT_SIZE = 4;

		public Editor() {
			setBackground(Color.BLACK);
			setOpaque(true);
			setDoubleBuffered(true);
			MouseController mouse = new MouseController();
			addMouseListener(mouse);
			addMouseMotionListener(mouse);
			addMouseWheelListener(mouse);
		}

		@Override
		protected void paintComponent(Graphics g) {
			int width = getWidth();
			int height = getHeight();

			g.setColor(getBackground());
			g.fillRect(0, 0, width, height);

			if(property == null) {
				ADM3AFont.render(g, TEXT_PADDING, TEXT_PADDING + ADM3AFont.HEIGHT, TEXT_COLOR,
						TRANSPARENT, "No parameter selected");
				return;
			}

			String[] text = new String[] {
					"Parameter: " + property.getName(),
					"Enabled: " + (property.isAutomation() ? "yes" : "no"),
					"Points: " + property.getCount(),
					"Zoom: X=" + scaleX + ", Y=" + scaleY
			};

			int startX = START_X - 1;
			int startY = START_Y - 1;

			// render grid
			g.setColor(GRID_BORDER);
			g.drawLine(startX, height - startY, width, height - startY);
			g.drawLine(startX, height - startY, startX, 0);

			startX++;
			startY++;

			// render time axis
			g.setColor(GRID_COLOR);
			int sx = (int) Math.round(-translation.x / scaleX);
			int interval = 100;
			if(sx % interval > 0) {
				sx -= sx % interval;
			}
			double gridOffX = translation.x % (interval * scaleX);
			int maxT = (int) Math.round(width / scaleX);
			for(int i = 0; i < maxT; i += interval) {
				int t = sx + i;
				double xoff = gridOffX + i * scaleX;
				int x = (int) Math.round(xoff);
				if(x < 0) {
					continue;
				} else if(x > width) {
					break;
				}
				g.drawLine(startX + x, 0, startX + x, height - startY);
				ADM3AFont.render(g, startX + x, height - startY + ADM3AFont.HEIGHT + 2, TEXT_COLOR,
						TRANSPARENT, Integer.toString(t));
			}

			int h = height - startY;
			Graphics gx = g.create(startX, 0, width - startX, h + 1);

			Axis axis = getAxis(width, height);

			if(axis != null) {
				Mtx44 mtx = axis.mtx;

				gx.setColor(GRID_COLOR);

				// render Y axis
				for(int i = 0; i <= axis.divisions; i++) {
					double value = axis.min + (axis.max - axis.min) / axis.divisions * i;
					Vec3 p = mtx.mult(new Vec3(0, value, 0));
					int py = (int) Math.round(p.y);
					gx.drawLine(0, py, width, py);
				}

				// render Y axis labels
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
						format = "% 4.0f";
					} else if(len == 2) {
						format = "% 4.1f";
					} else {
						format = "% 4.2f";
					}
				} else {
					if(len > 3) {
						format = "%5.0f";
					} else if(len == 3) {
						format = "%5.1f";
					} else if(len == 2) {
						format = "%5.2f";
					} else {
						format = "%5.3f";
					}
				}
				for(int i = 0; i <= axis.divisions; i++) {
					double value = axis.min + (axis.max - axis.min) / axis.divisions * i;
					Vec3 p = mtx.mult(new Vec3(0, value, 0));
					int py = (int) Math.round(p.y);
					String s = String.format(format, value);
					ADM3AFont.render(g, PADDING, py + ADM3AFont.HEIGHT / 2, TEXT_COLOR, TRANSPARENT,
							s);
				}
			}

			Mtx44 mtx = axis != null ? axis.mtx : getTransformation(h);

			Class<?> type = property.getType();
			if(type.equals(Boolean.class)) {
				drawBoolean(gx, mtx);
			} else if(type.equals(Double.class)) {
				drawFloat(gx, mtx);
			} else if(type.equals(Vec3.class)) {
				drawVec3(gx, mtx);
			} else if(type.equals(Color3.class)) {
				drawColor3(gx, mtx);
			}

			gx.dispose();

			renderOverlayRight(g, 0, text);

			if(drawOverlay) {
				Vec3 mouse = getMouse(width, height);
				if(mouse != null) {
					String[] postext = {
							"Position: X=" + mouse.x + ", Y=" + mouse.y
					};
					renderOverlay(g, startX + 2, 0, postext);
				}
			}
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
			if(axis == null) {
				return null;
			}
			double posX = (x - START_X - translation.x) / scaleX;
			double posY = (height - START_Y - y + translation.y) / scaleY;
			double range = axis.max - axis.min;
			double scale = range / (height - START_Y - PADDING);
			double py = posY * scale;
			posY = axis.min + py;
			return new Vec3(posX, posY, 0);
		}

		private Axis getAxis(int width, int height) {
			if(cachedAxis == null || cachedWidth != width || cachedHeight != height) {
				cachedWidth = width;
				cachedHeight = height;
				cachedAxis = getAxisInternal(height);
			}
			return cachedAxis;
		}

		private Axis getAxisInternal(int height) {
			Class<?> type = property.getType();
			if(type.equals(Boolean.class)) {
				return getAxisBoolean(height);
			} else if(type.equals(Double.class)) {
				return getAxisFloat(height);
			} else if(type.equals(Vec3.class)) {
				return getAxisVec3(height);
			} else if(type.equals(Color3.class)) {
				return getAxisColor3(height);
			} else {
				return null;
			}
		}

		private Axis getAxisBoolean(int height) {
			int h = height - START_Y - PADDING;
			Mtx44 transform = getTransformation(height - START_Y);
			Mtx44 mtx = transform.applyScale(1.0, h, 1.0).applyTrans(0, 0, 0);
			return new Axis(mtx, 0, 1, 1);
		}

		private Axis getAxisFloat(int height, double min, double max) {
			int h = height - START_Y - PADDING;
			Mtx44 transform = getTransformation(height - START_Y);

			double range = max - min;
			double yscale = h / range;
			Mtx44 mtx = transform.applyScale(1.0, yscale, 1.0).applyTrans(0, -min, 0);

			int maxlines = h / (2 * ADM3AFont.HEIGHT);
			int rounded = (maxlines / 5) * 5;
			if(rounded < 2) {
				rounded = 2;
			}

			return new Axis(mtx, min, max, rounded);
		}

		private Axis getAxisFloat(int height) {
			double min = getMinimum();
			double max = getMaximum();
			return getAxisFloat(height, min, max);
		}

		private Axis getAxisVec3(int height) {
			Vec3 vmin = getMinimum();
			Vec3 vmax = getMaximum();
			double min = Math.min(Math.min(vmin.x, vmin.y), vmin.z);
			double max = Math.max(Math.max(vmax.x, vmax.y), vmax.z);
			return getAxisFloat(height, min, max);
		}

		private Axis getAxisColor3(int height) {
			return getAxisFloat(height, 0.0, 1.0);
		}

		private void drawBoolean(Graphics g, Mtx44 mtx) {
			g.setColor(LINE_COLOR);
			NavigableMap<Integer, Boolean> values = getValues();

			List<Vec3> points = new ArrayList<>();

			g.setColor(LINE_COLOR);
			boolean last = values.firstEntry().getValue();
			for(Entry<Integer, Boolean> entry : values.sequencedEntrySet()) {
				int time = entry.getKey();
				boolean value = entry.getValue();

				if(value != last) {
					points.add(new Vec3(time, last ? 1.0 : 0.0, 0.0));
					last = value;
				}
				points.add(new Vec3(time, value ? 1.0 : 0.0, 0.0));

				Vec3 point = mtx.mult(new Vec3(time, value ? 1.0 : 0.0, 0.0));
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

		private void drawFloat(Graphics g, Mtx44 mtx) {
			NavigableMap<Integer, Double> values = getValues();

			List<Vec3> points = new ArrayList<>();

			for(Entry<Integer, Double> entry : values.sequencedEntrySet()) {
				int time = entry.getKey();
				double value = entry.getValue();

				points.add(new Vec3(time, value, 0));
			}

			g.setColor(LINE_COLOR);
			int[] x = new int[points.size()];
			int[] y = new int[points.size()];
			for(int i = 0; i < points.size(); i++) {
				Vec3 point = mtx.mult(points.get(i));
				x[i] = (int) Math.round(point.x);
				y[i] = (int) Math.round(point.y);

				int px = x[i] - POINT_SIZE / 2;
				int py = y[i] - POINT_SIZE / 2;
				g.fillOval(px, py, POINT_SIZE, POINT_SIZE);
			}

			g.drawPolyline(x, y, points.size());
		}

		private void drawVec3(Graphics g, Mtx44 mtx) {
			NavigableMap<Integer, Vec3> values = getValues();

			int[] t = new int[values.size()];
			int[] x = new int[values.size()];
			int[] y = new int[values.size()];
			int[] z = new int[values.size()];

			int i = 0;
			for(Entry<Integer, Vec3> entry : values.sequencedEntrySet()) {
				int time = entry.getKey();
				Vec3 value = entry.getValue();

				Vec3 pointX = mtx.mult(new Vec3(time, value.x, 0));
				Vec3 pointY = mtx.mult(new Vec3(time, value.y, 0));
				Vec3 pointZ = mtx.mult(new Vec3(time, value.z, 0));

				t[i] = (int) Math.round(pointX.x);
				x[i] = (int) Math.round(pointX.y);
				y[i] = (int) Math.round(pointY.y);
				z[i] = (int) Math.round(pointZ.y);

				int pt = t[i] - POINT_SIZE / 2;
				int px = x[i] - POINT_SIZE / 2;
				int py = y[i] - POINT_SIZE / 2;
				int pz = z[i] - POINT_SIZE / 2;
				g.setColor(LINE_COLOR_X);
				g.fillOval(pt, px, POINT_SIZE, POINT_SIZE);
				g.setColor(LINE_COLOR_Y);
				g.fillOval(pt, py, POINT_SIZE, POINT_SIZE);
				g.setColor(LINE_COLOR_Z);
				g.fillOval(pt, pz, POINT_SIZE, POINT_SIZE);

				i++;
			}

			g.setColor(LINE_COLOR_X);
			g.drawPolyline(t, x, t.length);
			g.setColor(LINE_COLOR_Y);
			g.drawPolyline(t, y, t.length);
			g.setColor(LINE_COLOR_Z);
			g.drawPolyline(t, z, t.length);
		}

		private void drawColor3(Graphics g, Mtx44 mtx) {
			drawVec3(g, mtx);
		}

		private class MouseController extends MouseAdapter {
			private Vec3 startTranslate;
			private int startX;
			private int startY;
			private Vec3 startMouse;

			@SuppressWarnings("unchecked")
			private <T> T getValue(double val, T old) {
				Axis axis = getAxis(getWidth(), getHeight());
				if(axis == null) {
					return null;
				}

				double value = val;
				if(property.getType().equals(Boolean.class)) {
					return (T) (Boolean) (value >= 0.5);
				} else if(property.getType().equals(Double.class)) {
					if(value < axis.min) {
						value = axis.min;
					} else if(value > axis.max) {
						value = axis.max;
					}

					return (T) (Double) value;
				} else if(property.getType().equals(Vec3.class)) {
					if(value < axis.min) {
						value = axis.min;
					} else if(value > axis.max) {
						value = axis.max;
					}

					Vec3 vec = (Vec3) old;
					Vec3 next = vec;
					switch(currentChannel) {
					case 0:
						next = new Vec3(value, vec.y, vec.z);
						break;
					case 1:
						next = new Vec3(vec.x, value, vec.z);
						break;
					case 2:
						next = new Vec3(vec.x, vec.y, value);
						break;
					default:
						throw new AssertionError("invalid current channel: " + currentChannel);
					}

					return (T) next;
				} else if(property.getType().equals(Color3.class)) {
					if(value < 0.0) {
						value = 0.0;
					} else if(value > 1.0) {
						value = 1.0;
					}

					Color3 color = (Color3) old;
					Color3 next = color;
					switch(currentChannel) {
					case 0:
						next = new Color3(value, color.getGreen(), color.getBlue());
						break;
					case 1:
						next = new Color3(color.getRed(), value, color.getBlue());
						break;
					case 2:
						next = new Color3(color.getRed(), color.getGreen(), value);
						break;
					default:
						throw new AssertionError("invalid current channel: " + currentChannel);
					}

					return (T) next;
				}

				return null;
			}

			private <T> void addPoint(Vec3 point) {
				Axis axis = getAxis(getWidth(), getHeight());
				if(axis == null) {
					return;
				}

				int time = (int) Math.round(point.x);
				if(time < 0) {
					time = 0;
				}

				double value = point.y;

				T old = getInterpolatedValue(time);
				T next = getValue(value, old);
				if(next != null) {
					setValue(time, next);
					selectedTime = time;

					updateFields();
				}
			}

			private void removePoint(Vec3 point) {
				int t = (int) Math.round(point.x);
				NavigableMap<Integer, ?> values = getValues();
				Integer floor = values.floorKey(t);
				Integer ceil = values.ceilingKey(t);
				if(floor == null && ceil != null) {
					if(values.size() > 1) {
						property.unsetValue(ceil);
						selectedTime = values.ceilingKey(t);
						updateFields();
						repaint();
					}
				} else if(floor != null && ceil == null) {
					if(values.size() > 1) {
						property.unsetValue(floor);
						selectedTime = values.floorKey(t);
						updateFields();
						repaint();
					}
				} else if(floor != null && ceil != null) {
					// which one is closer?
					int dstFloor = Math.abs(t - floor);
					int dstCeil = Math.abs(t - ceil);
					if(dstFloor < dstCeil) {
						property.unsetValue(floor);
						selectedTime = ceil;
						updateFields();
						repaint();
					} else {
						property.unsetValue(ceil);
						selectedTime = floor;
						updateFields();
						repaint();
					}
				}
			}

			private <T> void selectPoint(Vec3 point) {
				Axis axis = getAxis(getWidth(), getHeight());
				if(axis == null) {
					return;
				}

				int time = (int) Math.round(point.x);
				double value = point.y;
				if(time < 0) {
					time = 0;
				}

				NavigableMap<Integer, T> values = getValues();
				Integer floor = values.floorKey(time);
				Integer ceil = values.ceilingKey(time);
				if(floor == null && ceil != null) {
					time = ceil;
				} else if(floor != null && ceil == null) {
					time = floor;
				} else if(floor != null && ceil != null) {
					// which one is closer?
					int dstFloor = Math.abs(time - floor);
					int dstCeil = Math.abs(time - ceil);
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

				if(property.getType().equals(Vec3.class)) {
					Vec3 vec = (Vec3) values.get(time);
					double dstX = Math.abs(vec.x - value);
					double dstY = Math.abs(vec.y - value);
					double dstZ = Math.abs(vec.z - value);
					if(dstX <= dstY && dstX <= dstZ) {
						propertyPanel.setChannel(0);
					} else if(dstY <= dstX && dstY <= dstZ) {
						propertyPanel.setChannel(1);
					} else if(dstZ <= dstX && dstZ <= dstY) {
						propertyPanel.setChannel(2);
					}
				} else if(property.getType().equals(Color3.class)) {
					Color3 color = (Color3) values.get(time);
					double dstR = Math.abs(color.getRed() - value);
					double dstG = Math.abs(color.getGreen() - value);
					double dstB = Math.abs(color.getBlue() - value);
					if(dstR <= dstG && dstR <= dstB) {
						propertyPanel.setChannel(0);
					} else if(dstG <= dstR && dstG <= dstB) {
						propertyPanel.setChannel(1);
					} else if(dstB <= dstR && dstB <= dstG) {
						propertyPanel.setChannel(2);
					}
				}
			}

			private int getTime(Vec3 mouse) {
				int time = (int) Math.round(mouse.x);
				Integer floor = getValues().floorKey(time);
				Integer ceil = getValues().ceilingKey(time);
				if(floor != null && ceil == null) {
					return floor;
				} else if(floor == null && ceil != null) {
					return ceil;
				} else if(floor == null && ceil == null) {
					throw new IllegalStateException("no automation points available");
				} else {
					int dstFloor = Math.abs(time - floor);
					int dstCeil = Math.abs(time - ceil);
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
					removePoint(mouse);
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {
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
					int startTime = selectedTime;
					int time = (int) Math.round(mouse.x);

					NavigableMap<Integer, ?> values = property.getValues();

					// startTime is exactly on the start point
					Integer lower = values.lowerKey(startTime);
					Integer higher = values.higherKey(startTime);

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
					setValue(time, getValue(mouse.y,
							ClipPropertyAutomationEditor.this.getValue(selectedTime)));
					if(time != selectedTime) {
						property.unsetValue(selectedTime);
						selectedTime = time;
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
					double posX = (mouseX - START_X - translation.x) / scaleX;

					iscaleX -= notches;
					scaleX = Math.pow(2.0, iscaleX / 2.0);

					double tx = mouseX - START_X - posX * scaleX;
					if(tx > 0.0) {
						tx = 0.0;
					}

					translation = new Vec3(tx, translation.y, translation.z);
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
