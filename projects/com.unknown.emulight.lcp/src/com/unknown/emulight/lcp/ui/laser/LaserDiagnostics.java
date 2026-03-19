package com.unknown.emulight.lcp.ui.laser;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import com.unknown.net.shownet.Laser;
import com.unknown.util.HexFormatter;
import com.unknown.util.ui.LabeledPairLayout;

@SuppressWarnings("serial")
public class LaserDiagnostics extends JPanel {
	private static final int WIDTH = 16;

	private Laser laser;

	public LaserDiagnostics() {
		super(new BorderLayout());
		JPanel fields = new JPanel(new LabeledPairLayout());
		JTextField address = new JTextField("20000000");
		JTextField length = new JTextField("2048");
		fields.add(LabeledPairLayout.LABEL, new JLabel("Address:"));
		fields.add(LabeledPairLayout.COMPONENT, address);
		fields.add(LabeledPairLayout.LABEL, new JLabel("Length:"));
		fields.add(LabeledPairLayout.COMPONENT, length);

		JEditorPane result = new JEditorPane();
		result.setContentType("text/plain");
		result.setEditable(false);
		result.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));

		JButton read = new JButton("Read");

		read.addActionListener(e -> {
			if(laser != null) {
				try {
					int addr = Integer.parseInt(address.getText().trim(), 16);
					int len = Integer.parseInt(length.getText().trim(), 10);
					if(len <= 0 || len > 2048) {
						throw new IllegalArgumentException("invalid length: " + len);
					}
					byte[] data = laser.readMemory(addr, len);

					StringBuilder buf = new StringBuilder();
					for(int i = 0; i < data.length; i++) {
						if((i % WIDTH) == 0) {
							buf.append(HexFormatter.tohex(addr + i, 8));
							buf.append(":");
						}
						buf.append(' ');
						buf.append(HexFormatter.tohex(Byte.toUnsignedInt(data[i]), 2));
						if((i % WIDTH) == WIDTH - 1) {
							// add the decoded text
							buf.append("   ");
							for(int j = 0; j < WIDTH; j++) {
								int c = Byte.toUnsignedInt(data[i - WIDTH + 1 + j]);
								if(c >= 0x20 && c < 0x7F) {
									buf.append((char) c);
								} else {
									buf.append('.');
								}
							}
							buf.append("\n");
						}
					}

					if((data.length % WIDTH) != 0) {
						for(int i = (data.length % WIDTH); i < WIDTH; i++) {
							buf.append("   ");
						}

						buf.append("   ");

						for(int i = data.length - (data.length % WIDTH); i < data.length; i++) {
							int c = Byte.toUnsignedInt(data[i]);
							if(c >= 0x20 && c < 0x7F) {
								buf.append((char) c);
							} else {
								buf.append('.');
							}
						}
					}

					int pos = result.getCaretPosition();
					result.setText(buf.toString().trim());
					if(pos < result.getText().length()) {
						result.setCaretPosition(pos);
					}
				} catch(IllegalArgumentException ex) {
					JOptionPane.showMessageDialog(this, ex.getMessage(), "Error",
							JOptionPane.ERROR_MESSAGE | JOptionPane.OK_OPTION);
				} catch(IOException ex) {
					JOptionPane.showMessageDialog(this, ex.getMessage(), "Error",
							JOptionPane.ERROR_MESSAGE | JOptionPane.OK_OPTION);
				}
			} else {
				result.setText("NO LASER SELECTED");
			}
		});

		JPanel buttons = new JPanel(new FlowLayout());
		buttons.add(read);

		add(BorderLayout.NORTH, fields);
		add(BorderLayout.CENTER, new JScrollPane(result));
		add(BorderLayout.SOUTH, buttons);
	}

	public void setLaser(Laser laser) {
		this.laser = laser;
	}
}
