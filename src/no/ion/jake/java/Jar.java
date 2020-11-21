package no.ion.jake.java;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.spi.ToolProvider;

public class Jar {
    private final java.util.spi.ToolProvider jarTool;

    public Jar() {
        this.jarTool = java.util.spi.ToolProvider.findFirst("jar")
                .orElseThrow(() -> new IllegalStateException("no jar tool available"));
    }

    public Result run(List<String> arguments) {
        StringWriter outputStringWriter = new StringWriter();
        PrintWriter outputPrintWriter = new PrintWriter(outputStringWriter);
        String[] argumentsArray = arguments.toArray(String[]::new);

        int code = jarTool.run(outputPrintWriter, outputPrintWriter, argumentsArray);

        outputPrintWriter.flush();
        return new Result(code, outputStringWriter.toString());
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
}
