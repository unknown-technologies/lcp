package com.unknown.emulight.lcp.laser.node.fx;

import java.util.ArrayList;
import java.util.List;

import com.unknown.emulight.lcp.laser.Point3D;
import com.unknown.emulight.lcp.laser.node.GroupNode;
import com.unknown.emulight.lcp.laser.node.Node;
import com.unknown.emulight.lcp.laser.node.Property;
import com.unknown.emulight.lcp.laser.node.Shape;
import com.unknown.emulight.lcp.laser.node.StandardPropertyNames;
import com.unknown.math.g3d.Mtx44;
import com.unknown.math.g3d.Vec3;

public class MaskNode extends GroupNode {
	public static final String TYPE = "mask";

	private final Property<Double> radius = new Property<>(StandardPropertyNames.RADIUS, 0.1, 0.0, 1.0);
	private final Property<Boolean> clip = new Property<>(StandardPropertyNames.CLIP, false);

	public MaskNode() {
		super(TYPE);

		addProperty(radius);
		addProperty(clip);
	}

	public double getRadius(int time) {
		return radius.getValue(time);
	}

	public boolean isClip(int time) {
		return clip.getValue(time);
	}

	@Override
	protected List<Shape> render(List<Shape> result, int time, Mtx44 positionTransform, Mtx44 colorTransform) {
		List<Shape> shapes = new ArrayList<>();
		super.render(shapes, time, new Mtx44(), colorTransform, false, true);

		Mtx44 positionMtx = positionTransform.concat(getTransformation(time));

		boolean trunc = isClip(time);
		double r = getRadius(time);
		Vec3 zero = new Vec3(0, 0, 0);

		for(Shape shape : shapes) {
			List<Point3D> points = new ArrayList<>();
			for(Point3D point : shape.getPoints()) {
				Vec3 pos = point.getPosition();
				Vec3 diff = pos.sub(zero);
				double len = diff.length();
				if(len > r) {
					if(!trunc) {
						// scale to length
						pos = diff.scale(r / len);
						points.add(new Point3D(positionMtx.mult(pos), point.getColor()));
					}
				} else {
					points.add(new Point3D(positionMtx.mult(pos), point.getColor()));
				}
			}
			result.add(new Shape(shape.getSource(), points));
		}
		return result;
	}

	@Override
	public MaskNode clone() {
		MaskNode node = new MaskNode();
		copy(node);
		for(Node n : getChildren()) {
			node.addChild(n.clone());
		}
		return node;
	}
}
