package com.unknown.emulight.lcp.laser.node.pong;

import java.util.ArrayList;
import java.util.List;

import com.unknown.emulight.lcp.laser.Point3D;
import com.unknown.emulight.lcp.laser.node.Color3;
import com.unknown.emulight.lcp.laser.node.Node;
import com.unknown.emulight.lcp.laser.node.Property;
import com.unknown.emulight.lcp.laser.node.Shape;
import com.unknown.emulight.lcp.project.Project;
import com.unknown.math.g3d.Mtx44;
import com.unknown.math.g3d.Vec3;

public class PongNode extends Node {
	private final Property<Double> paddleSize = new Property<>("paddleSize", 0.125, 0.0, 1.0);
	private final Property<Color3> paddleColor = new Property<>("paddleColor", new Color3(0, 1, 0));
	private final Property<Boolean> scoreVisible = new Property<>("scoreVisible", true);
	private final Property<Double> scoreSize = new Property<>("scoreSize", 0.0078125, 0.0, 1.0);
	private final Property<Color3> scoreColor = new Property<>("scoreColor", new Color3(1, 0, 0));
	private final Property<Vec3> scorePosition = new Property<>("scorePosition", new Vec3(-0.033203125, -0.1875, 0),
			MIN3D, MAX3D);
	private final Property<Double> ballRadius = new Property<>("ballRadius", 0.00390625, 0.0, 1.0);
	private final Property<Color3> ballColor = new Property<>("ballColor", new Color3(0.5, 0.5, 0.5));
	private final Property<Integer> ballPoints = new Property<>("ballPoints", 5, 3, 1000);
	private final Property<Double> fieldSizeX = new Property<>("fieldSizeX", 0.328125, 0.0, 1.0);
	private final Property<Double> fieldSizeY = new Property<>("fieldSizeY", 0.328125, 0.0, 1.0);
	private final Property<Integer> score1 = new Property<>("score1", 3, 0, 9);
	private final Property<Integer> score2 = new Property<>("score2", 2, 0, 9);
	private final Property<Double> time1 = new Property<>("time1", 1.0, 0.0, 32.0);
	private final Property<Double> time2 = new Property<>("time2", 0.7, 0.0, 32.0);
	private final Property<Double> speedX = new Property<>("speedX", 1.0, 0.0, 32.0);
	private final Property<Double> speedY = new Property<>("speedY", 0.625, 0.0, 32.0);
	private final Property<Double> paddleSpeed = new Property<>("paddleSpeed", 0.25, 0.0, 1.0);

	public PongNode() {
		super(PongNodePlugin.ID, true);

		addProperty(paddleSize);
		addProperty(paddleColor);
		addProperty(scoreVisible);
		addProperty(scoreSize);
		addProperty(scoreColor);
		addProperty(scorePosition);
		addProperty(ballRadius);
		addProperty(ballColor);
		addProperty(ballPoints);
		addProperty(fieldSizeX);
		addProperty(fieldSizeY);
		addProperty(score1);
		addProperty(score2);
		addProperty(time1);
		addProperty(time2);
		addProperty(speedX);
		addProperty(speedY);
		addProperty(paddleSpeed);
	}

	public double getPaddleSize(int time) {
		return paddleSize.getValue(time);
	}

	public void setPaddleSize(int time, double size) {
		paddleSize.setValue(time, size);
	}

	public Color3 getPaddleColor(int time) {
		return paddleColor.getValue(time);
	}

	public void setPaddleColor(int time, Color3 color) {
		paddleColor.setValue(time, color);
	}

	public boolean isScoreVisible(int time) {
		return scoreVisible.getValue(time);
	}

	public void setScoreVisible(int time, boolean visible) {
		scoreVisible.setValue(time, visible);
	}

	public double getScoreSize(int time) {
		return scoreSize.getValue(time);
	}

	public void setScoreSize(int time, double size) {
		scoreSize.setValue(time, size);
	}

	public Color3 getScoreColor(int time) {
		return scoreColor.getValue(time);
	}

	public void setScoreColor(int time, Color3 color) {
		scoreColor.setValue(time, color);
	}

