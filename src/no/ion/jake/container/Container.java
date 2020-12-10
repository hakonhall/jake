package no.ion.jake.container;

import no.ion.jake.java.ClassPath;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.function.Predicate;

public class Container {
    private final String name;
    private final ContainerClassLoader classLoader;

    public static <T> T of(Class<T> api,
                           ClassPath classPath,
                           Predicate<String> shareClass,
                           String className) {
        return null;
    }

    /**
     * Creates a container that allows Java code execution as-if running with the provided class path.
     * Specifically, a qualified class name is resolved as follows:
     *
     * <ol>
     *     <li>If {@code shareClassLoader} evaluates to true for the class name, the class is loaded with the same
     *     class loader as this {@code Container} class.</li>
     *     <li>Otherwise, the class name is loaded with the platform class loader {@link ClassLoader#getPlatformClassLoader()}.
     *     If NOT found, the {@code classPath} is searched in order and loaded from the first match.  If NOT found,
     *     loading fails.</li>
     * </ol>
     *
     * @param name             the name of the container
     * @param classPath        the effective class path to use while running {@code staticMethod}
     * @param shareClassLoader whether to delegate to this Container class' class loader, when loading a class
     *                         whose qualified class name is passed to the predicate.  For instance, if T is a type
     *                         not defined entirely by the system class loader, then the predicate must return true
     *                         for at least all class names that may be accessed by the caller.  The same goes for
     *                         types in {@code arguments}.
     * @return the newly created container
     */
    public static Container create(String name,
                                   ClassPath classPath,
                                   Predicate<String> shareClassLoader) {
        var containerClassLoader = new ContainerClassLoader(
                name,
                ClassLoader.getPlatformClassLoader(),
                classPath,
                shareClassLoader,
                Container.class.getClassLoader());
        return new Container(name, containerClassLoader);
    }

    private Container(String name, ContainerClassLoader classLoader) {
        this.name = name;
        this.classLoader = classLoader;
    }

    /**
     * Invoke a static method of the fully qualified class {@code className} with the given arguments,
     * and returning the type {@code T}.  The class space of that method invocation will be as-if the class path
     * is given by {@code classPath}, except that
     *
     * <ol>
     *     <li>the system class loader will always get a chance to resolve classes first</li>
     *     <li>if {@code shareClassLoader} is true, when provided a qualified class name to be loaded, then
     *     {@code Container.getClassLoader()} will be used to load the class instead.</li>
     * </ol>
     *
     * @param <T>              the type returned from {@code staticMethod}
     * @return an instance cast to the type {@code returnClass}
     */
    public <T> T invoke(Class<? extends T> returnClass, String className,
                        String staticMethod, Class<?>[] parameterTypes, Object... arguments) {
        Class<?> bootstrapClass;
        try {
            bootstrapClass = classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            NoClassDefFoundError error = new NoClassDefFoundError(className + " not found");
            error.initCause(e);
            throw error;
        }

        int classModifiers = bootstrapClass.getModifiers();
        if (!Modifier.isPublic(classModifiers)) {
            throw new IllegalAccessError("The class " + className + " is not public");
        }

        var foo = bootstrapClass.getDeclaredMethods();

        Method method;
        try {
            method = bootstrapClass.getDeclaredMethod(staticMethod, parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("No such method " + staticMethod + " on " + className);
        }

        int methodModifiers = method.getModifiers();
        if (!Modifier.isPublic(methodModifiers) || !Modifier.isStatic(methodModifiers) || method.getReturnType() != returnClass) {
            throw new IllegalArgumentException("method is not public static, returning returnClass: " + staticMethod);
        }

        Thread currentThread = Thread.currentThread();
        final ClassLoader savedContextClassLoader = currentThread.getContextClassLoader();
        currentThread.setContextClassLoader(classLoader);

        Object result;
        try {
            result = method.invoke(null, arguments);
        } catch (IllegalAccessException e) {
            IllegalAccessError error = new IllegalAccessError("Failed to access " + staticMethod);
            error.initCause(e);
            throw error;
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }

            throw new UndeclaredThrowableException(e.getCause());
        } finally {
            currentThread.setContextClassLoader(savedContextClassLoader);
        }

        // This throws a ClassCastException if result is non-null and not assignable to the returnClass type.
        return returnClass.cast(result);
    }
}
