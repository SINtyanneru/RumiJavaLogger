package su.rumishistem.rumi_java_logger;

public enum FacilityCode {
	Kernel(0),
	User(1),
	Mail(2),
	Daemon(3),
	Auth(4),
	Secure(13),
	Syslog(5);

	private final int code;
	FacilityCode(int code) {
		this.code = code;
	}

	public int to_code() {
		return code;
	}
}
