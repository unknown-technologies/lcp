package com.unknown.emulight.lcp.ui;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import com.unknown.platform.windows.WinError;
import com.unknown.platform.windows.Windows;
import com.unknown.platform.windows.registry.RegistryKey;
import com.unknown.platform.windows.registry.RegistryValueInfo;
import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;

public class SerialLineInfo {
	private static final Logger log = Trace.create(SerialLineInfo.class);

	public static String[] getSerialLines() {
		if(Windows.isWindows()) {
			try {
				RegistryKey key = new RegistryKey(RegistryKey.HKEY_LOCAL_MACHINE,
						"HARDWARE\\DEVICEMAP\\SERIALCOMM", RegistryKey.KEY_READ);
				RegistryValueInfo[] values = key.enumerateValues(true);
				String[] serialLines = new String[values.length];

				for(int i = 0; i < values.length; i++) {
					serialLines[i] = values[i].getSZ();
				}

				return serialLines;
			} catch(WinError e) {
				log.log(Levels.ERROR, "Cannot enumerate COM ports: " + e.getMessage(), e);
				return new String[0];
			} catch(UnsatisfiedLinkError | NoClassDefFoundError e) {
				// UnsatisfiedLinkError is thrown at the first attempt, NoClassDefFoundError on every
				// later attempt
				log.log(Levels.ERROR, "Cannot enumerate COM ports: native library not available");
				return new String[0];
			}
		} else if(isLinux()) {
			List<String> devices = new ArrayList<>();

			Path sysfs = Paths.get("/sys/class/tty");
			for(File file : sysfs.toFile().listFiles()) {
				// is this a directory?
				if(!file.isDirectory()) {
					continue;
				}

				// does this have a driver?
				if(!new File(file, "device/driver").exists()) {
					continue;
				}

				File devfile = new File("/dev", file.getName());
				// does the device file in /dev exist?
				if(!devfile.exists()) {
					continue;
				}

				devices.add(devfile.toString());
			}

			devices.sort(new DeviceFileSorter());

			return devices.toArray(String[]::new);
		} else {
			return new String[0];
		}
	}

	private static boolean isDigit(char c) {
		return c >= '0' && c <= '9';
	}

	private static class DeviceFileSorter implements Comparator<String> {
		public int compare(String a, String b) {
			int minlen = Math.min(a.length(), b.length());
			for(int i = 0; i < minlen; i++) {
				char ca = a.charAt(i);
				char cb = b.charAt(i);
				if(isDigit(ca) && isDigit(ca) == isDigit(cb)) {
					String idA = a.substring(i);
					String idB = b.substring(i);
					try {
						int ia = Integer.parseInt(idA);
						int ib = Integer.parseInt(idB);
						return Integer.compareUnsigned(ia, ib);
					} catch(NumberFormatException e) {
						// standard ASCII handling
					}
				}
				if(ca != cb) {
					return ca - cb;
				}
			}
			return a.length() > b.length() ? 1 : -1;
		}
	}

	private static boolean isLinux() {
		return System.getProperty("os.name").toLowerCase().contains("linux");
	}
}