	public Vec3 getScorePosition(int time) {
		return scorePosition.getValue(time);
	}

	public void setScorePosition(int time, Vec3 position) {
		scorePosition.setValue(time, position);
	}

	public double getBallRadius(int time) {
		return ballRadius.getValue(time);
	}

	public void setBallRadius(int time, double radius) {
		ballRadius.setValue(time, radius);
	}

	public Color3 getBallColor(int time) {
		return ballColor.getValue(time);
	}

	public void setBallColor(int time, Color3 color) {
		ballColor.setValue(time, color);
	}

	public int getBallPoints(int time) {
		return ballPoints.getValue(time);
	}

	public void setBallPoints(int time, int points) {
		ballPoints.setValue(time, points);
	}

	public double getFieldSizeX(int time) {
		return fieldSizeX.getValue(time);
	}

	public void setFieldSizeX(int time, double size) {
		fieldSizeX.setValue(time, size);
	}

	public double getFieldSizeY(int time) {
		return fieldSizeY.getValue(time);
	}

	public void setFieldSizeY(int time, double size) {
		fieldSizeY.setValue(time, size);
	}

	public int getScore1(int time) {
		return score1.getValue(time);
	}

	public void setScore1(int time, int score) {
		score1.setValue(time, score);
	}

	public int getScore2(int time) {
		return score2.getValue(time);
	}

	public void setScore2(int time, int score) {
		score2.setValue(time, score);
	}

	public double getTime1(int time) {
		return time1.getValue(time);
	}

	public void setTime1(int time, double value) {
		time1.setValue(time, value);
	}

	public double getTime2(int time) {
		return time2.getValue(time);
	}

	public void setTime2(int time, double value) {
		time2.setValue(time, value);
	}

	public double getSpeedX(int time) {
		return speedX.getValue(time);
	}

	public void setSpeedX(int time, double speed) {
		speedX.setValue(time, speed);
	}

	public double getSpeedY(int time) {
		return speedY.getValue(time);
	}

	public void setSpeedY(int time, double speed) {
		speedY.setValue(time, speed);
	}

	public double getPaddleSpeed(int time) {
		return paddleSpeed.getValue(time);
	}

	public void setPaddleSpeed(int time, double speed) {
		paddleSpeed.setValue(time, speed);
	}

	private static void add(List<Point3D> points, Color3 color, double x, double y) {
		points.add(new Point3D(new Vec3(x, y, 0), color));
	}

	private void addPaddle(List<Point3D> points, int time, double x, double y) {
		double sz = getPaddleSize(time) / 2;
		Color3 color = getPaddleColor(time);
		Color3 black = new Color3(0, 0, 0);

		add(points, black, x, y - sz);
		add(points, black, x, y - sz);
		// add(points, color, x, y - sz);
		add(points, color, x, y + sz);
		add(points, color, x, y - sz);
		add(points, black, x, y - sz);
		add(points, black, x, y - sz);
	}

	private static void addZero(List<Point3D> points, double x, double y) {
		add(points, new Color3(0, 0, 0), x, y);
	}

