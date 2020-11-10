public abstract class AbstractEvent<T extends Enum<T>> implements Event<T> {
    private final T type;
    private final long timestamp;
    private final Dispatcher dispatcher;

    public AbstractEvent(T type, long timestamp, Dispatcher dispatcher) {
        this.type = type;
        this.timestamp = timestamp;
        this.dispatcher = dispatcher;
    }

    public AbstractEvent(T type, Dispatcher dispatcher) {
        this(type, -1L, dispatcher);
    }

    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    @Override
    public T getType() {
        return type;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "EventType: " + getType();
    }
}
