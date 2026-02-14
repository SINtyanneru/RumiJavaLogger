package su.rumishistem.rumi_java_logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class RumiJavaLogger {
	private boolean save_log_disc_enable = false;
	private Path log_dir_path = null;

	private boolean syslog_enable = false;
	private List<String> syslog_host = new ArrayList<>();
	private static final int SYSLOG_PORT = 514;

	private boolean hijack_std_enable = false;
	private PrintStream stdout = null;
	private PrintStream stderr = null;

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
		stdout = System.out;
		stderr = System.err;
		hijack_std_enable = true;

		//出力
		PipedOutputStream out_pos = new PipedOutputStream();
		PipedInputStream out_pis = new PipedInputStream(out_pos);
		PrintStream out = new PrintStream(out_pos, true, "UTF-8");
		System.setOut(out);

		//エラー
		PipedOutputStream err_pos = new PipedOutputStream();
		PipedInputStream err_pis = new PipedInputStream(err_pos);
		PrintStream err = new PrintStream(err_pos, true, "UTF-8");
		System.setErr(err);

		Thread out_watcher = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					BufferedReader br = new BufferedReader(new InputStreamReader(out_pis, StandardCharsets.UTF_8));
					try {
						String line;
						while ((line = br.readLine()) != null) {
							stdout.println(line);
							log_print(FacilityCode.User, default_level, line);
						}
					} finally {
						br.close();
					}
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		});
		out_watcher.setDaemon(true);
		out_watcher.start();

		Thread err_watcher = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					BufferedReader br = new BufferedReader(new InputStreamReader(err_pis, StandardCharsets.UTF_8));
					try {
						String line;
						while ((line = br.readLine()) != null) {
							stderr.println(line);
							log_print(FacilityCode.User, SeverityLevel.Error, line);
						}
					} finally {
						br.close();
					}
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		});
		err_watcher.setDaemon(true);
		err_watcher.start();
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

	private void log_print(FacilityCode f, SeverityLevel level, String message) {
		String log_message = "";

		switch (level) {
			case Ok:
				log_message = "[  \u001B[32mOK\u001B[0m  ] "+message;
				break;
			case Debug:
				log_message = "[DEBUG ] "+message;
				break;
			case Notice:
			case Informational:
				log_message = "[ INFO ] "+message;
				break;
			case Warning:
				log_message = "[ \u001B[33mWARN\u001B[0m ] "+message;
				break;
			case Error:
			case Critical:
			case Alert:
			case Emergency:
				log_message = "[\u001B[31mFAILED\u001B[0m] "+message;
				break;
		}

		//コンソールに出力
		switch (level) {
			case Notice:
			case Informational:
			case Ok:
			case Debug:
				print_stdout(log_message);
				break;
			case Warning:
			case Error:
			case Critical:
			case Alert:
			case Emergency:
				print_stderr(log_message);
				break;
		}

		//SysLog
		if (syslog_enable) {
			int pri = f.to_code() * 8 + level.to_code();
			String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
			String app = "None";

			String header = "<"+pri+">1 "+timestamp+" "+app;
			byte[] data = (header+" "+message).getBytes(StandardCharsets.UTF_8);

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

	private void print_stdout(String message) {
		if (hijack_std_enable) {
			stdout.println(message);
		} else {
			System.out.println(message);
		}
	}

	private void print_stderr(String message) {
		if (hijack_std_enable) {
			stderr.println(message);
		} else {
			System.err.println(message);
		}
	}
}