	private void addDigit(List<Point3D> points, int time, double x, double y, int value) {
		double scale = getScoreSize(time);
		double scaleX = scale;
		double scaleY = 2 * scale;
		Color3 color = getScoreColor(time);
		Color3 black = new Color3(0, 0, 0);

		switch(value % 10) {
		case 0:
			addZero(points, x + scaleX, y + scaleY);
			addZero(points, x + scaleX, y + scaleY);

			// add(points, color, x + scaleX, y + scaleY);
			add(points, color, x - scaleX, y + scaleY);
			add(points, color, x - scaleX, y - scaleY);
			add(points, color, x + scaleX, y - scaleY);
			add(points, color, x + scaleX, y + scaleY);

			addZero(points, x + scaleX, y + scaleY);
			addZero(points, x + scaleX, y + scaleY);
			break;
		case 1:
			addZero(points, x + scaleX, y + scaleY);
			addZero(points, x + scaleX, y + scaleY);

			// add(points, color, x + scaleX, y + scaleY);
			add(points, color, x + scaleX, y - scaleY);

			addZero(points, x + scaleX, y - scaleY);
			addZero(points, x + scaleX, y - scaleY);
			break;

		case 2:
			addZero(points, x - scaleX, y - scaleY);
			addZero(points, x - scaleX, y - scaleY);

			// add(points, color, x - scaleX, y - scaleY);
			add(points, color, x + scaleX, y - scaleY);
			add(points, color, x + scaleX, y);
			add(points, color, x - scaleX, y);
			add(points, color, x - scaleX, y + scaleY);
			add(points, color, x + scaleX, y + scaleY);

			addZero(points, x + scaleX, y + scaleY);
			addZero(points, x + scaleX, y + scaleY);
			break;

		case 3:
			addZero(points, x - scaleX, y - scaleY);
			addZero(points, x - scaleX, y - scaleY);

			// add(points, color, x - scaleX, y - scaleY);
			add(points, color, x + scaleX, y - scaleY);
			add(points, color, x + scaleX, y);
			add(points, color, x - scaleX, y);
			add(points, black, x + scaleX, y);
			add(points, color, x + scaleX, y + scaleY);
			add(points, color, x - scaleX, y + scaleY);

			addZero(points, x - scaleX, y + scaleY);
			addZero(points, x - scaleX, y + scaleY);
			break;

		case 4:
			addZero(points, x + scaleX, y + scaleY);
			addZero(points, x + scaleX, y + scaleY);

			// add(points, color, x + scaleX, y + scaleY);
			add(points, color, x + scaleX, y - scaleY);
			add(points, black, x - scaleX, y - scaleY);
			add(points, color, x - scaleX, y);
			add(points, color, x + scaleX, y);

			addZero(points, x + scaleX, y);
			addZero(points, x + scaleX, y);
			break;

		case 5:
			addZero(points, x + scaleX, y - scaleY);
			addZero(points, x + scaleX, y - scaleY);

			// add(points, color, x + scaleX, y - scaleY);
			add(points, color, x - scaleX, y - scaleY);
			add(points, color, x - scaleX, y);
			add(points, color, x + scaleX, y);
			add(points, color, x + scaleX, y + scaleY);
			add(points, color, x - scaleX, y + scaleY);

			addZero(points, x - scaleX, y + scaleY);
			addZero(points, x - scaleX, y + scaleY);
			break;

		case 6:
			addZero(points, x + scaleX, y - scaleY);
			addZero(points, x + scaleX, y - scaleY);

			// add(points, color, x + scaleX, y - scaleY);
			add(points, color, x - scaleX, y - scaleY);
			add(points, color, x - scaleX, y + scaleY);
			add(points, color, x + scaleX, y + scaleY);
			add(points, color, x + scaleX, y);
			add(points, color, x - scaleX, y);

			addZero(points, x - scaleX, y);
			addZero(points, x - scaleX, y);
			break;

		case 7:
			addZero(points, x + scaleX, y + scaleY);
			addZero(points, x + scaleX, y + scaleY);

			// add(points, color, x + scaleX, y + scaleY);
			add(points, color, x + scaleX, y - scaleY);
			add(points, color, x - scaleX, y - scaleY);

			addZero(points, x - scaleX, y - scaleY);
			addZero(points, x - scaleX, y - scaleY);
			break;

		case 8:
			addZero(points, x + scaleX, y);
			addZero(points, x + scaleX, y);

			// add(points, color, x + scaleX, y);
			add(points, color, x + scaleX, y - scaleY);
			add(points, color, x - scaleX, y - scaleY);
			add(points, color, x - scaleX, y);
			add(points, color, x + scaleX, y);
			add(points, color, x + scaleX, y + scaleY);
			add(points, color, x - scaleX, y + scaleY);
			add(points, color, x - scaleX, y);

			addZero(points, x - scaleX, y);
			addZero(points, x - scaleX, y);
			break;

		case 9:
			addZero(points, x - scaleX, y + scaleY);
			addZero(points, x - scaleX, y + scaleY);

			// add(points, color, x - scaleX, y + scaleY);
			add(points, color, x + scaleX, y + scaleY);
			add(points, color, x + scaleX, y - scaleY);
			add(points, color, x - scaleX, y - scaleY);
			add(points, color, x - scaleX, y);
			add(points, color, x + scaleX, y);

			addZero(points, x + scaleX, y);
			addZero(points, x + scaleX, y);
			break;
		}
	}

