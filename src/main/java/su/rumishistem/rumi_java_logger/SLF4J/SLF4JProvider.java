package su.rumishistem.rumi_java_logger.SLF4J;

import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.helpers.BasicMDCAdapter;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

public class SLF4JProvider implements SLF4JServiceProvider {
	private ILoggerFactory logger_factory;
	private IMarkerFactory marker_factory;
	private MDCAdapter mdc_adapter;

	@Override public void initialize() {
		logger_factory = SLF4JLogger::new;
		marker_factory = new BasicMarkerFactory();
		mdc_adapter = new BasicMDCAdapter();
	}
	@Override public ILoggerFactory getLoggerFactory() { return logger_factory; }
	@Override public IMarkerFactory getMarkerFactory() { return marker_factory; }
	@Override public MDCAdapter getMDCAdapter() { return mdc_adapter; }
	@Override public String getRequestedApiVersion() { return "2.0.99"; }
}
