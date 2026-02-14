package su.rumishistem.rumi_java_logger;

public enum SeverityLevel {
	/**
	 * システム使用不可、パニック状態
	 */
	Emergency(0),

	/**
	 * 早急な対処が必要、システムのデータベースが破損しているなどすぐさま修正すべき状態
	 */
	Alert(1),

	/**
	 * 致命的な状態
	 */
	Critical(2),

	/**
	 * エラー状態
	 */
	Error(3),

	/**
	 * 警告状態
	 */
	Warning(4),

	/**
	 * 正常だから注意が必要な状態
	 */
	Notice(5),

	/**
	 * お知らせ
	 */
	Informational(6),
	
	/**
	 * デバッグ
	 */
	Debug(7),

	/**
	 * 完了
	 */
	Ok(8);

	private final int code;
	SeverityLevel(int code) {
		this.code = code;
	}

	public int to_code() {
		return code;
	}
}
