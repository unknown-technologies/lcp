package com.unknown.emulight.lcp.laser;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import com.unknown.net.shownet.Laser;
import com.unknown.net.shownet.Point;
import com.unknown.net.shownet.ShowNET;
import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;

public class Pong {
	public static void main(String[] args) throws IOException, InterruptedException {
		Trace.setupConsoleApplication(Levels.INFO);

		InetAddress addr = InetAddress.getByName(args[0]);

		ShowNET shownet = new ShowNET(false);

		Laser laser = shownet.connect(addr);
		System.out.println(laser.getInfo());

		pong(laser);
	}

	private static void addPaddle(List<Point> points, int x, int y, int r, int g, int b) {
		Point p = new Point();
		p.red = (short) 0x0000;
		p.green = (short) 0x0000;
		p.blue = (short) 0x0000;
		p.x = (short) (x + 0x8000);
		p.y = (short) (y - 0x0800 + 0x8000);
		points.add(p);

		p = new Point();
		p.red = (short) 0x0000;
		p.green = (short) 0x0000;
		p.blue = (short) 0x0000;
		p.x = (short) (x + 0x8000);
		p.y = (short) (y - 0x0800 + 0x8000);
		points.add(p);

		p = new Point();
		p.red = (short) r;
		p.green = (short) g;
		p.blue = (short) b;
		p.x = (short) (x + 0x8000);
		p.y = (short) (y - 0x0800 + 0x8000);
		points.add(p);

		p = new Point();
		p.red = (short) r;
		p.green = (short) g;
		p.blue = (short) b;
		p.x = (short) (x + 0x8000);
		p.y = (short) (y + 0x0800 + 0x8000);
		points.add(p);

		p = new Point();
		p.red = (short) 0x0000;
		p.green = (short) 0x0000;
		p.blue = (short) 0x0000;
		p.x = (short) (x + 0x8000);
		p.y = (short) (y + 0x0800 + 0x8000);
		points.add(p);

		p = new Point();
		p.red = (short) 0x0000;
		p.green = (short) 0x0000;
		p.blue = (short) 0x0000;
		p.x = (short) (x + 0x8000);
		p.y = (short) (y + 0x0800 + 0x8000);
		points.add(p);
	}

	private static void addZero(List<Point> points, int x, int y) {
		Point p = new Point();
		p = new Point();
		p.x = (short) (x + 0x8000);
		p.y = (short) (y + 0x8000);
		points.add(p);
	}

