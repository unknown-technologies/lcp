package com.unknown.emulight.lcp.ui.laser;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JComponent;

import com.unknown.emulight.lcp.laser.Point3D;
import com.unknown.emulight.lcp.laser.node.CircleNode;
import com.unknown.emulight.lcp.laser.node.Color3;
import com.unknown.emulight.lcp.laser.node.GroupNode;
import com.unknown.emulight.lcp.laser.node.LineNode;
import com.unknown.emulight.lcp.laser.node.Node;
import com.unknown.emulight.lcp.laser.node.PointNode;
import com.unknown.emulight.lcp.laser.node.Property;
import com.unknown.emulight.lcp.laser.node.Shape;
import com.unknown.emulight.lcp.laser.node.StandardPropertyNames;
import com.unknown.emulight.lcp.laser.node.fx.MaskNode;
import com.unknown.math.g3d.Mtx44;
import com.unknown.math.g3d.Vec3;

@SuppressWarnings("serial")
public class ClipNodeEditor extends JComponent {
	private static final int POINT_RADIUS = 4;
	private static final int CROSSHAIR = 3;
	private static final Color SELECTION_COLOR = Color.GREEN;

	private final Callback updated;
	private final ClipTreeEditor treeEditor;

	private Node node;

	private int time;

	public ClipNodeEditor(Callback updated, ClipTreeEditor treeEditor) {
		this.updated = updated;
		this.treeEditor = treeEditor;

		setBackground(Color.BLACK);
		setOpaque(true);
		setDoubleBuffered(true);

		MouseController controller = new MouseController();
		addMouseListener(controller);
		addMouseMotionListener(controller);
	}

	public void setNode(Node node) {
		this.node = node;
		repaint();
	}

	public Node getNode() {
		return node;
	}

	public void setTime(int time) {
		this.time = time;
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		if(node == null) {
			return;
		}

		Node root = node.getRootNode();

		g.setColor(getBackground());
		g.fillRect(0, 0, getWidth(), getHeight());

		Mtx44 mtx = getTransform();
		Vec3[] vertices = { new Vec3(-1, -1, 0), new Vec3(1, -1, 0), new Vec3(1, 1, 0), new Vec3(-1, 1, 0) };
		int[] boundX = new int[vertices.length];
		int[] boundY = new int[vertices.length];
		for(int i = 0; i < vertices.length; i++) {
			Vec3 p = mtx.mult(vertices[i]);
			boundX[i] = (int) Math.round(p.x);
			boundY[i] = (int) Math.round(p.y);
		}
		g.setColor(Color.GREEN);
		g.drawPolygon(boundX, boundY, vertices.length);

		render(g, root, mtx, new Mtx44(), root == node, true);
	}

	private void render(Graphics g, Node n, Mtx44 posMtx, Mtx44 colMtx, boolean selected, boolean enabled) {
		Mtx44 pos = posMtx.concat(n.getTransformation(time));
		Mtx44 col = colMtx.concat(n.getColorTransformation(time));

		boolean sel = selected || n == node;
		boolean en = enabled && n.isEnabled(time);
		if(sel) {
			Vec3 zero = pos.mult(new Vec3(0, 0, 0));
			int zx = (int) Math.round(zero.x);
			int zy = (int) Math.round(zero.y);

			g.setColor(Color.GRAY);
			g.drawLine(zx - CROSSHAIR, zy, zx + CROSSHAIR, zy);
			g.drawLine(zx, zy - CROSSHAIR, zx, zy + CROSSHAIR);
		}
		if(n instanceof PointNode) {
			drawPoint(g, (PointNode) n, pos, col, sel, en);
		} else if(n instanceof LineNode) {
			drawLine(g, (LineNode) n, pos, col, sel, en);
		} else if(n instanceof CircleNode) {
			drawCircle(g, (CircleNode) n, pos, col, sel, en);
		} else if(n instanceof MaskNode) {
			drawMask(g, (MaskNode) n, pos, sel, en);
			for(Node no : n.getChildren()) {
				render(g, no, pos, col, sel, en);
			}
		} else if(n instanceof GroupNode) {
			for(Node no : n.getChildren()) {
				render(g, no, pos, col, sel, en);
			}
		} else {
			// generic non-editable node
			drawNode(g, n, posMtx, colMtx, sel, en);
		}
	}

