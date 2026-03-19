package com.unknown.emulight.lcp.io.esl.protocol;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.unknown.util.io.Endianess;

/*
 * typedef struct {
 *        u8              hour;
 *        u8              minute;
 *        u8              second;
 *        u8              weekday;
 *        u8              day;
 *        u8              month;
 *        u16             year;
 * } ESLClockInfo;
 */
public class ESLSetClockPacket extends ESLSystemPacket {
	private Date date;

	private static final int WEEKDAY_MONDAY = 1;
	private static final int WEEKDAY_TUESDAY = 2;
	private static final int WEEKDAY_WEDNESDAY = 3;
	private static final int WEEKDAY_THURSDAY = 4;
	private static final int WEEKDAY_FRIDAY = 5;
	private static final int WEEKDAY_SATURDAY = 6;
	private static final int WEEKDAY_SUNDAY = 7;

	ESLSetClockPacket(byte addr) {
		super(addr);
	}

	public ESLSetClockPacket(byte dest, byte channel, int source, int destination, Date date) {
		super(dest, channel, ESL_SYSTEM_CMD_SET_CLOCK, source, destination);
		this.date = date;
	}

	public ESLSetClockPacket(byte channel, int source, int destination, Date date) {
		super(channel, ESL_SYSTEM_CMD_SET_CLOCK, source, destination);
		this.date = date;
	}

	@Override
	protected int getDataSize() {
		return 8;
	}

	@Override
	protected void writeData(byte[] buf, int offset) {
		Calendar cal = Calendar.getInstance(TimeZone.getDefault());
		cal.setTime(date);

		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int minute = cal.get(Calendar.MINUTE);
		int second = cal.get(Calendar.SECOND);
		int weekday = getWeekday(cal.get(Calendar.DAY_OF_WEEK));
		int day = cal.get(Calendar.DATE);
		int month = cal.get(Calendar.MONTH) + 1;
		int year = cal.get(Calendar.YEAR);

		buf[offset + 0] = (byte) hour;
		buf[offset + 1] = (byte) minute;
		buf[offset + 2] = (byte) second;
		buf[offset + 3] = (byte) weekday;
		buf[offset + 4] = (byte) day;
		buf[offset + 5] = (byte) month;
		buf[offset + 6] = (byte) year;
		buf[offset + 7] = (byte) (year >>> 8);
	}

	@Override
	protected void readData(byte[] buf, int offset) {
		Calendar cal = Calendar.getInstance(TimeZone.getDefault());
		cal.set(Calendar.HOUR_OF_DAY, Byte.toUnsignedInt(buf[offset]));
		cal.set(Calendar.MINUTE, Byte.toUnsignedInt(buf[offset + 1]));
		cal.set(Calendar.SECOND, Byte.toUnsignedInt(buf[offset + 2]));
		cal.set(Calendar.DAY_OF_WEEK, setWeekday(Byte.toUnsignedInt(buf[offset + 3])));
		cal.set(Calendar.DATE, Byte.toUnsignedInt(buf[offset + 4]));
		cal.set(Calendar.MONTH, Byte.toUnsignedInt(buf[offset + 5]) - 1);
		cal.set(Calendar.YEAR, Short.toUnsignedInt(Endianess.get16bitLE(buf, offset + 6)));
	}

	private static int getWeekday(int weekday) {
		switch(weekday) {
		default:
		case Calendar.MONDAY:
			return WEEKDAY_MONDAY;
		case Calendar.TUESDAY:
			return WEEKDAY_TUESDAY;
		case Calendar.WEDNESDAY:
			return WEEKDAY_WEDNESDAY;
		case Calendar.THURSDAY:
			return WEEKDAY_THURSDAY;
		case Calendar.FRIDAY:
			return WEEKDAY_FRIDAY;
		case Calendar.SATURDAY:
			return WEEKDAY_SATURDAY;
		case Calendar.SUNDAY:
			return WEEKDAY_SUNDAY;
		}
	}

	private static int setWeekday(int weekday) {
		switch(weekday) {
		default:
		case WEEKDAY_MONDAY:
			return Calendar.MONDAY;
		case WEEKDAY_TUESDAY:
			return Calendar.TUESDAY;
		case WEEKDAY_WEDNESDAY:
			return Calendar.WEDNESDAY;
		case WEEKDAY_THURSDAY:
			return Calendar.THURSDAY;
		case WEEKDAY_FRIDAY:
			return Calendar.FRIDAY;
		case WEEKDAY_SATURDAY:
			return Calendar.SATURDAY;
		case WEEKDAY_SUNDAY:
			return Calendar.SUNDAY;
		}
	}
}