	@Override
	protected List<Shape> render(List<Shape> result, int time, Mtx44 positionTransform, Mtx44 colorTransform) {
		Mtx44 positionMtx = positionTransform.concat(getTransformation(time));
		Mtx44 colorMtx = colorTransform.concat(getColorTransformation(time));

		List<Point3D> points = new ArrayList<>();

		double fieldSzX = getFieldSizeX(time);
		double fieldSzY = getFieldSizeY(time);

		double radius = getBallRadius(time);
		Color3 ballCol = getBallColor(time);
		Vec3 scorePos = getScorePosition(time);

		int max = getBallPoints(time);

		Project project = getClip().getProject();

		int ppq = project.getPPQ();

		int timeSignatureNumerator = 4; // TODO: get this from the signature track

		int ballTimeX = ppq * timeSignatureNumerator;
		int ballTimeY = ppq * timeSignatureNumerator;

		double ballXpos = ((int) (time * getSpeedX(time)) % ballTimeX) / (double) ballTimeX;
		double ballYpos = ((int) (time * getSpeedY(time)) % ballTimeY) / (double) ballTimeY;

		double ballX = ((ballXpos > 0.5 ? 1.0 - ballXpos : ballXpos) * 4.0 - 1.0) * fieldSzX;
		double ballY = ((ballYpos > 0.5 ? 1.0 - ballYpos : ballYpos) * 4.0 - 1.0) * fieldSzY;

		int tmax1 = (int) (ppq * getTime1(time) * timeSignatureNumerator);
		int tmax2 = (int) (ppq * getTime2(time) * timeSignatureNumerator);
		if(tmax1 == 0) {
			tmax1 = 1;
		}
		if(tmax2 == 0) {
			tmax2 = 1;
		}

		int t1 = time % tmax1;
		int t2 = time % tmax2;

		double paddleSpd = getPaddleSpeed(time);
		double paddle1 = Math.sin(t1 / (double) tmax1 * 2.0 * Math.PI) * paddleSpd;
		double paddle2 = Math.sin(t2 / (double) tmax2 * 2.0 * Math.PI) * paddleSpd;

		double dst1 = Math.abs(-fieldSzX - ballX);
		double x1 = dst1 / (2.0 * fieldSzX);
		paddle1 = paddle1 * x1 + (ballY * (1.0 - x1));

		double dst2 = Math.abs(fieldSzX - ballX);
		double x2 = dst2 / (2.0 * fieldSzX);
		paddle2 = paddle2 * x2 + (ballY * (1.0 - x2));

		addPaddle(points, time, -fieldSzX, paddle1);

		addZero(points, -fieldSzX, paddle1);
		addZero(points, ballX, ballY + radius);
		addZero(points, ballX, ballY + radius);

		for(int i = 0; i <= max; i++) {
			double x = Math.sin(i / (double) max * 2.0 * Math.PI);
			double y = Math.cos(i / (double) max * 2.0 * Math.PI);

			add(points, ballCol, ballX + x * radius, ballY + y * radius);
		}

		addZero(points, ballX, ballY + radius);
		addZero(points, fieldSzX, paddle2);

		addPaddle(points, time, fieldSzX, paddle2);

		if(isScoreVisible(time)) {
			addDigit(points, time, -scorePos.x, scorePos.y, getScore2(time));
			addDigit(points, time, scorePos.x, scorePos.y, getScore1(time));
		}

		// apply transformations
		List<Point3D> pts = new ArrayList<>();
		for(Point3D point : points) {
			pts.add(new Point3D(positionMtx.mult(point.getPosition()), colorMtx.mult(point.getColor())));
		}

		result.add(new Shape(this, pts));

		return result;
	}

	@Override
	public PongNode clone() {
		PongNode node = new PongNode();
		copy(node);
		return node;
	}
}