	private static void addDigit(List<Point> points, int x, int y, int value, int scale, int r, int g, int b) {
		Point p;
		int scaleX = scale;
		int scaleY = 2 * scale;

		switch(value % 10) {
		case 0:
			addZero(points, x + scaleX, y + scaleY);
			addZero(points, x + scaleX, y + scaleY);
			p = new Point();
			p.red = (short) r;
			p.green = (short) g;
			p.blue = (short) b;
			p.x = (short) (x + scaleX + 0x8000);
			p.y = (short) (y + scaleY + 0x8000);
			points.add(p);

			p = new Point();
			p.red = (short) r;
			p.green = (short) g;
			p.blue = (short) b;
			p.x = (short) (x - scaleX + 0x8000);
			p.y = (short) (y + scaleY + 0x8000);
			points.add(p);

			p = new Point();
			p.red = (short) r;
			p.green = (short) g;
			p.blue = (short) b;
			p.x = (short) (x - scaleX + 0x8000);
			p.y = (short) (y - scaleY + 0x8000);
			points.add(p);

			p = new Point();
			p.red = (short) r;
			p.green = (short) g;
			p.blue = (short) b;
			p.x = (short) (x + scaleX + 0x8000);
			p.y = (short) (y - scaleY + 0x8000);
			points.add(p);

			p = new Point();
			p.red = (short) r;
			p.green = (short) g;
			p.blue = (short) b;
			p.x = (short) (x + scaleX + 0x8000);
			p.y = (short) (y + scaleY + 0x8000);
			points.add(p);

			addZero(points, x + scaleX, y + scaleY);
			addZero(points, x + scaleX, y + scaleY);
			break;
		case 1:
			addZero(points, x + scaleX, y + scaleY);
			addZero(points, x + scaleX, y + scaleY);
			p = new Point();
			p.red = (short) r;
			p.green = (short) g;
			p.blue = (short) b;
			p.x = (short) (x + scaleX + 0x8000);
			p.y = (short) (y + scaleY + 0x8000);
			points.add(p);

			p = new Point();
			p.red = (short) r;
			p.green = (short) g;
			p.blue = (short) b;
			p.x = (short) (x + scaleX + 0x8000);
			p.y = (short) (y - scaleY + 0x8000);
			points.add(p);

			addZero(points, x + scaleX, y + scaleY);
			addZero(points, x - scaleX, y + scaleY);
			break;

		case 2:
			addZero(points, x - scaleX, y - scaleY);
			addZero(points, x - scaleX, y - scaleY);

			p = new Point();
			p.red = (short) r;
			p.green = (short) g;
			p.blue = (short) b;
			p.x = (short) (x - scaleX + 0x8000);
			p.y = (short) (y - scaleY + 0x8000);
			points.add(p);

			p = new Point();
			p.red = (short) r;
			p.green = (short) g;
			p.blue = (short) b;
			p.x = (short) (x + scaleX + 0x8000);
			p.y = (short) (y - scaleY + 0x8000);
			points.add(p);

			p = new Point();
			p.red = (short) r;
			p.green = (short) g;
			p.blue = (short) b;
			p.x = (short) (x + scaleX + 0x8000);
			p.y = (short) (y + 0x8000);
			points.add(p);

			p = new Point();
			p.red = (short) r;
			p.green = (short) g;
			p.blue = (short) b;
			p.x = (short) (x - scaleX + 0x8000);
			p.y = (short) (y + 0x8000);
			points.add(p);

			p = new Point();
			p.red = (short) r;
			p.green = (short) g;
			p.blue = (short) b;
			p.x = (short) (x - scaleX + 0x8000);
			p.y = (short) (y + scaleY + 0x8000);
			points.add(p);

			p = new Point();
			p.red = (short) r;
			p.green = (short) g;
			p.blue = (short) b;
			p.x = (short) (x + scaleX + 0x8000);
			p.y = (short) (y + scaleY + 0x8000);
			points.add(p);

			addZero(points, x + scaleX, y + scaleY);
			addZero(points, x + scaleX, y + scaleY);
			break;

		case 3:
			addZero(points, x - scaleX, y - scaleY);
			addZero(points, x - scaleX, y - scaleY);

			p = new Point();
			p.red = (short) r;
			p.green = (short) g;
			p.blue = (short) b;
			p.x = (short) (x - scaleX + 0x8000);
			p.y = (short) (y - scaleY + 0x8000);
			points.add(p);

			p = new Point();
			p.red = (short) r;
			p.green = (short) g;
			p.blue = (short) b;
			p.x = (short) (x + scaleX + 0x8000);
			p.y = (short) (y - scaleY + 0x8000);
			points.add(p);

			p = new Point();
			p.red = (short) r;
			p.green = (short) g;
			p.blue = (short) b;
			p.x = (short) (x + scaleX + 0x8000);
			p.y = (short) (y + 0x8000);
			points.add(p);

			p = new Point();
			p.red = (short) r;
			p.green = (short) g;
			p.blue = (short) b;
			p.x = (short) (x - scaleX + 0x8000);
			p.y = (short) (y + 0x8000);
			points.add(p);

			p = new Point();
			p.red = (short) 0;
			p.green = (short) 0;
			p.blue = (short) 0;
			p.x = (short) (x + scaleX + 0x8000);
			p.y = (short) (y + 0x8000);
			points.add(p);

			p = new Point();
			p.red = (short) r;
			p.green = (short) g;
			p.blue = (short) b;
			p.x = (short) (x + scaleX + 0x8000);
			p.y = (short) (y + scaleY + 0x8000);
			points.add(p);

			p = new Point();
			p.red = (short) r;
			p.green = (short) g;
			p.blue = (short) b;
			p.x = (short) (x - scaleX + 0x8000);
			p.y = (short) (y + scaleY + 0x8000);
			points.add(p);

			addZero(points, x - scaleX, y + scaleY);
			addZero(points, x - scaleX, y + scaleY);
			break;
		}

	}

