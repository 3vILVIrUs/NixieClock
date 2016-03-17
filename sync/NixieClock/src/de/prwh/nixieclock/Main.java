package de.prwh.nixieclock;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
	private static class Action {
		public final String serial;
		
		public Action(String serial) {
			this.serial = serial;
		}
	}
	
	private static class SerialConn implements Closeable {
		private static Map<String, SerialConn> instances = new HashMap<String, SerialConn>();
		
		private SerialPort serial;
		
		private SerialConn(CommPortIdentifier comm) {
			try {
				serial = (SerialPort) comm.open("Nixie", 20000);
			} catch (PortInUseException e) {
				System.out.println("Port " + comm.getName() + " currently in use");
				System.exit(2);
			}
		}
		
		public OutputStream getOutput(int dataRate) {
			try {
				serial.setSerialPortParams(dataRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
				return serial.getOutputStream();
			} catch (UnsupportedCommOperationException e) {
				System.out.println("Invalid connection parameters for serial connection");
				System.exit(1);
			} catch (IOException e) {
				System.out.println("Unknown IO Error");
				e.printStackTrace();
				System.exit(2);
			}
			return null;
		}
		
		public static SerialConn getInstance(String identifier) {
			SerialConn conn = instances.get(identifier);
			if (conn == null) {
				try {
					CommPortIdentifier port = CommPortIdentifier.getPortIdentifier(identifier);
					conn = new SerialConn(port);
					instances.put(identifier, conn);
				} catch(NoSuchPortException e) {
					System.out.println("Port " + identifier + " not found");
					System.exit(1);
				}
			}
			return conn;
		}

		@Override
		public void close() throws IOException {
			serial.removeEventListener();
			serial.close();
		}
	}
	
	public static String portIdentifier = "/dev/ttyUSB3";
	
	public static void main(String[] argsArr) throws InterruptedException {
		List<Action> actions = processArgs(argsArr);
		try (SerialConn conn = SerialConn.getInstance(portIdentifier);
				PrintWriter printer = new PrintWriter(conn.getOutput(9600))) {
			Thread.sleep(5000);
			for (Action action : actions) {
				printer.println(action.serial);
				printer.flush();
			}
			Thread.sleep(5000);
		} catch (IOException e) {
			System.out.println("Unknown error while closing serial connection");
			e.printStackTrace();
		}
	}
	
	private static List<Action> processArgs(String[] argsArr) {
		List<String> args = Arrays.asList(argsArr);
		List<Action> actions = new ArrayList<Action>();
		
		if (args.contains("--help")) {
			printHelp();
			System.exit(0);
		}
		
		for (int i = 0; i < args.size(); i++) {
			String arg = args.get(i);
			
			switch (arg) {
			case "-t":
			case "--time":
				if (i + 1 < args.size() && !args.get(i + 1).contains("-")) {
					i++;
					actions.add(setTime(args.get(i)));
				} else {
					actions.add(setTime(null));
				}
				break;
				
			case "-o":
			case "--offset":
				if (i + 1 < args.size() && args.get(i + 1).matches("-?\\d{1,2}")) {
					i++;
					actions.add(setOffset(args.get(i)));
				} else {
					System.out.println("Offset needs a numeric offset");
					System.exit(1);
				}
				break;
				
			case "-d":
			case "--dst":
				if (i + 1 < args.size() && !args.get(i + 1).contains("-")) {
					i++;
					actions.add(setDst(args.get(i)));
				}
				break;
			case "-p":
			case "--port":
				if (i + 1 < args.size() && !args.get(i + 1).startsWith("-")) {
					i++;
					portIdentifier = args.get(i);
				} else {
					System.out.println("Invalid port identifier");
					System.exit(1);
				}
				break;
			}
		}
		return actions;
	}

	private static Action setTime(String timeStr) {
		long time = System.currentTimeMillis() / 1000;
		if (timeStr != null) {
			if (timeStr.matches("\\d{10}")) {
				time = Long.parseLong(timeStr);
			} else {
				SimpleDateFormat df = new SimpleDateFormat();
				df.applyPattern("yyyy-MM-dd HH:mm:ss");
				try {
					time = df.parse(timeStr).getTime() / 1000;
				} catch (ParseException e) {
					System.out.println("Invalid date format: " + timeStr);
					System.out.println("Use plain seconds since epoch or a ISO 8601 Date without milliseconds, like \"2015-10-27 18:43:25\"");
					System.exit(1);
				}
			}
		}
		
		return new Action("T" + time);
	}
	
	private static Action setOffset(String offsetStr) {
		int offset = 0;
		try {
			offset = Integer.parseInt(offsetStr);
		} catch (NumberFormatException e) {
			System.out.println("Invalid offset: " + offsetStr);
			System.exit(1);
		}
		if (offset < -12 || offset > 12) {
			System.out.println("Invalid offset: " + offset + " - must be between -12 and 12");
			System.exit(1);
		}
		
		return new Action("O" + offset);
	}

	private static Action setDst(String setting) {
		if (setting.equalsIgnoreCase("on")) {
			return new Action("DON");
		} else if (setting.equalsIgnoreCase("off")) {
			return new Action("DOFF");
		} else {
			System.out.println("Invalid option for daylight saving time: " + setting + " - only \"on\" and \"off\" are allowed");
			System.exit(1);
			return null;
		}
	}

	private static void printHelp() {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(Main.class.getResourceAsStream("/help.txt")))) {
			String line;
			while ((line = reader.readLine()) != null) {
				System.out.println(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
}
