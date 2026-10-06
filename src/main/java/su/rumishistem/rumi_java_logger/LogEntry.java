package su.rumishistem.rumi_java_logger;

public record LogEntry(
	FacilityCode facility,
	SeverityLevel severity_level,
	String text
) {
}