	private Mtx44 getTransform() {
		int width = getWidth();
		int height = getHeight();
		int max = width > height ? height : width;
		max -= 20;
		double scale = max / 2.0;
		double translateX = width / 2.0;
		double translateY = height / 2.0;

		return Mtx44.scale(scale, scale, 1.0).transApply(translateX, translateY, 0);
	}

	private Mtx44 getInverseTransform() {
		return getInverseTransform(node.getRootNode());
	}

	private Mtx44 getInverseTransform(Node no) {
		int width = getWidth();
		int height = getHeight();
		int max = width > height ? height : width;
		max -= 20;
		double scale = max / 2.0;
		double translateX = width / 2.0;
		double translateY = height / 2.0;

		Mtx44 mtx = Mtx44.scale(1.0 / scale, 1.0 / scale, 1.0).applyTrans(-translateX, -translateY, 0);
		for(Node n = no; n != null; n = n.getParent()) {
			mtx = n.getInverseTransformation(time).concat(mtx);
		}
		return mtx;
	}

	private void drawPoint(Graphics g, PointNode point, Mtx44 mtx, Mtx44 colorMtx, boolean selected,
			boolean enabled) {
		Vec3 pos = mtx.mult(point.getPosition(time));
		int x = (int) Math.round(pos.x);
		int y = (int) Math.round(pos.y);

		boolean en = enabled && point.isEnabled(time);

		boolean black = false;
		if(selected) {
			g.setColor(SELECTION_COLOR);
		} else if(!en) {
			g.setColor(Color.DARK_GRAY);
		} else {
			Color3 color = point.getColor(time);
			Vec3 result = colorMtx.mult(color);
			black = (new Color3(color).getColor().getRGB() & 0xFFFFFF) == 0;
			g.setColor(new Color((float) result.x, (float) result.y, (float) result.z));
		}
		if(en && !black) {
			g.fillOval(x - POINT_RADIUS, y - POINT_RADIUS, 2 * POINT_RADIUS, 2 * POINT_RADIUS);
		} else {
			g.drawOval(x - POINT_RADIUS, y - POINT_RADIUS, 2 * POINT_RADIUS, 2 * POINT_RADIUS);
		}
	}

	private void drawLine(Graphics g, LineNode line, Mtx44 mtx, Mtx44 colorMtx, boolean selected, boolean enabled) {
		Vec3 pos1 = mtx.mult(line.getStart(time));
		Vec3 pos2 = mtx.mult(line.getEnd(time));
		int x1 = (int) Math.round(pos1.x);
		int y1 = (int) Math.round(pos1.y);
		int x2 = (int) Math.round(pos2.x);
		int y2 = (int) Math.round(pos2.y);

		boolean en = enabled && line.isEnabled(time);

		if(selected) {
			g.setColor(SELECTION_COLOR);
		} else if(!en) {
			g.setColor(Color.DARK_GRAY);
		} else {
			Color3 color = line.getColor(time);
			Vec3 result = colorMtx.mult(color);
			g.setColor(new Color((float) result.x, (float) result.y, (float) result.z));
		}

		g.drawLine(x1, y1, x2, y2);

		int count = line.getPointCount(time);

		if(count <= 2) {
			if(en) {
				g.fillOval(x1 - POINT_RADIUS, y1 - POINT_RADIUS, 2 * POINT_RADIUS, 2 * POINT_RADIUS);
				g.fillOval(x2 - POINT_RADIUS, y2 - POINT_RADIUS, 2 * POINT_RADIUS, 2 * POINT_RADIUS);
			} else {
				g.drawOval(x1 - POINT_RADIUS, y1 - POINT_RADIUS, 2 * POINT_RADIUS, 2 * POINT_RADIUS);
				g.drawOval(x2 - POINT_RADIUS, y2 - POINT_RADIUS, 2 * POINT_RADIUS, 2 * POINT_RADIUS);
			}
		} else {
			Vec3 delta = pos2.sub(pos1);

			Vec3 step = delta.scale(1.0 / (count - 1.0));

			for(int i = 0; i < count; i++) {
				Vec3 pos = pos1.add(step.scale(i));
				int x = (int) Math.round(pos.x);
				int y = (int) Math.round(pos.y);
				if(en) {
					g.fillOval(x - POINT_RADIUS, y - POINT_RADIUS, 2 * POINT_RADIUS,
							2 * POINT_RADIUS);
				} else {
					g.drawOval(x - POINT_RADIUS, y - POINT_RADIUS, 2 * POINT_RADIUS,
							2 * POINT_RADIUS);
				}
			}
		}
	}

