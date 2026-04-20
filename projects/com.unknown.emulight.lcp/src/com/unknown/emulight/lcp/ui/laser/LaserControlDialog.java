package com.unknown.emulight.lcp.ui.laser;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;

import com.unknown.emulight.lcp.laser.LaserProcessor;
import com.unknown.emulight.lcp.laser.LaserRenderer;
import com.unknown.emulight.lcp.project.EmulightSystem;
import com.unknown.emulight.lcp.ui.UIUtils;
import com.unknown.net.shownet.InterfaceId;
import com.unknown.net.shownet.Laser;
import com.unknown.net.shownet.Point;
import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;
import com.unknown.util.ui.LabeledPairLayout;

@SuppressWarnings("serial")
public class LaserControlDialog extends JDialog implements LaserRenderer {
	private static final Logger log = Trace.create(LaserControlDialog.class);

	private final EmulightSystem sys;
	private final InterfaceId id;
	private final LaserRenderer oldRenderer;

	private int scannerX = 0x8000;
	private int scannerY = 0x8000;
	private int red = 0;
	private int green = 0;
	private int blue = 0;
	private int intensity = 0;
	private int userColor1 = 0;
	private int userColor2 = 0;
	private int userColor3 = 0;

	private final byte[] dmx = new byte[512];
	private volatile boolean dmxDirty = true;

