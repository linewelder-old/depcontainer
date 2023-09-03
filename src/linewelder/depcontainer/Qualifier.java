package linewelder.depcontainer;

import java.lang.annotation.*;

/**
 * Применяется к параметру конструктора для указания квалификатора нужного компонента.
 * Заменяет стандартное поведение, при котором берётся первый попавшийся компонент,
 * нахождением компонента с конкретным квалификатором.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Qualifier {
    String value();
}
