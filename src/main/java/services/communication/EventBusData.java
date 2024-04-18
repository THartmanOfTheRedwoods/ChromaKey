package services.communication;

public record EventBusData<T>(String type, T eventData) {
}
