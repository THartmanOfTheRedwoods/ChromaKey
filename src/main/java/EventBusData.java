public class EventBusData<T> {
    private final String type;
    private final T eventData;

    // @TODO: Make this a generic instead of utilizing Object
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
