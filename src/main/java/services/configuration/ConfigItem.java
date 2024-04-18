package services.configuration;

/**
 * @param value We store the config item as a value, and use valueType to convert it
 */
public record ConfigItem<T>(T value) {
}
