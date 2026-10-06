package su.rumishistem.rumi_java_logger;

public class Main {
	public static void main(String[] args) throws InterruptedException{
		RumiJavaLogger logger = new RumiJavaLogger();
		logger.set_syslog_server("192.168.100.120");

		logger.print(SeverityLevel.Ok, "完了");
		logger.print(SeverityLevel.Error, "エラー");
		logger.print(SeverityLevel.Warning, "警告");
		logger.print(SeverityLevel.Informational, "お知らせ");
		logger.print(SeverityLevel.Debug, "デバッグ");

		logger.close();
	}
}
