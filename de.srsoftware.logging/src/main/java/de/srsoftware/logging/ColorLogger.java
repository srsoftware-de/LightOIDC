/* © SRSoftware 2024 */
package de.srsoftware.logging;

import static de.srsoftware.logging.ConsoleColors.*;
import static java.lang.System.Logger.Level.*;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;

public class ColorLogger implements System.Logger {
	private final String      name;
	private static int        rootLevel = INFO.getSeverity();
	private static DateFormat TIME	    = new SimpleDateFormat("hh:mm:ss.SSS");
	private static DateFormat DATE	    = new SimpleDateFormat("yyyy-MM-dd");
	private static String     lastDate  = null;

	public ColorLogger(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isLoggable(Level level) {
		return level.getSeverity() >= rootLevel;
	}

	@Override
	public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) {
		if (isLoggable(level)) {
			System.out.println(colorize(msg, level.getSeverity()));
			thrown.printStackTrace();
		}
	}

	@Override
	public void log(Level level, ResourceBundle bundle, String format, Object... params) {
		if (isLoggable(level)) {
			System.out.println(colorize(MessageFormat.format(format, params), level.getSeverity()));
		}
	}

	public ColorLogger setLogLevel(Level level) {
		rootLevel = level.getSeverity();
		return this;
	}

	private static String colorize(String message, int severity) {
		var           color = severity >= ERROR.getSeverity() ? RED : severity >= WARNING.getSeverity() ? YELLOW : severity >= INFO.getSeverity() ? WHITE_BRIGHT : WHITE;
		var           date  = new Date();
		var           day   = DATE.format(date);
		StringBuilder sb    = new StringBuilder();
		if (!day.equals(lastDate)) {
			lastDate = day;
			sb.append(WHITE).append(day).append("\n");
		}
		return sb.append(WHITE).append(TIME.format(date)).append(" ").append(color).append(message).append(RESET).toString();
	}
}
