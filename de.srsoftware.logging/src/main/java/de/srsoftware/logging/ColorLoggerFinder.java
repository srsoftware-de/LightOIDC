/* © SRSoftware 2024 */
package de.srsoftware.logging;

import java.util.HashMap;
import java.util.Map;

public class ColorLoggerFinder extends System.LoggerFinder {
	private static final Map<String, ColorLogger> LOGGERS = new HashMap<>();

	@Override
	public System.Logger getLogger(String name, Module module) {
		return LOGGERS.computeIfAbsent(name, ColorLogger::new);
	}
}
