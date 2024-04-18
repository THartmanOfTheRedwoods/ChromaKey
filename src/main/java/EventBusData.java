public class EventBusData<T> {
    private final String type;
    private final T eventData;

    public EventBusData(String type, T eventData) {
        this.type = type;
        this.eventData = eventData;
    }

    public String getType() {
        return type;
    }

    public T getEventData() {
        return eventData;
    }
}
