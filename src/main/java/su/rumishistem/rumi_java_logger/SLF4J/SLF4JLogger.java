package su.rumishistem.rumi_java_logger.SLF4J;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.LegacyAbstractLogger;
import org.slf4j.helpers.MessageFormatter;

import su.rumishistem.rumi_java_logger.FacilityCode;
import su.rumishistem.rumi_java_logger.RumiJavaLogger;
import su.rumishistem.rumi_java_logger.SeverityLevel;

public class SLF4JLogger extends LegacyAbstractLogger {
	private final String name;

	public SLF4JLogger(String name) {
		this.name = name;
	}

	@Override public boolean isTraceEnabled() { return false; }
	@Override public boolean isDebugEnabled() { return false; }
	@Override public boolean isInfoEnabled() { return true; }
	@Override public boolean isWarnEnabled() { return true; }
	@Override public boolean isErrorEnabled() { return true; }

	@Override
	protected String getFullyQualifiedCallerName() {
		return null;
	}

	@Override
	protected void handleNormalizedLoggingCall(Level level, Marker marker, String pattern, Object[] args, Throwable t) {
		String msg = MessageFormatter.basicArrayFormat(pattern, args);
		if (t != null) {
			StringWriter sw = new StringWriter();
			t.printStackTrace(new PrintWriter(sw));
			msg = msg + "\n" + sw;
		}

		SeverityLevel sev = switch (level) {
			case ERROR -> SeverityLevel.Error;
			case WARN -> SeverityLevel.Warning;
			default -> SeverityLevel.Informational;
		};

		RumiJavaLogger.get_instance().log(FacilityCode.User, sev, "[" + name + "] " + msg);
	}
}
