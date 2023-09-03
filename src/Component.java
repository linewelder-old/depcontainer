import java.lang.annotation.*;

/**
 * Применяется к классу, объектами которого заведует {@link DependencyContainer}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Component {}
