package com.unknown.emulight.lcp.ui;

import java.text.ParseException;

import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.JSpinner;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.JTextField;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.DocumentFilter;

import com.unknown.util.ui.AbstractDocumentFilter;

@SuppressWarnings("serial")
public class SpinnerProgramEditor extends DefaultEditor {
	public SpinnerProgramEditor(JSpinner spinner) {
		super(spinner);
		if(!(spinner.getModel() instanceof SpinnerProgramModel)) {
			throw new IllegalArgumentException("model not a SpinnerProgramModel");
		}
		getTextField().setEditable(true);
		getTextField().setFormatterFactory(new DefaultFormatterFactory(new ProgramFormatter()));
		getTextField().setHorizontalAlignment(JTextField.RIGHT);
	}

	public SpinnerProgramModel getModel() {
		return (SpinnerProgramModel) (getSpinner().getModel());
	}

	private class ProgramFormatter extends AbstractFormatter {
		private DocumentFilter filter;

		@Override
		public String valueToString(Object value) throws ParseException {
			if(value == null) {
				return "--";
			}
			int val = (int) value;
			if(val == -1) {
				return "--";
			} else {
				return Integer.toString(val);
			}
		}

		@Override
		public Object stringToValue(String string) throws ParseException {
			if(string.equals("-") || string.equals("--")) {
				return -1;
			} else {
				return Integer.parseInt(string);
			}
		}

		@Override
		protected DocumentFilter getDocumentFilter() {
			if(filter == null) {
				filter = new Filter();
			}
			return filter;
		}

		private class Filter extends AbstractDocumentFilter {
			@Override
			protected boolean test(String text) {
				if(text.equals("-") || text.equals("--")) {
					return true;
				}
				try {
					int val = Integer.parseInt(text);
					return val >= 0 && val <= 127;
				} catch(NumberFormatException e) {
					return false;
				}
			}
		}
	}
}
