public class BaseEvent extends AbstractEvent<BaseEventType> {
    private final String orderId;

    public BaseEvent(BaseEventType type, Dispatcher dispatcher,String orderId) {
        super(type, dispatcher);
        this.orderId = orderId;
    }

    public String getOrderId() {
        return this.orderId;
    }
}