	private void drawCircle(Graphics g, CircleNode circle, Mtx44 mtx, Mtx44 colorMtx, boolean selected,
			boolean enabled) {
		int cnt = circle.getPointCount(time) + 1;
		int max = cnt - 1;
		if(cnt < 2) {
			return;
		}

		Vec3 pos = circle.getPosition(time);
		double r = circle.getRadius(time);
		int[] px = new int[cnt];
		int[] py = new int[cnt];
		for(int i = 0; i < cnt; i++) {
			double phi = i / (double) max;
			double x = Math.cos(phi * 2.0 * Math.PI) * r;
			double y = Math.sin(phi * 2.0 * Math.PI) * r;

			Vec3 p = mtx.mult(new Vec3(x + pos.x, y + pos.y, 0));
			px[i] = (int) Math.round(p.x);
			py[i] = (int) Math.round(p.y);
		}

		boolean en = enabled && circle.isEnabled(time);

		if(selected) {
			g.setColor(SELECTION_COLOR);
		} else if(!en) {
			g.setColor(Color.DARK_GRAY);
		} else {
			Color3 color = circle.getColor(time);
			Vec3 result = colorMtx.mult(color);
			g.setColor(new Color((float) result.x, (float) result.y, (float) result.z));
		}

		g.drawPolyline(px, py, cnt);
	}

	private void drawMask(Graphics g, MaskNode mask, Mtx44 mtx, boolean selected, boolean enabled) {
		double r = mask.getRadius(time);

		int cnt = 50;
		int max = cnt - 1;
		int[] px = new int[cnt];
		int[] py = new int[cnt];
		for(int i = 0; i < cnt; i++) {
			double phi = i / (double) max;
			double x = Math.cos(phi * 2.0 * Math.PI) * r;
			double y = Math.sin(phi * 2.0 * Math.PI) * r;

			Vec3 p = mtx.mult(new Vec3(x, y, 0));
			px[i] = (int) Math.round(p.x);
			py[i] = (int) Math.round(p.y);
		}

		boolean en = enabled && mask.isEnabled(time);

		if(selected) {
			g.setColor(SELECTION_COLOR);
		} else if(!en) {
			g.setColor(Color.DARK_GRAY);
		} else {
			g.setColor(Color.GRAY);
		}

		g.drawPolyline(px, py, cnt);
	}

	private void drawNode(Graphics g, Node n, Mtx44 mtx, Mtx44 colorMtx, boolean selected, boolean enabled) {
		List<Point3D> points = n.render(time, mtx, colorMtx);
		Vec3 lastPos = null;

		boolean en = enabled && n.isEnabled(time);

		for(Point3D point : points) {
			Vec3 pos = point.getPosition();
			Color color = new Color3(point.getColor()).getColor();

			if(lastPos != null) {
				if(!en || (color.getRGB() & 0xFFFFFF) == 0) {
					// laser is off, draw it as dark gray
					g.setColor(Color.DARK_GRAY);
				} else {
					g.setColor(color);
				}
				int lastX = (int) Math.round(lastPos.x);
				int lastY = (int) Math.round(lastPos.y);
				int x = (int) Math.round(pos.x);
				int y = (int) Math.round(pos.y);
				g.drawLine(lastX, lastY, x, y);
			}

			lastPos = pos;
		}

		// dark to bright
		points.sort((a, b) -> Double.compare(a.getColor().length(), b.getColor().length()));

		for(Point3D point : points) {
			Vec3 pos = point.getPosition();

			int x = (int) Math.round(pos.x);
			int y = (int) Math.round(pos.y);

			Color color = new Color3(point.getColor()).getColor();
			boolean outline = !en || (color.getRGB() & 0xFFFFFF) == 0;
			if(selected) {
				g.setColor(SELECTION_COLOR);
			} else if(outline) {
				g.setColor(Color.DARK_GRAY);
			} else {
				g.setColor(color);
			}

			if(outline) {
				g.drawOval(x - POINT_RADIUS, y - POINT_RADIUS, 2 * POINT_RADIUS, 2 * POINT_RADIUS);
			} else {
				g.fillOval(x - POINT_RADIUS, y - POINT_RADIUS, 2 * POINT_RADIUS, 2 * POINT_RADIUS);
			}
		}
	}

