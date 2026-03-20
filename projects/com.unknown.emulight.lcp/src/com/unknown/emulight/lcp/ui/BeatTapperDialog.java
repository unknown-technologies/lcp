package com.unknown.emulight.lcp.ui;

import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent.Cause;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

import com.unknown.util.ui.CopyableLabel;

@SuppressWarnings("serial")
public class BeatTapperDialog extends JDialog {
	private static final int MENU_MODIFIER = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

	private final CopyableLabel label;

	private long lastTime = -1;

	private final Deque<Long> times = new LinkedList<>();

	public BeatTapperDialog(JFrame parent) {
		super(parent, "Beat Tapper");

		JLabel title = new JLabel("BPM:");
		label = new CopyableLabel("000.000");

		Font font = label.getFont();
		font = new Font(font.getName(), Font.BOLD, font.getSize() * 2);
		title.setFont(font);
		label.setFont(font);

		JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		panel.add(title);
		panel.add(label);

		setLayout(new GridBagLayout());
		add(panel, new GridBagConstraints());

		JRootPane root = getRootPane();

		KeyStroke enterKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
		KeyStroke spaceKey = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0);
		Object action = new Object();
		root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(enterKey, action);
		root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(spaceKey, action);
		root.getActionMap().put(action, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				tap();
			}
		});

		KeyStroke quitKey = KeyStroke.getKeyStroke(KeyEvent.VK_Q, MENU_MODIFIER);
		KeyStroke escKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		Object quit = new Object();
		root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(quitKey, quit);
		root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escKey, quit);
		root.getActionMap().put(quit, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});

		addWindowFocusListener(new WindowAdapter() {
			@Override
			public void windowGainedFocus(WindowEvent e) {
				requestFocusInWindow();
			}
		});

		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				requestFocus(Cause.MOUSE_EVENT);
				tap();
			}
		});

		setSize(320, 240);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}

	private void setBPM(double bpm) {
		label.setText(String.format("%07.3f", bpm));
	}

	public void tap() {
		long now = System.currentTimeMillis();
		long dt = now - lastTime;
		if(lastTime < 0 || dt > 3000) {
			times.clear();
			setBPM(0);
		} else {
			times.add(dt);
			if(times.size() > 10) {
				times.removeFirst();
			}

			// compute BPM
			long[] deltas = new long[times.size()];
			int i = 0;
			for(long ts : times) {
				deltas[i++] = ts;
			}

			// sort delta times to ignore outliers
			Arrays.sort(deltas);

			// find 1/4 and 3/4 point
			int start = Math.round(deltas.length / 4.0f);
			int end = Math.round(deltas.length * 3 / 4.0f);
			// make sure there is at least one point used
			if(start == end) {
				end = start + 1;
			}
			int count = end - start;

			// compute final BPM as average of the 1/4 to 3/4 delta times
			long deltacnt = 0;
			for(i = start; i < end; i++) {
				deltacnt += deltas[i];
			}

			double avgtime = deltacnt / (count * 1000.0);
			setBPM(60.0 / avgtime);
		}
		lastTime = now;
	}
}
