package linewelder.depcontainer;

import java.lang.annotation.*;

/**
 * Используется для обозначения метода компонента ({@link Component}),
 * который должен быть вызван после конструктора.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PostConstructor {}
