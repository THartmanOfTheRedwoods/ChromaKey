package services.configuration;

public enum ConfigKey {
    BACKGROUND_DIR("Background Directory", new ConfigItem<>("backgrounds")),
    BACKGROUND("Background", new ConfigItem<>("tiger.jpg")),
    CAMERA("Camera", new ConfigItem<>(0)),
    FRAME_WIDTH("Frame Width", new ConfigItem<>(800)),
    FRAME_HEIGHT("Frame Height", new ConfigItem<>(450)),
    LOWER_HUE("Lower Hsv", new ConfigItem<>(40)),
    LOWER_SATURATION("Lower Saturation", new ConfigItem<>(40)),
    LOWER_VALUE("Lower Value", new ConfigItem<>(40)),
    UPPER_HUE("Upper Hsv", new ConfigItem<>(80)),
    UPPER_SATURATION("Upper Saturation", new ConfigItem<>(255)),
    UPPER_VALUE("Upper Value", new ConfigItem<>(255));

    private final String label;
    private final ConfigItem<?> defaultValue;
    ConfigKey(String label, ConfigItem<?> defaultValue) {
        this.label = label;
        this.defaultValue = defaultValue;
    }

    public String getLabel() {
        return this.label;
    }

    public ConfigItem<?> getDefault() {
        return this.defaultValue;
    }

}