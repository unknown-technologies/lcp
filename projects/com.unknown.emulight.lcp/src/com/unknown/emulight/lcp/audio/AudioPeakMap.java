package com.unknown.emulight.lcp.audio;

public class AudioPeakMap {
	private final Layer[] map;
	private final int sampleCount;

	public AudioPeakMap(AudioData part) {
		sampleCount = part.getSampleCount();
		int count = (int) Math.ceil(Math.log(sampleCount) / Math.log(2));

		float[] mono = part.getMono();

		map = new Layer[count];
		map[0] = new Layer(mono);
		for(int i = 1; i < map.length; i++) {
			map[i] = new Layer(map[i - 1]);
		}
	}

	public void getWaveform(float[] min, float[] max, int start, int end) {
		getWaveform(min, max, start, end, 0, min.length);
	}

	public void getWaveform(float[] min, float[] max, int start, int end, int off, int len) {
		if(start > sampleCount) {
			throw new IllegalArgumentException("invalid start position");
		}
		if(end > sampleCount) {
			throw new IllegalArgumentException("invalid end position");
		}
		if(end < start) {
			throw new IllegalArgumentException("end (" + end + ") < start (" + start + ")");
		}
		if(len == 0) {
			throw new IllegalArgumentException("len = 0");
		}

		double pointsPerSample = (end - start) / (double) len;
		double position = Math.log(pointsPerSample) / Math.log(2);
		int layerId = (int) Math.floor(position);
		if(layerId < 0) {
			layerId = 0;
		}
		int layerIdCeil = (int) Math.ceil(position);
		if(layerIdCeil >= map.length) {
			layerIdCeil = map.length - 1;
		} else if(layerIdCeil < 0) {
			layerIdCeil = 0;
		}

		Layer layer = map[layerId];
		Layer layerCeil = map[layerIdCeil];
		for(int i = 0; i < len; i++) {
			// TODO: some better form of interpolation between layers
			double t = start + i * pointsPerSample;
			if(layerId == layerIdCeil) {
				int index = (int) Math.round(t / layer.getDivisor());
				min[i + off] = layer.getMinimum(index);
				max[i + off] = layer.getMaximum(index);
			} else {
				int index1 = (int) Math.round(t / layer.getDivisor());
				int index2 = (int) Math.round(t / layerCeil.getDivisor());
				min[i + off] = Math.min(layer.getMinimum(index1), layerCeil.getMinimum(index2));
				max[i + off] = Math.max(layer.getMaximum(index1), layerCeil.getMaximum(index2));
			}
		}
	}

	public static class Layer {
		private float[] min;
		private float[] max;
		private final int divisor;

		public Layer(float[] samples) {
			min = samples;
			max = samples;
			divisor = 1;
		}

		public Layer(Layer layer) {
			divisor = layer.divisor * 2;

			int len = layer.min.length / 2;
			min = new float[len];
			max = new float[len];
			for(int i = 0; i < len; i++) {
				int start = 2 * i;
				min[i] = Math.min(layer.getMinimum(start), layer.getMinimum(start + 1));
				max[i] = Math.max(layer.getMaximum(start), layer.getMaximum(start + 1));
			}
		}

		public int getDivisor() {
			return divisor;
		}

		public float getMinimum(int pos) {
			if(pos < min.length) {
				return min[pos];
			} else if(min.length > 0) {
				return min[min.length - 1];
			} else {
				return 0;
			}
		}

		public float getMaximum(int pos) {
			if(pos < max.length) {
				return max[pos];
			} else if(max.length > 0) {
				return max[max.length - 1];
			} else {
				return 0;
			}
		}

		public float[] getMinimum() {
			return min;
		}

		public float[] getMaximum() {
			return max;
		}
	}
}
