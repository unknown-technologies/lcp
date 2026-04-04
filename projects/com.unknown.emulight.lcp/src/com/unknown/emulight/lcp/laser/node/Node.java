package com.unknown.emulight.lcp.laser.node;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.unknown.emulight.lcp.laser.LaserPart;
import com.unknown.emulight.lcp.laser.Point3D;
import com.unknown.emulight.lcp.laser.node.fx.StroboNode;
import com.unknown.emulight.lcp.laser.node.plugin.CustomNodePlugin;
import com.unknown.emulight.lcp.laser.node.plugin.CustomNodePluginRegistry;
import com.unknown.math.g3d.Mtx44;
import com.unknown.math.g3d.Vec3;
import com.unknown.xml.dom.Element;

public abstract class Node implements Cloneable {
	protected static final Vec3 ZERO = new Vec3(0.0, 0.0, 0.0);
	protected static final Vec3 ONE = new Vec3(1.0, 1.0, 1.0);
	protected static final Vec3 TWO = new Vec3(2.0, 2.0, 2.0);
	protected static final Vec3 MIN3D = new Vec3(-1.0, -1.0, -1.0);
	protected static final Vec3 MAX3D = new Vec3(1.0, 1.0, 1.0);
	protected static final Color3 BLACK = new Color3(0.0, 0.0, 0.0);
	protected static final Color3 WHITE = new Color3(1.0, 1.0, 1.0);

	private final Property<String> name = new Property<>(StandardPropertyNames.NAME, String.class, true);
	private final Property<Boolean> enabled = new Property<>(StandardPropertyNames.ENABLED, true);
	private final Property<Vec3> translation = new Property<>(StandardPropertyNames.TRANSLATION, ZERO, MIN3D,
			MAX3D);
	private final Property<Vec3> scale = new Property<>(StandardPropertyNames.SCALE, ONE, ZERO, TWO);
	private final Property<Double> rotation = new Property<>(StandardPropertyNames.ROTATION, 0.0, 0.0, 360.0);

	private final Property<Vec3> colorScale = new Property<>(StandardPropertyNames.COLOR_SCALE, ONE, ZERO, ONE);
	private final Property<Double> brightness = new Property<>(StandardPropertyNames.BRIGHTNESS, 1.0, 0.0, 1.0);

	private final Map<String, Property<?>> properties = new HashMap<>();
	private final List<Property<?>> propertyList = new ArrayList<>();

	private final boolean isLeaf;

	private final String type;

	private Node parent;
	private LaserPart clip;

	public List<Node> getChildren() {
		return Collections.emptyList();
	}

	public void addChild(@SuppressWarnings("unused") Node node) {
		if(isLeaf) {
			throw new UnsupportedOperationException("cannot add a child node to a leaf");
		}
	}

	public void addChild(@SuppressWarnings("unused") Node node, @SuppressWarnings("unused") int index) {
		if(isLeaf) {
			throw new UnsupportedOperationException("cannot add a child node to a leaf");
		}
	}

	public void removeChild(@SuppressWarnings("unused") Node node) {
		if(isLeaf) {
			throw new UnsupportedOperationException("cannot remove a child node from a leaf");
		}
	}

	protected abstract List<Shape> render(List<Shape> result, int time, Mtx44 positionTransform,
			Mtx44 colorTransform);

	protected Node(String type, boolean isLeaf) {
		this.type = type;
		this.isLeaf = isLeaf;

		addProperty(name);
		addProperty(enabled);
		addProperty(translation);
		addProperty(scale);
		addProperty(rotation);
		addProperty(colorScale);
		addProperty(brightness);
	}

	protected void setParent(Node parent) {
		this.parent = parent;
	}

	public Node getParent() {
		return parent;
	}

	public Node getRootNode() {
		for(Node node = this; node != null; node = node.getParent()) {
			if(node.getParent() == null) {
				return node;
			}
		}
		return this;
	}

	// the clip is only ever set on the root node
	public LaserPart getClip() {
		Node root = getRootNode();
		assert root.clip != null;
		return root.clip;
	}

	protected void setClip(LaserPart clip) {
		Node root = getRootNode();
		root.clip = clip;
	}

	public boolean isLeaf() {
		return isLeaf;
	}

	public String getName() {
		return name.getValue(0);
	}

	public void setName(String name) {
		this.name.setValue(0, name);
	}

	public List<Property<?>> getProperties() {
		return Collections.unmodifiableList(propertyList);
	}

	protected void addProperty(Property<?> property) {
		if(properties.put(property.getName(), property) == null) {
			propertyList.add(property);
		}
	}

	public <T> void setProperty(int time, String key, T value) {
		@SuppressWarnings("unchecked")
		Property<T> prop = (Property<T>) properties.get(key);
		if(prop == null) {
			throw new IllegalArgumentException("unknown property");
		}
		prop.setValue(time, value);
	}

	@SuppressWarnings("unchecked")
	public <T> Property<T> getProperty(String key) {
		return (Property<T>) properties.get(key);
	}

	public boolean isEnabled(int time) {
		return enabled.getValue(time);
	}

	public void setEnabled(int time, boolean enabled) {
		this.enabled.setValue(time, enabled);
	}

	public Vec3 getTranslation(int time) {
		return translation.getValue(time);
	}

	public void setTranslation(int time, Vec3 translation) {
		this.translation.setValue(time, translation);
	}

	public Vec3 getScale(int time) {
		return scale.getValue(time);
	}

	public void setScale(int time, Vec3 scale) {
		this.scale.setValue(time, scale);
	}

	public double getRotation(int time) {
		return rotation.getValue(time);
	}

	public void setRotation(int time, double rotation) {
		this.rotation.setValue(time, rotation);
	}

