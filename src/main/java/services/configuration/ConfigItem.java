package services.configuration;

public class ConfigItem<T> {
    // We store the config item as a value, and use valueType to convert it
    private T value;

    public ConfigItem(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }
}
