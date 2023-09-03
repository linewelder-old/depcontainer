import java.lang.annotation.*;

/**
 * Применяется к конструктору класса компонента ({@link Component}), позволяя обозначить конструктор,
 * который использует {@link DependencyContainer} для создания объекта.
 */
@Target(ElementType.CONSTRUCTOR)
@Retention(RetentionPolicy.RUNTIME)
public @interface Autowired {}