	private Node getNode(Vec3 point) {
		Node root = node.getRootNode();
		List<Shape> shapes = root.getShapes(time);

		Shape closest = null;
		double closestDistance = Double.MAX_VALUE;
		for(Shape shape : shapes) {
			for(Point3D p : shape.getPoints()) {
				Vec3 pos = p.getPosition();
				double dst = point.sub(pos).length();
				if(dst < closestDistance) {
					closest = shape;
					closestDistance = dst;
				}
			}
		}

		Mtx44 mtx = node.getFullTransform(time);
		Vec3 pos = mtx.mult(new Vec3(0, 0, 0));
		double dst = point.sub(pos).length();
		if(dst < closestDistance) {
			return node;
		} else if(closest != null) {
			return closest.getSource();
		} else {
			return null;
		}
	}

	private void select(Vec3 point) {
		Node closest = getNode(point);

		if(closest != null) {
			// select this node
			treeEditor.select(closest);
		}

		repaint();
	}

	private class MouseController extends MouseAdapter {
		private int startX;
		private int startY;

		private LineNode line;
		private CircleNode circle;
		private Node startNode;
		private Vec3 startPos;
		private Property<Vec3> posProp;

		private boolean useTranslation;

		private Property<Vec3> getLinePointProperty(LineNode ln, Vec3 point) {
			Mtx44 mtx = ln.getFullTransform(time);
			Vec3 start = mtx.mult(ln.getStart(time));
			Vec3 end = mtx.mult(ln.getEnd(time));
			Vec3 zero = mtx.mult(new Vec3(0, 0, 0));

			double startDist = start.sub(point).length();
			double endDist = end.sub(point).length();
			double zeroDist = zero.sub(point).length();

			if(startDist < endDist && startDist < zeroDist) {
				return ln.getProperty(StandardPropertyNames.START);
			} else if(endDist < startDist && endDist < zeroDist) {
				return ln.getProperty(StandardPropertyNames.END);
			} else {
				useTranslation = true;
				return ln.getProperty(StandardPropertyNames.TRANSLATION);
			}
		}

		private static double clamp(double x) {
			return Math.max(Math.min(x, 1), -1);
		}

