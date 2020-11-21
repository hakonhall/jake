package no.ion.jake.container;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

/**
 * Needed to make protected methods accessible to ContainerClassLoader implementation.
 */
public class DelegatingClassLoader extends ClassLoader {
    public DelegatingClassLoader(ClassLoader delegate) {
        super(delegate);
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return super.loadClass(name, resolve);
    }

    @Override
    public URL findResource(String name) {
        return super.findResource(name);
    }

    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        return super.findResources(name);
    }
}
