package com.unknown.emulight.lcp.ui;

import javax.swing.AbstractSpinnerModel;

@SuppressWarnings("serial")
public class SpinnerProgramModel extends AbstractSpinnerModel {
	private int program;

	public SpinnerProgramModel(int prog) {
		setValue(prog);
	}

	public int getProgram() {
		return program;
	}

	@Override
	public Object getValue() {
		return program;
	}

	@Override
	public void setValue(Object value) {
		this.program = (int) value;
		fireStateChanged();
	}

	@Override
	public Object getNextValue() {
		if(program == 127) {
			return program;
		} else {
			return program + 1;
		}
	}

	@Override
	public Object getPreviousValue() {
		if(program == -1) {
			return program;
		} else {
			return program - 1;
		}
	}
}
