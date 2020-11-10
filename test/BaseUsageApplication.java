

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;

public class BaseUsageApplication {

    public static final Logger LOG = LoggerFactory.getLogger(BaseUsageApplication.class);

    public static void main(String... args) {
        AsyncDispatcher dispatcher = new AsyncDispatcher(Executors.newCachedThreadPool());

        //注册事件处理器
        dispatcher.register(BaseEventType.CREATE_ORDER,(BaseEvent e)->{
            LOG.info("Create_Order事件处理器 订单: " + e.getOrderId());
        });

        dispatcher.register(BaseEventType.CREATE_ORDER,(BaseEvent e)->{
            LOG.info("Create_Order事件处理器(2) 订单: "+e.getOrderId());
        });

        dispatcher.register(BaseEventType.PAY_ORDER,(BaseEvent e)->{
            LOG.info("Pay_Order事件处理器: "+e.getType()+",订单: " + e.getOrderId());
        });


        dispatcher.register(BaseEventType.class,(BaseEvent e)->{
            LOG.info("class事件处理器:  "+"事件类型:"+e.getType()+",订单:" + e.getOrderId());
        });

        //启动事件循环
        dispatcher.serviceStart();

        for (int i = 1; i <= 3; i++) {
            dispatcher.dispatchEvent(new BaseEvent(BaseEventType.CREATE_ORDER, dispatcher, "Id-" + i));
        }
    }
}
