import java.lang.reflect.*;
import java.util.*;

public class DependencyContainer {
    private static final Map<Class<?>, Object> objects = new HashMap<>();

    public static <T> void addInstance(Class<?> klass, T instance) {
        if (klass.getAnnotation(Component.class) == null) {
            throw new IllegalArgumentException(klass + " does not have Component attribute.");
        }

        objects.put(klass, instance);
    }

    public static <T> void addInstance(T instance) {
        addInstance(instance.getClass(), instance);
    }

    public static <T> T getInstance(Class<T> klass) {
        if (klass.getAnnotation(Component.class) == null) {
            throw new IllegalArgumentException(klass + " does not have Component attribute.");
        }

        Object object = objects.get(klass);
        if (object != null) {
            return klass.cast(object);
        }

        final T instance = instantiate(klass);
        objects.put(klass, instance);
        return instance;
    }

    private static <T> T instantiate(Class<T> klass) {
        final Constructor<?> constructor = findSuitableConstructor(klass);

        final Parameter[] parameters = constructor.getParameters();
        final Object[] arguments = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            try {
                arguments[i] = getInstance(parameters[i].getType());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unable to find dependency for " + klass + ".", e);
            }
        }

        try {
            final T instance = klass.cast(constructor.newInstance(arguments));

            Arrays.stream(klass.getMethods())
                    .filter(method -> method.getAnnotation(PostConstructor.class) != null)
                    .forEach(method -> {
                        try {
                            method.invoke(instance);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(
                                    "Failed to call " + method.getName() + " post-constructor method on " +
                                    klass + " instance.", e);
                        }
                    });

            return instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to construct " + klass + " instance.", e);
        }
    }

    private static Constructor<?> findSuitableConstructor(Class<?> klass) {
        final Constructor<?>[] constructors = klass.getConstructors();
        if (constructors.length == 0) {
            throw new IllegalArgumentException("No public constructors in " + klass + ".");
        }

        for (Constructor<?> constructor : constructors) {
            if (constructor.getAnnotation(Autowired.class) != null) {
                return constructor;
            }
        }

        return constructors[0];
    }
}
