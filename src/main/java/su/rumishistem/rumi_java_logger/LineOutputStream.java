package su.rumishistem.rumi_java_logger;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class LineOutputStream extends OutputStream{
	private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	private final Consumer<String> handler;

	public LineOutputStream(Consumer<String> handler) {
		this.handler = handler;
	}

	@Override
	public synchronized void write(int b) {
		if (b == '\n') {
			flushLine();
		} else {
			buffer.write(b);
		}
	}

	private void flushLine() {
		String line = buffer.toString(StandardCharsets.UTF_8);
		buffer.reset();
		//CRLF対策
		if (line.endsWith("\r")) {
			line = line.substring(0, line.length() - 1);
		}
		handler.accept(line);
	}
}