	public Mtx44 getTransformation(int time) {
		return Mtx44.rotDegZ(getRotation(time)).transApply(getTranslation(time)).applyScale(getScale(time));
	}

	public Mtx44 getInverseTransformation(int time) {
		Vec3 sc = getScale(time);
		Vec3 tr = getTranslation(time);
		return Mtx44.rotDegZ(-getRotation(time)).scaleApply(1.0 / sc.x, 1.0 / sc.y, 1.0 / sc.z).applyTrans(
				-tr.x, -tr.y, -tr.z);
	}

	public Vec3 getColorScale(int time) {
		return colorScale.getValue(time);
	}

	public void setColorScale(int time, Vec3 colorScale) {
		this.colorScale.setValue(time, colorScale);
	}

	public double getBrightness(int time) {
		return brightness.getValue(time);
	}

	public void setBrightness(int time, double brightness) {
		this.brightness.setValue(time, brightness);
	}

	public Mtx44 getColorTransformation(int time) {
		double intensity = getBrightness(time);
		return Mtx44.scale(getColorScale(time)).applyScale(intensity, intensity, intensity);
	}

	public Mtx44 getFullTransform(int time) {
		Mtx44 result = new Mtx44();
		for(Node node = this; node != null; node = node.getParent()) {
			result = node.getTransformation(time).concat(result);
		}

		return result;
	}

	public Mtx44 getFullColorTransform(int time) {
		Mtx44 result = new Mtx44();
		for(Node node = this; node != null; node = node.getParent()) {
			result = node.getTransformation(time).concat(result);
		}

		return result;
	}

	public final List<Shape> getShapes(int time) {
		List<Shape> shapes = new ArrayList<>();
		render(shapes, time, new Mtx44(), new Mtx44());
		return shapes;
	}

	public final List<Point3D> render(int time) {
		return render(time, new Mtx44(), new Mtx44());
	}

	public final List<Point3D> render(int time, Mtx44 positionTransform, Mtx44 colorTransform) {
		List<Point3D> result = new ArrayList<>();

		List<Shape> shapes = new ArrayList<>();
		render(shapes, time, positionTransform, colorTransform);

		if(shapes.isEmpty()) {
			Vec3 pos = positionTransform.mult(new Vec3(0, 0, 0));
			Vec3 col = colorTransform.mult(new Vec3(0, 0, 0));
			result.add(new Point3D(pos, col));
		} else if(shapes.size() == 1) {
			result.addAll(shapes.get(0).getPoints());
		} else {
			// concatenate points and insert empty points between shapes
			for(Shape shape : shapes) {
				List<Point3D> points = shape.getPoints();

				Point3D first = points.getFirst();
				Point3D last = points.getLast();
				Point3D emptyStart = new Point3D(first.getPosition(), new Vec3(0, 0, 0));
				result.add(emptyStart);
				result.addAll(points);
				Point3D emptyEnd = new Point3D(last.getPosition(), new Vec3(0, 0, 0));
				result.add(emptyEnd);
			}
		}

		return result;
	}

	public final List<Shape> getShapes(int time, Mtx44 positionTransform, Mtx44 colorTransform) {
		List<Shape> shapes = new ArrayList<>();
		render(shapes, time, positionTransform, colorTransform);
		return shapes;
	}

	private <T> void copy(Property<T> other) {
		@SuppressWarnings("unchecked")
		Property<T> prop = (Property<T>) properties.get(other.getName());
		prop.copy(other);
	}

	protected void copy(Node other) {
		for(Property<? extends Object> p : propertyList) {
			Property<? extends Object> prop = other.properties.get(p.getName());
			if(prop != null) {
				copy(prop);
			} else if(!other.getClass().equals(getClass())) {
				throw new IllegalArgumentException("the other node has additional properties");
			}
		}
		other.clip = clip;
	}

	@Override
	public abstract Node clone();

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[name=" + getName() + "]";
	}

	public Element write() {
		Element xml = new Element("node");
		xml.addAttribute("type", type);
		for(Property<? extends Object> p : propertyList) {
			xml.addChild(p.write());
		}
		for(Node n : getChildren()) {
			xml.addChild(n.write());
		}
		return xml;
	}

	public static Node read(Element xml) throws IOException {
		return read(null, xml);
	}

	public static Node read(LaserPart clip, Element xml) throws IOException {
		if(!xml.name.equals("node")) {
			throw new IOException("not a node");
		}

		String type = xml.getAttribute("type");
		if(type == null) {
			throw new IOException("missing node type");
		}

		Node node;
		switch(type) {
		case CircleNode.TYPE:
			node = new CircleNode();
			break;
		case GroupNode.TYPE:
			node = new GroupNode();
			break;
		case LineNode.TYPE:
			node = new LineNode();
			break;
		case PointNode.TYPE:
			node = new PointNode();
			break;
		case StroboNode.TYPE:
			node = new StroboNode();
			break;
		case RasterImageNode.TYPE:
			node = new RasterImageNode();
			break;
		default:
			// maybe a node from a plugin?
			CustomNodePlugin plugin = CustomNodePluginRegistry.get().getPlugin(type);
			if(plugin != null) {
				node = plugin.create();
			} else {
				throw new IOException("unknown node type: " + type);
			}
		}

		for(Element e : xml.getChildren()) {
			switch(e.name) {
			case "property":
				String name = e.getAttribute("name");
				if(name == null) {
					throw new IOException("missing property name");
				}

				Property<?> prop = node.properties.get(name);
				if(prop == null) {
					throw new IOException("unknown property");
				}

				prop.read(e);
				break;
			case "node":
				Node n = read(e);
				node.addChild(n);
				break;
			default:
				throw new IOException("unknown element: " + e.name);
			}
		}

		node.clip = clip;
		return node;
	}
}
