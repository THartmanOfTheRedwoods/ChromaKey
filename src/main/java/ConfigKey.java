public enum ConfigKey {
    BACKGROUND_DIR("Background Directory", new ConfigItem<String>("backgrounds")),
    BACKGROUND("Background", new ConfigItem<String>("tiger.jpg")),
    CAMERA("Camera", new ConfigItem<Integer>(0)),
    FRAME_WIDTH("Frame Width", new ConfigItem<Integer>(800)),
    FRAME_HEIGHT("Frame Height", new ConfigItem<Integer>(450)),
    LOWER_HUE("Lower Hsv", new ConfigItem<Integer>(40)),
    LOWER_SATURATION("Lower Saturation", new ConfigItem<Integer>(40)),
    LOWER_VALUE("Lower Value", new ConfigItem<Integer>(40)),
    UPPER_HUE("Upper Hsv", new ConfigItem<Integer>(80)),
    UPPER_SATURATION("Upper Saturation", new ConfigItem<Integer>(255)),
    UPPER_VALUE("Upper Value", new ConfigItem<Integer>(255));

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