	public LaserControlDialog(JDialog parent, EmulightSystem sys, InterfaceId id) {
		super(parent, "Manual Control", ModalityType.APPLICATION_MODAL);

		this.sys = sys;
		this.id = id;

		int hardwareId = 0;

		LaserProcessor processor = sys.getLaserProcessor();
		InetAddress addr = processor.getLaserAddress(id);
		if(addr != null) {
			Laser laser = processor.getLaser(addr);
			hardwareId = laser.getHardwareId();
		}

		JPanel laserControls = new JPanel(new LabeledPairLayout());
		laserControls.setBorder(UIUtils.padding());
		JSlider sliderX = new JSlider(0, 0xFFFF, scannerX);
		JSlider sliderY = new JSlider(0, 0xFFFF, scannerY);
		JSlider sliderRed = new JSlider(0, 0xFF, red);
		JSlider sliderGreen = new JSlider(0, 0xFF, green);
		JSlider sliderBlue = new JSlider(0, 0xFF, blue);
		laserControls.add(LabeledPairLayout.LABEL, new JLabel("Scanner X:"));
		laserControls.add(LabeledPairLayout.COMPONENT, sliderX);
		laserControls.add(LabeledPairLayout.LABEL, new JLabel("Scanner Y:"));
		laserControls.add(LabeledPairLayout.COMPONENT, sliderY);
		laserControls.add(LabeledPairLayout.LABEL, new JLabel("Red:"));
		laserControls.add(LabeledPairLayout.COMPONENT, sliderRed);
		laserControls.add(LabeledPairLayout.LABEL, new JLabel("Green:"));
		laserControls.add(LabeledPairLayout.COMPONENT, sliderGreen);
		laserControls.add(LabeledPairLayout.LABEL, new JLabel("Blue:"));
		laserControls.add(LabeledPairLayout.COMPONENT, sliderBlue);

		sliderX.addChangeListener(e -> scannerX = sliderX.getValue());
		sliderY.addChangeListener(e -> scannerY = sliderY.getValue());
		sliderRed.addChangeListener(e -> red = sliderRed.getValue());
		sliderGreen.addChangeListener(e -> green = sliderGreen.getValue());
		sliderBlue.addChangeListener(e -> blue = sliderBlue.getValue());

		if(hardwareId == 1) {
			JSlider sliderIntensity = new JSlider(0, 0xFFFF, intensity);
			JSlider sliderUserColor1 = new JSlider(0, 0xFFFF, userColor1);
			JSlider sliderUserColor2 = new JSlider(0, 0xFFFF, userColor2);

			laserControls.add(LabeledPairLayout.LABEL, new JLabel("Intensity:"));
			laserControls.add(LabeledPairLayout.COMPONENT, sliderIntensity);
			laserControls.add(LabeledPairLayout.LABEL, new JLabel("User Color 1:"));
			laserControls.add(LabeledPairLayout.COMPONENT, sliderUserColor1);
			laserControls.add(LabeledPairLayout.LABEL, new JLabel("User Color 2:"));
			laserControls.add(LabeledPairLayout.COMPONENT, sliderUserColor2);

			sliderIntensity.addChangeListener(e -> intensity = sliderIntensity.getValue());
			sliderUserColor1.addChangeListener(e -> userColor1 = sliderUserColor1.getValue());
			sliderUserColor2.addChangeListener(e -> userColor2 = sliderUserColor2.getValue());
		} else if(hardwareId == 5) {
			JSlider sliderUserColor1 = new JSlider(0, 0xFFFF, userColor1);
			JSlider sliderUserColor2 = new JSlider(0, 0xFFFF, userColor2);
			JSlider sliderUserColor3 = new JSlider(0, 0xFFFF, userColor3);

			laserControls.add(LabeledPairLayout.LABEL, new JLabel("User Color 1:"));
			laserControls.add(LabeledPairLayout.COMPONENT, sliderUserColor1);
			laserControls.add(LabeledPairLayout.LABEL, new JLabel("User Color 2:"));
			laserControls.add(LabeledPairLayout.COMPONENT, sliderUserColor2);
			laserControls.add(LabeledPairLayout.LABEL, new JLabel("User Color 3:"));
			laserControls.add(LabeledPairLayout.COMPONENT, sliderUserColor3);

			sliderUserColor1.addChangeListener(e -> userColor1 = sliderUserColor1.getValue());
			sliderUserColor2.addChangeListener(e -> userColor2 = sliderUserColor2.getValue());
			sliderUserColor3.addChangeListener(e -> userColor3 = sliderUserColor3.getValue());
		}

		JPanel dmxFaders = new JPanel(new FlowLayout());
		for(int i = 0; i < 8; i++) {
			dmxFaders.add(createFader(i));
		}

		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("Laser", laserControls);
		tabs.addTab("DMX", dmxFaders);

		JButton close = new JButton("Close");
		close.addActionListener(e -> {
			dispose();
			close();
		});

		JPanel buttons = new JPanel(new FlowLayout());
		buttons.add(close);

		add(BorderLayout.CENTER, tabs);
		add(BorderLayout.SOUTH, buttons);

		pack();

		close.requestFocusInWindow();

		KeyStroke escKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		Object quit = new Object();
		JRootPane root = getRootPane();
		root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escKey, quit);
		root.getActionMap().put(quit, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
				close();
			}
		});

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				close();
			}
		});

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		oldRenderer = processor.getRenderer();
		processor.setRenderer(this);
	}

	private void close() {
		LaserProcessor processor = sys.getLaserProcessor();

		// clear DMX
		for(int i = 0; i < dmx.length; i++) {
			dmx[i] = 0;
		}

		try {
			InetAddress addr = processor.getLaserAddress(id);
			if(addr != null) {
				Laser laser = processor.getLaser(addr);
				if(laser != null && laser.isConnected()) {
					try {
						laser.sendDMX(dmx);
					} catch(IOException e) {
						log.log(Levels.ERROR, "Failed to send DMX data: " + e.getMessage());
					}
				}
			}
		} finally {
			processor.setRenderer(oldRenderer);
		}
	}

	public void render() {
		LaserProcessor processor = sys.getLaserProcessor();
		for(Laser laser : processor.getLasers()) {
			if(!laser.isConnected()) {
				continue;
			}
			int hardwareId = laser.getHardwareId();
			try {
				if(id.equals(laser.getInterfaceId())) {
					if(dmxDirty) {
						dmxDirty = false;
						laser.sendDMX(dmx);
					}
					Point point = new Point();
					point.x = (short) scannerX;
					point.y = (short) scannerY;
					point.red = (short) (red << 8);
					point.green = (short) (green << 8);
					point.blue = (short) (blue << 8);
					if(hardwareId == 5) {
						point.userColor1 = (short) (userColor1 << 8);
						point.userColor2 = (short) (userColor2 << 8);
						point.userColor3 = (short) (userColor3 << 8);
					} else if(hardwareId == 1) {
						point.intensity = (short) (intensity << 8);
						point.userColor1 = (short) (userColor1 << 8);
						point.userColor2 = (short) (userColor2 << 8);
					}
					laser.sendFrame(List.of(point), 1000);
				} else {
					laser.sendNop();
				}
			} catch(IOException e) {
				log.log(Levels.ERROR, "Failed to send frame: " + e.getMessage());
			}
		}
	}

	private JPanel createFader(int channel) {
		JSlider fader = new JSlider(JSlider.VERTICAL, 0, 255, Byte.toUnsignedInt(dmx[channel]));
		JSpinner numericFader = new JSpinner(new SpinnerNumberModel(fader.getValue(), 0, 255, 1));

		fader.addChangeListener(e -> {
			int value = fader.getValue();
			int spinner = ((SpinnerNumberModel) numericFader.getModel()).getNumber().intValue();
			if(spinner != value) {
				numericFader.setValue(value);
			}
			dmx[channel] = (byte) value;
			dmxDirty = true;
		});

		numericFader.addChangeListener(e -> {
			int value = ((SpinnerNumberModel) numericFader.getModel()).getNumber().intValue();
			int slider = fader.getValue();
			if(slider != value) {
				fader.setValue(value);
			}
		});

		fader.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
					numericFader.setValue(0);
					e.consume();
				}
			}
		});

		fader.setToolTipText("Channel " + (channel + 1));

		JPanel faderControls = new JPanel();
		faderControls.setBorder(UIUtils.border("CH" + (channel + 1)));
		faderControls.setToolTipText("Channel " + (channel + 1));
		faderControls.setLayout(new BoxLayout(faderControls, BoxLayout.Y_AXIS));
		faderControls.add(fader);

		JPanel numericFaderPanel = new JPanel(new FlowLayout());
		numericFaderPanel.add(numericFader);
		numericFader.setMaximumSize(numericFader.getPreferredSize());
		faderControls.add(numericFaderPanel);

		return faderControls;
	}
}
