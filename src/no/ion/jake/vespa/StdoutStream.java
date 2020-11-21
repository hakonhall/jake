package no.ion.jake.vespa;

import no.ion.jake.LogSink;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.logging.Level;

public class StdoutStream extends OutputStream {
    private final ByteArrayOutputStream pendingBytes = new ByteArrayOutputStream();
    private final LogSink logSink;
    private final Level level;
    private final Charset charset;

    public StdoutStream(LogSink logSink, Level level, Charset charset) {
        this.logSink = logSink;
        this.level = level;
        this.charset = charset;
    }

    @Override
    public void write(int b) {
        if (!logSink.isEnabled(level)) return;
        internalWrite(b);
    }

    private void internalWrite(int b) {
        if (b == '\n') {
            writePendingBytes();
        } else {
            pendingBytes.write(b);
        }
    }

    private void writePendingBytes() {
        String string = pendingBytes.toString(charset);
        pendingBytes.reset();
        logSink.log(level, string, null);
    }

    @Override
    public void write(byte[] b) {
        if (!logSink.isEnabled(level)) return;
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        if (!logSink.isEnabled(level)) return;

        if (len <= 0) return;

        int startIndex = off;
        for (int i = startIndex; i < off + len; ++i) {
            if (b[i] == '\n') {
                pendingBytes.write(b, startIndex, i - startIndex);
                writePendingBytes();
                startIndex = i + 1;
            }
        }

        if (startIndex < off + len) {
            pendingBytes.write(b, startIndex, off + len - startIndex);
        }
    }

    @Override
    public void flush() {
        // I refuse to flush bytes lacking a newline
    }

    @Override
    public void close() {
        pendingBytes.reset();
    }
}
