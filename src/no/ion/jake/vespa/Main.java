package no.ion.jake.vespa;

import no.ion.jake.AbortException;
import no.ion.jake.LogSink;
import no.ion.jake.Project;
import no.ion.jake.build.Build;
import no.ion.jake.build.ModuleContext;
import no.ion.jake.engine.BuildSet;
import no.ion.jake.engine.DeclaratorImpl;
import no.ion.jake.engine.JakeExecutor;
import no.ion.jake.java.Jar;
import no.ion.jake.java.Javac;
import no.ion.jake.javadoc.Javadoc;
import no.ion.jake.maven.MavenCentral;
import no.ion.jake.maven.MavenRepository;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

public class Main {
    private final Project project;
    private final Options options;

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
        Options options = new Options();

        for (int i = 0; i < args.length; ++i) {
            String arg = args[i];
            switch (arg) {
                case "--jar":
                    options.setJarPath(Path.of(args[++i]));
                    continue;
                case "--dep-graph":
                    options.setDotPath(Path.of(args[++i]));
                    options.setMode(Options.Mode.GRAPHVIZ);
                    continue;
                case "--dot":
                    options.setDotPath(Path.of(args[++i]));
                    continue;
                case "-p":
                case "--project":
                    options.setProjectPath(Path.of(args[++i]));
                    continue;
                case "-T":
                case "--threads":
                    arg = args[++i];
                    if (arg.endsWith("C")) {
                        options.setThreadsPerHardwareThread(Float.parseFloat(arg.substring(0, arg.length() - 1)));
                    } else {
                        options.setThreads(Float.parseFloat(arg));
                    }
                    if (options.threads() < 0f) {
                        throw new UserError("negative #threads specified");
                    }
                    continue;
                case "--time":
                    options.setLogTime(true);
                    continue;
                case "-v":
                case "--verbose":
                    options.setVerbose(true);
                    continue;
            }

            if (arg.startsWith("-")) {
                throw new UserError("unknown option: " + arg);
            } else {
                throw new UserError("extraneous argument: " + arg);
            }
        }

        options.validateAndNormalize();
        Project project = new Project(options.projectPath(), options.jarPath());

        return new Main(project, options).run();
    }

    private Main(Project project, Options options) {
        this.project = project;
        this.options = options;
    }

    private int run() {
        LogSink logSink = new PrintStreamLogSink(System.out, options.verbose() ? Level.FINEST : Level.INFO, options.logTime());

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
        MavenCentral mavenCentral = new MavenCentral();
        MavenRepository mavenRepository = new MavenRepository(project.pathToMavenRepository(), mavenCentral);
        var executor = new JakeExecutor(options.threads());
        var buildSet = new BuildSet(executor, logSink);

        final TestutilModule testutilModule;
        {
            var testutilContext2 = new ModuleContext(project, project.path().resolve("testutil").normalize());
            testutilModule = new TestutilModule(testutilContext2, mavenRepository, javac, jar, javadoc);
            var testutilConstruction = new DeclaratorImpl(buildSet, testutilContext2, testutilModule);
            testutilModule.declareBuilds(testutilConstruction);
        }

        final YoleanModule yoleanModule;
        {
            var yoleanContext2 = new ModuleContext(project, project.path().resolve("yolean").normalize());
            yoleanModule = new YoleanModule(yoleanContext2, mavenRepository, javac, jar, javadoc);
            var yoleanConstruction = new DeclaratorImpl(buildSet, yoleanContext2, yoleanModule);
            yoleanModule.declareBuilds(yoleanConstruction);
        }

        final VespajlibModule vespajlibModule;
        {
            var vespajlibContext2 = new ModuleContext(project, project.path().resolve("vespajlib").normalize());
            vespajlibModule = new VespajlibModule(vespajlibContext2, mavenRepository, javac, jar, javadoc,
                    testutilModule, yoleanModule);
            var vespajlibConstruction = new DeclaratorImpl(buildSet, vespajlibContext2, vespajlibModule);
            vespajlibModule.declareBuilds(vespajlibConstruction);
        }

        switch (options.mode()) {
            case BUILD:
                buildSet.buildEverything();
                logSink.log(Level.INFO, "SUCCESS", null);
                break;
            case GRAPHVIZ:
                buildSet.printGraphviz(options.dotPath());
                break;
        }

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
