package services.configuration;

import java.util.HashMap;
import java.util.Map;

public class Configuration {

    private final Map<ConfigKey, ConfigItem<?>> settings;
    // Private constructor so instances of services.configuration.Configuration can't be called from external class.
    private Configuration(){
        settings = new HashMap<>();
    }

    public void addConfig(ConfigKey key, ConfigItem<?> value) {
        // Expect value to be an array of length 2 with index 0 being
        this.settings.put(key, value);
    }

    public ConfigItem<?> getConfig(ConfigKey key) {
        return (this.settings.get(key) != null) ? this.settings.get(key) : key.getDefault();
    }

    // Private static inner class with static instance.
    // Class doesn't load until getInstance is called.
    // Thread-Safe because SingletonHelper class and INSTANCE are static.
    // Returns a single instance of Outer class services.configuration.Configuration and can because its in services.configuration.Configuration's scope.
    private static class SingletonHelper {
        private static final Configuration INSTANCE = new Configuration();
    }

    // Static getInstance method for accessing Singleton Object in a thread safe manner.
    public static Configuration getInstance() {
        return SingletonHelper.INSTANCE;
    }

}
