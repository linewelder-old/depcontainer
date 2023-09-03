import java.lang.reflect.*;
import java.util.*;

public class DependencyContainer {
    private static final Map<Class, Object> objects = new HashMap<>();

    public static <T> void add(Class<?> klass, T instance) {
        objects.put(klass, instance);
    }

    public static <T> void add(T instance) {
        add(instance.getClass(), instance);
    }

    public static <T> T get(Class<T> klass) {
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
                arguments[i] = get(parameters[i].getType());
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
                            throw new RuntimeException(e);
                        }
                    });

            return instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
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
