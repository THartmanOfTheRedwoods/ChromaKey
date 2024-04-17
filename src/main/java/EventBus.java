import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
public class EventBus {
    public interface EventListener {
        void onEvent(EventBusData<?> data);
    }
    private final Map<String, Set<EventListener>> listeners;

    // Private constructor so instances of Configuration can't be called from external class.
    private EventBus(){
        listeners = new HashMap<>();
    }

    public void unregister(String eventName, EventListener listener) {
        Set<EventListener> eventListeners = listeners.get(eventName);
        if (eventListeners != null) {
            eventListeners.remove(listener); // Remove the specific listener
        }
    }

    public void register(String eventName, EventListener listener) {
        Set<EventListener> eventListeners = listeners.get(eventName);
        if (eventListeners == null) {
            eventListeners = new HashSet<>();
            listeners.put(eventName, eventListeners);
        } else {
            eventListeners.add(listener);
        }
    }

    public void fireEvent(String eventName, EventBusData<?> eventData) {
        Set<EventListener> eventListeners = listeners.get(eventName);
        if (eventListeners != null) {
            for (EventListener listener : eventListeners) {
                listener.onEvent(eventData);
            }
        }
    }

    // Private static inner class with static instance.
    // Class doesn't load until getInstance is called.
    // Thread-Safe because SingletonHelper class and INSTANCE are static.
    // Returns a single instance of Outer class Configuration and can because its in Configuration's scope.
    private static class SingletonHelper {
        private static final EventBus INSTANCE = new EventBus();
    }

    // Static getInstance method for accessing Singleton Object in a thread safe manner.
    public static EventBus getInstance() {
        return SingletonHelper.INSTANCE;
    }

}
