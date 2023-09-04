package linewelder.depcontainer;

import java.lang.reflect.*;
import java.util.*;

/**
 * Создаёт по мере востребования и потом хранит объекты классов, обозначенных аннотацией {@link Component}.
 * Автоматически подставляет нужные аргументы в конструкторы при их создании (dependency injection).
 */
public class DependencyContainer {
    /**
     * Звено в списке компонентов с одним типом, но разными квалификаторами.
     */
    private static class NamedComponentNode {
        final String qualifier;
        /**
         * Если null, — данный компонент в стадии создания.
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

    /**
     * Добавляет компонент в контейнер. Кидает исключение если компонент с таким типом и квалификатором уже есть.
     * @param klass Тип компонента.
     * @param qualifier Квалификатор компонента. Если null, то кидает исключение,
     *                  если есть хотя бы один компонент такого типа в контейнере.
     * @param component Компонент для добавления.
     * @param <T> Тип компонента.
     */
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

    /**
     * Добавляет компонент в контейнер. Кидает исключение если уже есть хотя бы один компонент с таким типом.
     * @param klass Тип компонента.
     * @param component Компонент для добавления.
     * @param <T> Тип компонента.
     */
    public static <T> void addComponent(Class<?> klass, T component) {
        addComponent(klass, null, component);
    }

    /**
     * Добавляет компонент в контейнер. Кидает исключение если компонент с таким типом и квалификатором уже есть.
     * Как тип компонента используется {@code component.getClass()}.
     * @param qualifier Квалификатор компонента. Если null, то кидает исключение,
     *                  если есть хотя бы один компонент такого типа в контейнере.
     * @param component Компонент для добавления.
     * @param <T> Тип компонента.
     */
    public static <T> void addComponent(String qualifier, T component) {
        addComponent(component.getClass(), qualifier, component);
    }

    /**
     * Добавляет компонент в контейнер. Кидает исключение если уже есть хотя бы один компонент с таким типом.
     * Как тип компонента используется {@code component.getClass()}.
     * @param component Компонент для добавления.
     * @param <T> Тип компонента.
     */
    public static <T> void addComponent(T component) {
        addComponent(component.getClass(), null, component);
    }

    /**
     * Достаёт из контейнера или создаёт новый компонент данного типа и с данным квалификатором.
     * Если компонент в процессе создания - кидает исключение для избежания цикличных зависимостей.
     * @param klass Тип компонента.
     * @param qualifier Если null, то достаёт первый попавшийся компонент из контейнера. Иначе - находит компонент
     *                  с таким квалификатором.
     * @return Найденный или новосозданный компонент.
     * @param <T> Тип компонента.
     */
    public static <T> T getComponent(Class<T> klass, String qualifier) {
        if (klass.getAnnotation(Component.class) == null) {
            throw new IllegalArgumentException(klass + " does not have a Component attribute.");
        }

        final Object component = findComponent(klass, qualifier);
        if (component == null) {
            setComponentInstance(klass, qualifier, null);
            final T instance = instantiate(klass);
            setComponentInstance(klass, qualifier, instance);
            return instance;

        }

        return klass.cast(component);
    }

    /**
     * Находит компонент в контейнере с данным типом и квалификаторм.
     * Кидает исключение, если компонент в стадии создания.
     * @param klass Тип компонента.
     * @param qualifier Квалификатор компонента, если null - возвращает первый попавшийся компонент данного типа.
     * @return Найденный компонент или null, если такого нет.
     */
    private static Object findComponent(Class<?> klass, String qualifier) {
        NamedComponentNode node = components.get(klass);
        if (node == null) {
            return null;
        }

        if (qualifier == null) {
            if (node.component == null) {
                throw new IllegalArgumentException(
                        "Cyclic dependency detected: " + klass + " instance is currently being created.");
            }
            return node.component;
        }

        while (node != null) {
            if (qualifier.equals(node.qualifier)) {
                if (node.component == null) {
                    throw new IllegalArgumentException(
                            "Cyclic dependency detected: " + klass + " instance is currently being created.");
                }
                return node.component;
            }
            node = node.next;
        }

        return null;
    }

    /**
     * Добавляет компонент (может быть null) в контейнер заменяя существующий, если такой уже есть.
     * @param klass Тип, который реализует добавляемый компонент.
     * @param qualifier Опциональный квалификатор компонента.
     * @param component Компонент или null.
     * @param <T> Тип компонента.
     * @return true, если такой компонент уже был в контейнере и был заменён, иначе - false.
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

    /**
     * Создаёт новый инстанс компонента с данным типом, разрешая его зависимости.
     * @param klass Тип компонента.
     * @return Созданный инстанс.
     * @param <T> Тип компонента.
     */
    private static <T> T instantiate(Class<T> klass) {
        final Constructor<?> constructor = findSuitableConstructor(klass);

        final Parameter[] parameters = constructor.getParameters();
        final Object[] arguments = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            try {
                final Class<?> parameterType = parameters[i].getType();
                final String qualifier = Optional.ofNullable(parameters[i].getAnnotation(Qualifier.class))
                        .map(Qualifier::value)
                        .orElse(null);
                arguments[i] = getComponent(parameterType, qualifier);
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

    /**
     * Возвращает конструктор, который должен быть использован для создания инстанса компонента
     * данного типа.
     * @param klass Тип компонента.
     * @return Найденный конструктор.
     */
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
