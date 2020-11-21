package no.ion.jake.javadoc;

import javax.tools.DocumentationTool;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class Javadoc {
    private final DocumentationTool javadoc;

    public Javadoc() {
        this.javadoc = ToolProvider.getSystemDocumentationTool();
        if (javadoc == null) {
            throw new IllegalStateException("no javadoc tool available");
        }
    }

    public static class Result {
        private final int code;
        private final String out;

        public Result(int code, String out) {
            this.code = code;
            this.out = out;
        }

        public int code() { return code; }
        public String out() { return out; }
    }

    public Result run(List<String> arguments) {
        var outStream = new ByteArrayOutputStream();
        int code = javadoc.run(InputStream.nullInputStream(), outStream, outStream, arguments.toArray(String[]::new));
        return new Result(code, outStream.toString(StandardCharsets.UTF_8));
    }
}
