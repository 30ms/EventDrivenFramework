/**
 * 事件分发器接口
 * 用于将事件分发到注册该事件(EventType)的事件处理器(EventHandle)
 */
public interface Dispatcher {

    /**
     * 触发一个事件，分发到指定的处理器
     * @param event 事件
     * @param <T> 事件类型的类型
     * @param <E> 事件类型
     */
    <T extends Enum<T>, E extends Event<T>> void dispatchEvent(E event);

    /**
     * 为一个事件类型，注册一个处理器
     * @param eventType 事件类型
     * @param eventHandler 事件处理器
     * @param <T>
     * @param <E>
     */
    <T extends Enum<T>, E extends Event<T>> void register(T eventType, EventHandler<T, E> eventHandler);

    /**
     * 针对一种事件类型的类型，注册一个处理器，即所有该class的类型都会触发该处理器
     * @param eventTypeClazz 事件类型的类型
     * @param eventHandler 事件处理器
     * @param <T>
     * @param <E>
     */
    <T extends Enum<T>, E extends Event<T>> void register(Class<T> eventTypeClazz, EventHandler<T,E> eventHandler);
}
