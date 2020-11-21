package no.ion.jake.container;

import no.ion.jake.java.ClassPath;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.function.Predicate;

public class ContainerClassLoader extends URLClassLoader {
    static {
        if (!ClassLoader.registerAsParallelCapable()) {
            throw new InternalError();
        }
    }

    private final Predicate<String> useShared;
    private final DelegatingClassLoader sharedClassLoader;

    public ContainerClassLoader(String name,
                                ClassLoader parent,
                                ClassPath classPath,
                                Predicate<String> useShared,
                                ClassLoader sharedClassLoader) {
        super(name + "-loader", classPath.toUrls(), parent);
        this.useShared = useShared;
        this.sharedClassLoader = new DelegatingClassLoader(sharedClassLoader);
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);
            if (c != null) {
                return c;
            }

            if (useShared.test(name)) {
                return sharedClassLoader.loadClass(name, resolve);
            } else {
                return super.loadClass(name, resolve);
            }
        }
    }

    @Override
    public URL findResource(String name) {
        if (useShared.test(name)) {
            return sharedClassLoader.findResource(name);
        } else {
            return super.findResource(name);
        }
    }

    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        if (useShared.test(name)) {
            return sharedClassLoader.findResources(name);
        } else {
            return super.findResources(name);
        }
    }
}
