/**
 * 事件处理器接口
 * @param <T>
 * @param <E>
 */
public interface EventHandler<T extends  Enum<T>,E extends Event<T>> {

    /**
     * 事件处理函数
     * @param event 事件
     */
    void handle(E event);
}
