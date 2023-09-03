import java.lang.reflect.*;
import java.util.*;

public class DependencyContainer {
    /**
     * Сопоставляет тип с компонентами этого типа.
     * Если компонент в стадии создания, то типу сопоставлен null для обнаружения цикличных зависимостей.
     */
    private static final Map<Class<?>, Object> components = new HashMap<>();

    public static <T> void addComponent(Class<?> klass, T component) {
        if (klass.getAnnotation(Component.class) == null) {
            throw new IllegalArgumentException(klass + " does not have a Component attribute.");
        }

        if (components.containsKey(klass)) {
            throw new IllegalArgumentException("The container already contains a " + klass + " instance.");
        }

        components.put(klass, component);
    }

    public static <T> void addComponent(T component) {
        addComponent(component.getClass(), component);
    }

    public static <T> T getComponent(Class<T> klass) {
        if (klass.getAnnotation(Component.class) == null) {
            throw new IllegalArgumentException(klass + " does not have a Component attribute.");
        }

        if (!components.containsKey(klass)) {
            components.put(klass, null);
            final T instance = instantiate(klass);
            components.put(klass, instance);
            return instance;
        }

        final Object object = components.get(klass);
        if (object == null) {
            throw new IllegalArgumentException(
                    "Cyclic dependency detected: " + klass + " instance is currently being created.");
        }

        return klass.cast(object);
    }

    private static <T> T instantiate(Class<T> klass) {
        final Constructor<?> constructor = findSuitableConstructor(klass);

        final Parameter[] parameters = constructor.getParameters();
        final Object[] arguments = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            try {
                arguments[i] = getComponent(parameters[i].getType());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unable to resolve dependencies for " + klass + ".", e);
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
            throw new RuntimeException("Failed to call " + constructor + ".", e);
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
