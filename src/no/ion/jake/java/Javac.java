package no.ion.jake.java;

import no.ion.jake.UserError;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Wraps {@link javax.tools.JavaCompiler}
 *
 * <p>Additional info may be retrieved: "Such tools should instead use the javax.tools, javax.lang.model, and
 * com.sun.source.* APIs, available since JDK 6</p>
 */
public class Javac {
    private final javax.tools.JavaCompiler javaCompiler;

    public Javac() {
        javaCompiler = javax.tools.ToolProvider.getSystemJavaCompiler();
        if (javaCompiler == null) {
            throw new UserError("java compiler not available");
        }
    }

    public static class CompileResult {
        final int code;
        final String message;

        CompileResult(int code, String message) {
            this.code = code;
            this.message = message;
        }
    }

    public CompileResult compile(List<String> commandLine) {
        var outStream = new ByteArrayOutputStream();
        int code = javaCompiler.run(InputStream.nullInputStream(), outStream, outStream, commandLine.toArray(String[]::new));
        return new CompileResult(code, outStream.toString(StandardCharsets.UTF_8));
    }
}