		private static Vec3 clamp(Vec3 vec) {
			double px = clamp(vec.x);
			double py = clamp(vec.y);
			double pz = clamp(vec.z);
			return new Vec3(px, py, pz);
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			int x = e.getX();
			int y = e.getY();

			Mtx44 mtx = getInverseTransform();
			Vec3 point = mtx.mult(new Vec3(x, y, 0));

			if((e.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) != 0) {
				Color3 color = new Color3(1.0, 1.0, 1.0);
				if(node instanceof GroupNode) {
					GroupNode group = (GroupNode) node;
					PointNode p = new PointNode();
					p.setPosition(time, clamp(point));
					p.setColor(time, color);
					group.addChild(p);
					treeEditor.nodeInserted(p);
					repaint();
				}
			} else if(e.getModifiersEx() == 0) {
				// no modifiers
				select(point);
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {
			startX = e.getX();
			startY = e.getY();

			Mtx44 mtx = getInverseTransform();
			Vec3 point = mtx.mult(new Vec3(startX, startY, 0));

			startNode = getNode(point);
			useTranslation = false;
			if(startNode != null) {
				if(e.getModifiersEx() == (MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.SHIFT_DOWN_MASK)) {
					if(startNode instanceof CircleNode) {
						circle = (CircleNode) startNode;
					}
				} else {
					if(startNode instanceof LineNode) {
						posProp = getLinePointProperty((LineNode) startNode, point);
					} else {
						posProp = startNode.getProperty(StandardPropertyNames.POSITION);
						if(posProp == null) {
							useTranslation = true;
							posProp = startNode
									.getProperty(StandardPropertyNames.TRANSLATION);
						}
					}

					if(posProp != null) {
						startPos = posProp.getValue(time);
					} else {
						startPos = null;
					}
				}
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			int x = e.getX();
			int y = e.getY();

			if(startX == x && startY == y) {
				// only a click
				return;
			}

			if((e.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) != 0) {
				// create something
				if((e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0) {
					if(circle != null) {
						// in this case a circle
						Mtx44 mtx = getInverseTransform(circle);
						Vec3 start = mtx.mult(new Vec3(startX, startY, 0));
						Vec3 end = mtx.mult(new Vec3(x, y, 0));

						circle.setPosition(time, start);
						circle.setRadius(time, clamp(end.sub(start).length()));
						updated.callback();
						repaint();
					}
				} else {
					if(line != null) {
						// in this case a line
						Mtx44 mtx = getInverseTransform(line);
						Vec3 start = mtx.mult(new Vec3(startX, startY, 0));
						Vec3 end = mtx.mult(new Vec3(x, y, 0));

						line.setStart(time, clamp(start));
						line.setEnd(time, clamp(end));
						updated.callback();
						repaint();
					}
				}
			}

			line = null;
			circle = null;
			startNode = null;
			startPos = null;
			posProp = null;
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			int x = e.getX();
			int y = e.getY();

			if((e.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) != 0) {
				if((e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0) {
					if(circle == null) {
						if(node instanceof GroupNode) {
							GroupNode group = (GroupNode) node;
							circle = new CircleNode();
							group.addChild(circle);
							treeEditor.nodeInserted(circle);
						}
					}
					if(circle != null) {
						Mtx44 mtx = getInverseTransform(circle);
						Vec3 start = mtx.mult(new Vec3(startX, startY, 0));
						Vec3 end = mtx.mult(new Vec3(x, y, 0));
						circle.setPosition(time, start);
						circle.setRadius(time, clamp(end.sub(start).length()));
						updated.callback();
						repaint();
					}
				} else {
					if(line == null) {
						if(node instanceof GroupNode) {
							GroupNode group = (GroupNode) node;
							line = new LineNode();
							group.addChild(line);
							treeEditor.nodeInserted(line);
						}
					}
					if(line != null) {
						Mtx44 mtx = getInverseTransform(line);
						Vec3 start = mtx.mult(new Vec3(startX, startY, 0));
						Vec3 end = mtx.mult(new Vec3(x, y, 0));
						line.setStart(time, clamp(start));
						line.setEnd(time, clamp(end));
						updated.callback();
						repaint();
					}
				}
			} else if(e.getModifiersEx() == (MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.SHIFT_DOWN_MASK)) {
				// shift down

				Mtx44 mtx = getInverseTransform();
				Vec3 end = mtx.mult(new Vec3(x, y, 0));

				if(circle != null) {
					// change circle radius
					mtx = circle.getFullTransform(time);
					Vec3 pos = mtx.mult(circle.getPosition(time));
					double radius = end.sub(pos).length();
					circle.setRadius(time, clamp(radius));
					updated.callback();
					repaint();
				}
			} else if(e.getModifiersEx() == MouseEvent.BUTTON1_DOWN_MASK) {
				// no modifiers

				if(startNode != null && startPos != null && posProp != null) {
					Node transformNode = useTranslation ? startNode.getParent() : startNode;
					Mtx44 mtx = getInverseTransform(transformNode);
					Vec3 start = mtx.mult(new Vec3(startX, startY, 0));
					Vec3 end = mtx.mult(new Vec3(x, y, 0));
					Vec3 diff = end.sub(start);

					Vec3 pos = startPos.add(diff);
					posProp.setValue(time, clamp(pos));
					updated.callback();
					repaint();
				}
			}
		}
	}
}
