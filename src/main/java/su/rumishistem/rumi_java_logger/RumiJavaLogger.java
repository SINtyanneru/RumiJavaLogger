package su.rumishistem.rumi_java_logger;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class RumiJavaLogger {
	private boolean save_log_disc_enable = false;
	private Path log_dir_path = null;

	private boolean syslog_enable = false;
	private List<String> syslog_host = new ArrayList<>();
	private static final int SYSLOG_PORT = 514;

	private boolean hijack_std_enable = false;
	private PrintStream stdout = null;
	private PrintStream stderr = null;

	private LinkedBlockingQueue<LogEntry> log_queue = new LinkedBlockingQueue<>(10000);
	private volatile boolean running = true;
	private Thread log_worker;

	public RumiJavaLogger() {
		log_worker = new Thread(new Runnable() {
			@Override
			public void run() {
				while (running || !log_queue.isEmpty()) {
					try {
						LogEntry e = log_queue.take();
						logentry_print(e);
					} catch (InterruptedException e) {
						return;
					} catch (Exception ex) {
						ex.printStackTrace();
						//無視
					} catch (Throwable tw) {
						tw.printStackTrace();
						//無視
					}
				}
			}
		});
		//log_worker.setDaemon(true);
		log_worker.start();
	}

	/**
	 * ログをディスクに保存する設定をします。
	 * @param path ディレクトリパス
	 */
	public void save_log_disc(String path) {
		log_dir_path = Path.of(path);
		save_log_disc_enable = true;

		//ログディレクトリを作成
		File log_dir = new File(path);
		if (!log_dir.exists()) log_dir.mkdir();
	}

	/**
	 * SysLogサーバーを設定します。
	 * 
	 * デフォルトポートは514です。
	 * 変更時にはset_syslog_port関数を使用してください。
	 * @param host ホスト
	 */
	public void set_syslog_server(String host) {
		this.syslog_enable = true;
		this.syslog_host.add(host);
	}

	/**
	 * 標準出力/標準エラー出力を傍受します
	 * @param default_level 標準出力の重要度レベルです。
	 * @throws IOException
	 */
	public void hijack_std(SeverityLevel default_level) throws IOException{
		//すでにハイジャック済みなら何もしない
		if (hijack_std_enable) return;

		//RumiJavaLoggerだけは直接読み書きできるようにする
		stdout = System.out;
		stderr = System.err;
		hijack_std_enable = true;

		//標準出力をハイジャック
		PrintStream stdout_hooked = new PrintStream(new LineOutputStream(line -> {
			log_print(FacilityCode.User, default_level, line);
		}), true, StandardCharsets.UTF_8);
		System.setOut(stdout_hooked);

		//標準エラーをハイジャック
		PrintStream stderr_hooked = new PrintStream(new LineOutputStream(line -> {
			log_print(FacilityCode.User, SeverityLevel.Error, line);
		}), true, StandardCharsets.UTF_8);
		System.setErr(stderr_hooked);
	}

	/**
	 * ログを出力します
	 * @param f コード
	 * @param level レベル
	 * @param message メッセージ
	 */
	public void log(FacilityCode f, SeverityLevel level, String message) {
		log_print(f, level, message);
	}

	/**
	 * Userとしてログを出力します
	 * @param level レベル
	 * @param message メッセージ
	 */
	public void print(SeverityLevel level, String message) {
		log(FacilityCode.User, level, message);
	}

	/**
	 * Exceptionを標準エラーへ流します。
	 */
	public void print_exception(Exception ex) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		ex.printStackTrace(pw);
		String stack_trace = sw.toString();
		pw.close();

		log_print(FacilityCode.User, SeverityLevel.Error, stack_trace);
	}

	/**
	 * Throwableを標準エラーへ流します。
	 */
	public void print_throwable(Throwable ex) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		ex.printStackTrace(pw);
		String stack_trace = sw.toString();
		pw.close();

		log_print(FacilityCode.User, SeverityLevel.Error, stack_trace);
	}

	/**
	 * ロガーを終了します。
	 */
	public void close() throws InterruptedException {
		running = false;
		log_worker.join();
	}

	private void log_print(FacilityCode f, SeverityLevel level, String message) {
		log_queue.offer(new LogEntry(f, level, message));
	}

	private void logentry_print(LogEntry e) {
		String log_message = "";

		switch (e.severity_level()) {
			case Ok:
				log_message = "[  \u001B[32mOK\u001B[0m  ] "+e.text();
				break;
			case Debug:
				log_message = "[DEBUG ] "+e.text();
				break;
			case Notice:
			case Informational:
				log_message = "[ INFO ] "+e.text();
				break;
			case Warning:
				log_message = "[ \u001B[33mWARN\u001B[0m ] "+e.text();
				break;
			case Error:
			case Critical:
			case Alert:
			case Emergency:
				log_message = "[\u001B[31mFAILED\u001B[0m] "+e.text();
				break;
		}

		//コンソールに出力
		switch (e.severity_level()) {
			case Notice:
			case Informational:
			case Ok:
			case Debug:
				if (hijack_std_enable) {
					stdout.println(log_message);
				} else {
					System.out.println(log_message);
				}
				break;
			case Warning:
			case Error:
			case Critical:
			case Alert:
			case Emergency:
				if (hijack_std_enable) {
					stderr.println(log_message);
				} else {
					System.err.println(log_message);
				}
				break;
		}

		//SysLog
		if (syslog_enable) {
			int pri = e.facility().to_code() * 8 + e.severity_level().to_code();
			String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
			String app = "None";

			String header = "<"+pri+">1 "+timestamp+" "+app;
			byte[] data = (header+" "+e.text()).getBytes(StandardCharsets.UTF_8);

			try {
				DatagramSocket udp = new DatagramSocket();
				for (String host:syslog_host) {
					DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(host), SYSLOG_PORT);
					udp.send(packet);
				}
				udp.close();
			} catch (Exception ex) {
			}
		}

		//ログをディスクに保存
		if (save_log_disc_enable) {
			try {
				String file_name = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".txt";
				Files.writeString(log_dir_path.resolve(file_name), "[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm.ss")) + "]" + log_message + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			} catch (IOException ex) {
			}
		}
	}
}