	private static void pong(Laser laser) throws IOException, InterruptedException {
		List<Point> points = new ArrayList<>();

		int max = 5;
		int radius = 0x0080;
		int fieldSize = 0x2A00;

		int paddleR = 0x0000;
		int paddleG = 0xFFFF;
		int paddleB = 0x0000;

		int ballR = 0x8000;
		int ballG = 0x8000;
		int ballB = 0x8000;

		int scoreR = 0xFFFF;
		int scoreG = 0x0000;
		int scoreB = 0x0000;

		int score1 = 3;
		int score2 = 2;

		int paddle1 = 0;
		int paddle2 = 2;

		int ballX = 0;
		int ballY = 0;

		int directionX = 1;
		int directionY = 1;
		int speedX = 0x100;
		int speedY = 0x0A0;

		int tmax1 = 1000;
		int tmax2 = 700;
		int t1 = 0;
		int t2 = 0;
		while(true) {
			Thread.sleep(5);
			points.clear();

			t1++;
			t1 %= tmax1;

			t2++;
			t2 %= tmax2;
			paddle1 = (int) (Math.sin(t1 / (double) tmax1 * 2.0 * Math.PI) * 0x2000);
			paddle2 = (int) (Math.sin(t2 / (double) tmax2 * 2.0 * Math.PI) * 0x2000);

			int dst1 = Math.abs(-fieldSize - ballX);
			double x1 = dst1 / (2.0 * fieldSize);
			paddle1 = (int) (paddle1 * x1 + (ballY * (1.0 - x1)));

			int dst2 = Math.abs(fieldSize - ballX);
			double x2 = dst2 / (2.0 * fieldSize);
			paddle2 = (int) (paddle2 * x2 + (ballY * (1.0 - x2)));

			if(directionX > 0) {
				if(ballX >= fieldSize) {
					directionX = -1;
					ballX -= speedX;
				} else {
					ballX += speedX;
				}
			} else {
				if(ballX <= -fieldSize) {
					directionX = 1;
					ballX += speedX;
				} else {
					ballX -= speedX;
				}
			}

			if(directionY > 0) {
				if(ballY >= fieldSize) {
					directionY = -1;
					ballY -= speedY;
				} else {
					ballY += speedY;
				}
			} else {
				if(ballY <= -fieldSize) {
					directionY = 1;
					ballY += speedY;
				} else {
					ballY -= speedY;
				}
			}

			addPaddle(points, -fieldSize, paddle1, paddleR, paddleG, paddleB);

			addZero(points, ballX - radius, ballY + radius);

			for(int i = 0; i < max; i++) {
				double x = Math.sin(i / (double) max * 2.0 * Math.PI);
				double y = Math.cos(i / (double) max * 2.0 * Math.PI);

				Point p = new Point();
				p.red = (short) ballR;
				p.green = (short) ballG;
				p.blue = (short) ballB;
				p.x = (short) (ballX + x * radius + 0x8000);
				p.y = (short) (ballY + y * radius + 0x8000);
				points.add(p);
			}

			addPaddle(points, fieldSize, paddle2, paddleR, paddleG, paddleB);

			addDigit(points, 0x0440, -0x1800, score2, 0x0100, scoreR, scoreG, scoreB);
			addDigit(points, -0x0440, -0x1800, score1, 0x0100, scoreR, scoreG, scoreB);

			laser.sendFrame(points, 1500);
		}
	}
}
