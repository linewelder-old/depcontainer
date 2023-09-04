package linewelder.depcontainer;

/**
 * Интерфейс обработчика события добавления компонента в контейнер.
 */
public interface ComponentEventListener {
    void onEvent(Class<?> klass, String qualifier, Object component);
}
