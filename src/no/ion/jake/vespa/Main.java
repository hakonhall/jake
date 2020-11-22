package no.ion.jake.vespa;

import no.ion.jake.AbortException;
import no.ion.jake.BuildContext;
import no.ion.jake.LogSink;
import no.ion.jake.module.ModuleContext;
import no.ion.jake.Project;
import no.ion.jake.java.Jar;
import no.ion.jake.java.Javac;
import no.ion.jake.javadoc.Javadoc;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

import static no.ion.jake.util.Exceptions.uncheckIO;

public class Main {
    private final Project project;
    private final boolean verbose;

    public static void main(String[] args) {
        try {
            System.exit(main2(args));
        } catch (UserError userError) {
            System.err.println(userError.getMessage());
            System.exit(1);
        } catch (RuntimeException e) {
            // This should never happen...
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static int main2(String[] args) {
        Path projectPath = null;
        boolean verbose = false;

        for (int i = 0; i < args.length; ++i) {
            String arg = args[i];
            switch (arg) {
                case "-p":
                case "--project":
                    projectPath = Path.of(args[++i]);
                    continue;
                case "-v":
                case "--verbose":
                    verbose = true;
                    continue;
            }

            if (arg.startsWith("-")) {
                throw new UserError("unknown option: " + arg);
            } else {
                throw new UserError("extraneous argument: " + arg);
            }
        }

        if (projectPath == null) {
            throw new UserError("missing required --project");
        } else if (!Files.isDirectory(projectPath)) {
            throw new UserError("no such directory: " + projectPath);
        }
        projectPath = projectPath.toAbsolutePath();
        Project project = new Project(projectPath);

        return new Main(project, verbose).run();
    }

    private Main(Project project, boolean verbose) {
        this.project = project;
        this.verbose = verbose;
    }

    private int run() {
        LogSink logSink = new PrintStreamLogSink(System.out, verbose ? Level.FINEST : Level.INFO);

        // Our program would like to print progress to System.out and never write to err.
        // Unfortunately, tests may print to out/err, and the JVM may print e.g. "WARNING: An illegal reflective
        // access operation has occurred" (even with --illegal-access=permit) to a saved copy of err.
        //
        // Therefore, we close err, and set out and err to a PrintStream that forwards to the log sink with a level
        // only visible with --verbose.
        //
        // To be able to see any stack trace thrown out of main(), or whatever we'd like to print above this stack frame,
        // we restore out and err to the original out when returning.
        PrintStream originalOut = System.out;
        StdoutStream stdoutStream = new StdoutStream(logSink, Level.FINER, StandardCharsets.UTF_8);
        PrintStream newOut = new PrintStream(stdoutStream);
        System.err.close();
        System.setOut(newOut);
        System.setErr(newOut);
        System.setIn(InputStream.nullInputStream());

        try {
            return runWithOutAndErrRedirected(logSink);
        } catch (AbortException e) {
            if (!e.getMessage().isEmpty()) {
                logSink.log(Level.SEVERE, e.getMessage(), null);
            }
            return 1;
        } catch (RuntimeException e) {
            logSink.log(Level.SEVERE, null, e);
            return 1;
        } finally {
            System.setOut(originalOut);
            System.setErr(originalOut);
        }
    }

    private int runWithOutAndErrRedirected(LogSink logSink) {
        Javac javac = new Javac();
        Jar jar = new Jar();
        Javadoc javadoc = new Javadoc();

        ModuleContext testutilContext = new ModuleContext(project, "testutil");
        TestutilModule testutil = new TestutilModule(testutilContext, javac, javadoc, jar);
        BuildContext testutilBuildContext = new BuildContext(testutilContext, logSink);
        testutil.build(testutilBuildContext);

        ModuleContext yoleanContext = new ModuleContext(project, "yolean");
        YoleanModule yolean = new YoleanModule(yoleanContext, javac, javadoc, jar);
        BuildContext yoleanBuildContext = new BuildContext(yoleanContext, logSink);
        yolean.build(yoleanBuildContext);

        ModuleContext vespajlibContext = new ModuleContext(project, "vespajlib");
        VespajlibModule vespajlib = new VespajlibModule(vespajlibContext, javac, jar, javadoc, testutil, yolean);
        BuildContext vespajlibBuildContext = new BuildContext(vespajlibContext, logSink);
        vespajlib.build(vespajlibBuildContext);

        logSink.log(Level.INFO, "SUCCESS", null);

        return 0;
    }

    private void setOutAndCloseErr(LogSink logSink) {
        StdoutStream stdoutStream = new StdoutStream(logSink, Level.FINER, StandardCharsets.UTF_8);
        PrintStream newStdout = new PrintStream(stdoutStream);
        // TODO: Hides "WARNING: An illegal reflective access operation has occurred", but is likely to have other bad side effects
        System.err.close();
        System.setOut(newStdout);
        System.setErr(newStdout);
        System.setIn(InputStream.nullInputStream());
    }

}
