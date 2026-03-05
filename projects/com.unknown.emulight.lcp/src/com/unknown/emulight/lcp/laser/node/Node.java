package com.unknown.emulight.lcp.laser.node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.unknown.emulight.lcp.laser.Clip;
import com.unknown.emulight.lcp.laser.Point3D;
import com.unknown.math.g3d.Mtx44;
import com.unknown.math.g3d.Vec3;
import com.unknown.xml.dom.Element;

public abstract class Node implements Cloneable {
	private final Property<String> name = new Property<>(StandardPropertyNames.NAME, String.class);
	private final Property<Vec3> translation = new Property<>(StandardPropertyNames.TRANSLATION, new Vec3(0, 0, 0));
	private final Property<Vec3> scale = new Property<>(StandardPropertyNames.SCALE, new Vec3(1, 1, 1));
	private final Property<Double> rotation = new Property<>(StandardPropertyNames.ROTATION, 0.0);

	private final Property<Vec3> colorScale = new Property<>(StandardPropertyNames.COLOR_SCALE, new Vec3(1, 1, 1));

	private final Map<String, Property<?>> properties = new HashMap<>();
	private final List<Property<?>> propertyList = new ArrayList<>();

	private final boolean isLeaf;

	private Node parent;
	private Clip clip;

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

	protected abstract List<Shape> render(List<Shape> result, Mtx44 positionTransform, Mtx44 colorTransform);

	protected Node(boolean isLeaf) {
		this.isLeaf = isLeaf;

		addProperty(name);
		addProperty(translation);
		addProperty(scale);
		addProperty(rotation);
		addProperty(colorScale);
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
	public Clip getClip() {
		Node root = getRootNode();
		return root.clip;
	}

	protected void setClip(Clip clip) {
		Node root = getRootNode();
		root.clip = clip;
	}

	public boolean isLeaf() {
		return isLeaf;
	}

	public String getName() {
		return name.getValue();
	}

	public void setName(String name) {
		this.name.setValue(name);
	}

	public List<Property<?>> getProperties() {
		return Collections.unmodifiableList(propertyList);
	}

	protected void addProperty(Property<?> property) {
		if(properties.put(property.getName(), property) == null) {
			propertyList.add(property);
		}
	}

	public <T> void setProperty(String key, T value) {
		@SuppressWarnings("unchecked")
		Property<T> prop = (Property<T>) properties.get(key);
		if(prop == null) {
			throw new IllegalArgumentException("unknown property");
		}
		prop.setValue(value);
	}

	@SuppressWarnings("unchecked")
	public <T> Property<T> getProperty(String key) {
		return (Property<T>) properties.get(key);
	}

	public Vec3 getTranslation() {
		return translation.getValue();
	}

	public void setTranslation(Vec3 translation) {
		this.translation.setValue(translation);
	}

	public Vec3 getScale() {
		return scale.getValue();
	}

	public void setScale(Vec3 scale) {
		this.scale.setValue(scale);
	}

	public double getRotation() {
		return rotation.getValue();
	}

	public void setRotation(double rotation) {
		this.rotation.setValue(rotation);
	}

	public Mtx44 getTransformation() {
		return Mtx44.rotDegZ(getRotation()).transApply(getTranslation()).applyScale(getScale());
	}

	public Mtx44 getInverseTransformation() {
		Vec3 sc = getScale();
		Vec3 tr = getTranslation();
		return Mtx44.rotDegZ(-getRotation()).scaleApply(1.0 / sc.x, 1.0 / sc.y, 1.0 / sc.z).applyTrans(-tr.x,
				-tr.y, -tr.z);
	}

	public Vec3 getColorScale() {
		return colorScale.getValue();
	}

	public void setColorScale(Vec3 colorScale) {
		this.colorScale.setValue(colorScale);
	}

	public Mtx44 getColorTransformation() {
		return Mtx44.scale(getColorScale());
	}

	public Mtx44 getFullTransform() {
		Mtx44 result = new Mtx44();
		for(Node node = this; node != null; node = node.getParent()) {
			result = node.getTransformation().concat(result);
		}

		return result;
	}

	public Mtx44 getFullColorTransform() {
		Mtx44 result = new Mtx44();
		for(Node node = this; node != null; node = node.getParent()) {
			result = node.getTransformation().concat(result);
		}

		return result;
	}

	public final List<Shape> getShapes() {
		List<Shape> shapes = new ArrayList<>();
		render(shapes, new Mtx44(), new Mtx44());
		return shapes;
	}

	public final List<Point3D> render() {
		return render(new Mtx44(), new Mtx44());
	}

	public final List<Point3D> render(Mtx44 positionTransform, Mtx44 colorTransform) {
		List<Point3D> result = new ArrayList<>();

		List<Shape> shapes = new ArrayList<>();
		render(shapes, positionTransform, colorTransform);

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

		return result;
	}

	protected void copyFrom(Node other) {
		for(Property<?> p : other.propertyList) {
			if(properties.containsKey(p.getName())) {
				setProperty(p.getName(), p.getValue());
			} else if(!other.getClass().equals(getClass())) {
				throw new IllegalArgumentException("the other node has additional properties");
			}
		}
		clip = other.clip;
	}

	@Override
	public abstract Node clone();

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[name=" + name.getValue() + "]";
	}

	public Element write() {
		Element xml = new Element("node");
		for(Property<? extends Object> p : propertyList) {
			xml.addAttribute(p.getName(), p.getValue().toString());
		}
		return xml;
	}
}
