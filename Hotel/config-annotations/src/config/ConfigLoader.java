package config;

import annotations.ConfigProperty;
import annotations.ConfigClass;
import annotations.PropertyType;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

public class ConfigLoader {

    /**
     * Загружает конфигурацию в объект на основе аннотаций
     */
    public static void loadConfig(Object configObject) {
        Class<?> clazz = configObject.getClass();

        // Получаем имя файла конфигурации из аннотации класса или используем по умолчанию
        String configFileName = "application.properties";
        if (clazz.isAnnotationPresent(ConfigClass.class)) {
            ConfigClass classAnnotation = clazz.getAnnotation(ConfigClass.class);
            configFileName = classAnnotation.configFileName();
        }

        // Загружаем свойства из файла
        Properties properties = loadProperties(configFileName);

        // Обрабатываем все поля класса
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(ConfigProperty.class)) {
                processField(configObject, field, properties);
            }
        }
    }

    private static Properties loadProperties(String fileName) {
        Properties properties = new Properties();

        try (InputStream input = new FileInputStream(fileName)) {
            properties.load(input);
        } catch (IOException e) {
            System.err.println("⚠️ Файл конфигурации не найден: " + fileName +
                    ". Используются значения по умолчанию.");
        }

        // Также пробуем загрузить из ресурсов
        try (InputStream input = ConfigLoader.class.getClassLoader()
                .getResourceAsStream(fileName)) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException e) {
            // Игнорируем, если нет в ресурсах
        }

        return properties;
    }

    private static void processField(Object object, Field field, Properties properties) {
        ConfigProperty annotation = field.getAnnotation(ConfigProperty.class);

        // Определяем ключ свойства
        String propertyKey = getPropertyKey(annotation, field);

        // Получаем значение из properties
        String propertyValue = properties.getProperty(propertyKey);
        if (propertyValue == null) {
            return; // Значение не указано в конфиге, оставляем значение по умолчанию
        }

        try {
            // Преобразуем значение в нужный тип
            Object value = convertValue(propertyValue, field.getType(), annotation.type());

            // Устанавливаем значение в поле
            field.setAccessible(true);
            field.set(object, value);

        } catch (IllegalAccessException e) {
            System.err.println("❌ Ошибка при установке поля " + field.getName() + ": " + e.getMessage());
        }
    }

    private static String getPropertyKey(ConfigProperty annotation, Field field) {
        // Если propertyName указано явно - используем его
        if (!annotation.propertyName().isEmpty()) {
            return annotation.propertyName();
        }

        // Иначе формируем ИМЯ_КЛАССА.ИМЯ_ПОЛЯ
        String className = field.getDeclaringClass().getSimpleName().toLowerCase();
        String fieldName = field.getName().toLowerCase();
        return className + "." + fieldName;
    }

    private static Object convertValue(String value, Class<?> targetType, PropertyType propertyType) {
        // Убираем пробелы
        value = value.trim();

        // Определяем фактический тип для преобразования
        PropertyType actualType = (propertyType == PropertyType.AUTO) ?
                determineType(targetType) : propertyType;

        try {
            switch (actualType) {
                case STRING:
                    return value;

                case INTEGER:
                    return Integer.parseInt(value);

                case LONG:
                    return Long.parseLong(value);

                case DOUBLE:
                    return Double.parseDouble(value);

                case BOOLEAN:
                    return Boolean.parseBoolean(value);

                case ARRAY:
                case LIST:
                    return convertToCollection(value, targetType);

                default:
                    throw new IllegalArgumentException("Не поддерживаемый тип: " + actualType);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Не удалось преобразовать значение '" + value + "' к типу " + actualType, e);
        }
    }

    private static PropertyType determineType(Class<?> type) {
        if (type == String.class) return PropertyType.STRING;
        if (type == int.class || type == Integer.class) return PropertyType.INTEGER;
        if (type == long.class || type == Long.class) return PropertyType.LONG;
        if (type == double.class || type == Double.class) return PropertyType.DOUBLE;
        if (type == boolean.class || type == Boolean.class) return PropertyType.BOOLEAN;
        if (type.isArray()) return PropertyType.ARRAY;
        if (List.class.isAssignableFrom(type)) return PropertyType.LIST;
        return PropertyType.STRING;
    }

    private static Object convertToCollection(String value, Class<?> targetType) {
        String[] parts = value.split(",");

        if (targetType.isArray()) {
            Class<?> componentType = targetType.getComponentType();
            return convertArray(parts, componentType);
        } else if (List.class.isAssignableFrom(targetType)) {
            return Arrays.asList(parts);
        }

        throw new IllegalArgumentException("Неподдерживаемый тип коллекции: " + targetType);
    }

    private static Object convertArray(String[] values, Class<?> componentType) {
        if (componentType == String.class) {
            return values;
        } else if (componentType == Integer.class || componentType == int.class) {
            Integer[] array = new Integer[values.length];
            for (int i = 0; i < values.length; i++) {
                array[i] = Integer.parseInt(values[i].trim());
            }
            return array;
        }
        // Добавь другие типы при необходимости
        return values;
    }
}