import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.ArrayList;
import java.util.concurrent.*;

/**
 * 异步事件分发器
 * 每一个事件类型允许注册一个或多个事件处理器
 * 事件循环允许在单独的线程中
 * 事件处理器与事件循环运行在不同的线程中（异步），同一个事件的多个处理器运行在同一个线程中（同步）
 */
public class AsyncDispatcher implements Dispatcher{
    private static final Logger LOG = LoggerFactory.getLogger(AsyncDispatcher.class);
    private static final int EVENT_PRINT_THRESHOLD = 1000;

    //事件阻塞队列
    private final BlockingQueue<Event<?>> eventQueue;
    private volatile boolean stopped = false;

    //事件循环线程
    private Thread eventLoopThread;
    //事件处理线程池
    private final ExecutorService eventHandlingPool;
    //事件和事件处理器映射
    protected final Map<Object, EventHandler<?, ?>> eventDispatchers;

    public AsyncDispatcher(ExecutorService eventHandlingPool) {
        this(new LinkedBlockingQueue<>(),eventHandlingPool);
    }

    public AsyncDispatcher(BlockingQueue<Event<?>> eventQueue, ExecutorService eventHandlingPool) {
        this.eventQueue = eventQueue;
        this.eventDispatchers = new HashMap<>();
        this.eventHandlingPool = eventHandlingPool;
    }

    /**
     * 事件循环逻辑，循环获取队列中事件进行分发
     * @return
     */
    Runnable createThread() {
        return () -> {
            while (!stopped && !Thread.currentThread().isInterrupted()) {
                Event<?> event;
                try {
                    //事件阻塞队列获取事件
                    event = eventQueue.take();
                } catch (InterruptedException e) {
                    if (!stopped) {
                        LOG.warn("AsyDispatcher thread interrupted",e);
                    }
                    return;
                }
                //分发事件
                dispatch(event);
            }
        };
    }

    /**
     * 启动一个事件循环线程
     */
    public void serviceStart() {
        //创建一个单线程的线程池
        ThreadPoolExecutor singleThreadPool = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),((runnable, executor) -> {
            eventLoopThread = new Thread(runnable);
            eventLoopThread.setName("AsyncDispatcher event handler");
            executor.execute(eventLoopThread);
        }));
        singleThreadPool.execute(createThread());
    }

    /**
     * 关闭事件循环
     */
    public void serviceStop() {
        stopped = true;
        if (eventLoopThread != null) {
            eventLoopThread.interrupt();
            try{
                //等待事件循环线程结束
                eventLoopThread.join();
            } catch (InterruptedException e) {
                LOG.warn("Interrupted Exception while stopping",e);
            }
        }
    }

    /**
     * 分发函数，事件循环调用
     * @param event
     * @param <T>
     * @param <E>
     */
    protected <T extends Enum<T>, E extends Event<T>> void dispatch(E event) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Dispatching the event " + event.getClass().getName()+"."+event.toString());
        }
        T type = event.getType();
        try {
            //事件类型class处理器
            @SuppressWarnings("unchecked")
            EventHandler<T, E> handler1 = (EventHandler<T, E>) eventDispatchers.get(type.getClass());
            //事件类型处理器
            @SuppressWarnings("unchecked")
            EventHandler<T, E> handler2 = (EventHandler<T, E>) eventDispatchers.get(type);
            if (handler1 == null && handler2 == null) {
                throw new Exception("No handler for registered for " + type);
            }
            //提交事件处理到事件处理线程池,同一个事件的所有处理器同步执行
            eventHandlingPool.execute(() -> {
                if (handler1 != null) {
                    handler1.handle(event);//先执行class注册的handler
                }//
                if (handler2 != null) {
                    handler2.handle(event);//再执行type注册的handler
                }
            });
        } catch (Throwable t) {
            LOG.error("something happen in handle", t);
        }
    }

    @Override
    public <T extends Enum<T>, E extends Event<T>> void dispatchEvent(E event) {
        int queueSize = eventQueue.size();
        if (queueSize != 0) {
            LOG.debug("size of event-queue is " + queueSize);
        }
        int remCapacity = eventQueue.remainingCapacity();
        if (remCapacity < EVENT_PRINT_THRESHOLD) {
            LOG.warn("Very low remaining capacity in the event-queue: " + remCapacity);
        }
        try{
            eventQueue.put(event);
        } catch (InterruptedException e) {
            LOG.error("interrupted while put in event queue");
            throw new RuntimeException(e);
        }
    }

    private <T extends Enum<T>, E extends Event<T>> void internalRegister(Object eventType, EventHandler<T, E> eventHandler) {
        //检查是否已经注册处理器
        EventHandler<T, E> registerHandler = (EventHandler<T, E>) eventDispatchers.get(eventType);
        LOG.info("Register " + eventType + " for " + eventHandler.getClass());
        if (registerHandler == null) {
            eventDispatchers.put(eventType, eventHandler);
        } else if (!(registerHandler instanceof MultiListenerHandler)) {//若该事件类型已注册了处理器，而且不是多监听处理器
            MultiListenerHandler<T, E> multiListenerHandler = new MultiListenerHandler<>();
            multiListenerHandler.addHandler(registerHandler);
            multiListenerHandler.addHandler(eventHandler);
            eventDispatchers.put(eventType, multiListenerHandler);
        }else {
            //已经是 multiListenHandler，只需添加handle
            MultiListenerHandler<T, E> multiListenerHandler = (MultiListenerHandler) registerHandler;
            multiListenerHandler.addHandler(eventHandler);
        }
    }

    @Override
    public <T extends Enum<T>, E extends Event<T>> void register(T eventType, EventHandler<T, E> eventHandler) {
        this.internalRegister(eventType, eventHandler);
    }

    @Override
    public <T extends Enum<T>, E extends Event<T>> void register(Class<T> eventTypeClazz, EventHandler<T, E> eventHandler) {
        this.internalRegister(eventTypeClazz, eventHandler);
    }

    /**
     * 多监听处理器。多个对同一事件感兴趣的事件处理器。
     * @param <T>
     * @param <E>
     */
    static class MultiListenerHandler<T extends Enum<T>, E extends Event<T>> implements EventHandler<T, E> {
        List<EventHandler<T, E>> listOfHandler;

        public MultiListenerHandler() {
            this.listOfHandler = new ArrayList<>();
        }

        @Override
        public void handle(E event) {
            for (EventHandler<T, E> handler : listOfHandler) {
                handler.handle(event);
            }
        }

        void addHandler(EventHandler<T, E> handler) {
            listOfHandler.add(handler);
        }
    }

}
