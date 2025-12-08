package annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigProperty {

    /**
     * Имя файла конфигурации
     * По умолчанию: "application.properties"
     */
    String configFileName() default "application.properties";

    /**
     * Имя свойства в файле конфигурации
     * По умолчанию: ИМЯ_КЛАССА.ИМЯ_ПОЛЯ
     */
    String propertyName() default "";

    /**
     * Тип, к которому нужно преобразовать значение
     * По умолчанию: AUTO (автоматическое определение по типу поля)
     */
    PropertyType type() default PropertyType.AUTO;
}