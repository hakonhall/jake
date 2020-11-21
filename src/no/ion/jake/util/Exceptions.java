package no.ion.jake.util;

import java.io.IOException;
import java.io.UncheckedIOException;

public class Exceptions {
    @FunctionalInterface
    public interface IOThrowingSupplier<T> {
        T get() throws IOException;
    }

    public static <T> T uncheckIO(IOThrowingSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @FunctionalInterface
    public interface IOThrowingRunnable {
        void run() throws IOException;
    }

    public static void uncheckIO(IOThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
