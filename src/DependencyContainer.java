import java.lang.reflect.*;
import java.util.*;

public class DependencyContainer {
    /**
     * Звено в списке компонентов с одним типом, но разными квалификаторами.
     */
    private static class NamedComponentNode {
        final String qualifier;
        /**
         * Если null, - данный компонент в стадии создания.
         */
        Object component;
        NamedComponentNode next = null;

        NamedComponentNode(String qualifier, Object component) {
            this.qualifier = qualifier;
            this.component = component;
        }
    }

    /**
     * Сопоставляет типу компонент, или список компонентов, подставляемый при разрешении зависимостей.
     */
    private static final Map<Class<?>, NamedComponentNode> components = new HashMap<>();

    public static <T> void addComponent(Class<?> klass, String qualifier, T component) {
        if (component == null) {
            throw new IllegalArgumentException("Component can't be null.");
        }

        if (setComponentInstance(klass, qualifier, component)) {
            throw new IllegalArgumentException(
                "The container already contains a " + klass +
                " instance with qualifier '" + qualifier +"'.");
        }
    }

    public static <T> void addComponent(Class<?> klass, T component) {
        addComponent(klass, null, component);
    }

    public static <T> void addComponent(String qualifier, T component) {
        addComponent(component.getClass(), qualifier, component);
    }

    public static <T> void addComponent(T component) {
        addComponent(component.getClass(), null, component);
    }

    public static <T> T getComponent(Class<T> klass) {
        if (klass.getAnnotation(Component.class) == null) {
            throw new IllegalArgumentException(klass + " does not have a Component attribute.");
        }

        if (!components.containsKey(klass)) {
            setComponentInstance(klass, null, null);
            final T instance = instantiate(klass);
            setComponentInstance(klass, null, instance);
            return instance;
        }

        final NamedComponentNode node = components.get(klass);
        final Object component = node.component;
        if (component == null) {
            throw new IllegalArgumentException(
                    "Cyclic dependency detected: " + klass + " instance is currently being created.");
        }

        return klass.cast(component);
    }

    /**
     * Добавляет компонент (может быть null) в контейнер заменяя существующий, если такой уже есть.
     * @param klass Тип, который реализует добавляемый компонент.
     * @param qualifier Опциональный квалификатор компонента.
     * @param component Компонент или null.
     * @param <T> Тип компонента.
     * @return true, если такой компонент уже был в конейнере и был заменён, иначе - false.
     */
    private static <T> boolean setComponentInstance(Class<?> klass, String qualifier, T component) {
        if (klass.getAnnotation(Component.class) == null) {
            throw new IllegalArgumentException(klass + " does not have a Component attribute.");
        }

        if (component != null && !klass.isInstance(component)) {
            throw new IllegalArgumentException("Component must be a " + klass + " instance.");
        }

        if (components.containsKey(klass)) {
            NamedComponentNode node = components.get(klass);
            do {
                if (Objects.equals(qualifier, node.qualifier)) {
                    node.component = component;
                    return true;
                }

                node = node.next;
            } while (node.next != null);

            node.next = new NamedComponentNode(qualifier, component);
            return false;
        }

        components.put(klass, new NamedComponentNode(qualifier, component));
        return false;
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
