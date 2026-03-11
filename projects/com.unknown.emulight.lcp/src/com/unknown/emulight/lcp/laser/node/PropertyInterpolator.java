package com.unknown.emulight.lcp.laser.node;

import java.util.HashMap;
import java.util.Map;

import com.unknown.math.g3d.Vec3;

public class PropertyInterpolator {
	private static final Map<Class<?>, Interpolator<?>> interpolators = new HashMap<>();

	static {
		add(Double.class, (int time1, Double value1, int time2, Double value2, int time) -> {
			double dt = time2 - time1;
			double t = (time - time1) / dt;
			return value1 * (1.0 - t) + value2 * t;
		});
		add(Vec3.class, (int time1, Vec3 value1, int time2, Vec3 value2, int time) -> {
			double dt = time2 - time1;
			double t = (time - time1) / dt;
			double x = value1.x * (1.0 - t) + value2.x * t;
			double y = value1.y * (1.0 - t) + value2.y * t;
			double z = value1.z * (1.0 - t) + value2.z * t;
			return new Vec3(x, y, z);
		});
		add(Color3.class, (int time1, Color3 value1, int time2, Color3 value2, int time) -> {
			double dt = time2 - time1;
			double t = (time - time1) / dt;
			double x = value1.x * (1.0 - t) + value2.x * t;
			double y = value1.y * (1.0 - t) + value2.y * t;
			double z = value1.z * (1.0 - t) + value2.z * t;
			return new Color3(x, y, z);
		});
	}

	private static <T> void add(Class<T> clazz, Interpolator<T> interpolator) {
		interpolators.put(clazz, interpolator);
	}

	public static <T> T interpolate(Class<T> clazz, int time1, T value1, int time2, T value2, int time) {
		@SuppressWarnings("unchecked")
		Interpolator<T> inter = (Interpolator<T>) interpolators.get(clazz);
		if(inter != null) {
			return inter.interpolate(time1, value1, time2, value2, time);
		} else {
			return value1;
		}
	}

	@FunctionalInterface
	private interface Interpolator<T> {
		T interpolate(int time1, T value1, int time2, T value2, int time);
	}
}
