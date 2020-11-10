/**
 * 事件接口
 * @param <T>
 */
public interface Event<T extends Enum<T>> {
    /**
     * 获取事件类型
     * @return 事件类型
     */
    T getType();

    /**
     * 获取事件触发的时间戳
     * @return 事件触发时间戳
     */
    long getTimestamp();



}
