package com.atguigu.gmall.seckill.receiver;


import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.model.user.UserRecode;
import com.atguigu.gmall.seckill.service.SeckillService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Configuration
public class SeckillOrderReceiver {

    @Autowired
    SeckillService seckillService;

    @RabbitListener(bindings = @QueueBinding(
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_SECKILL_USER,durable = "true"),
            key = {MqConst.ROUTING_SECKILL_USER},
            value = @Queue(value = MqConst.QUEUE_SECKILL_USER, durable = "true")
    ))
    public void seckill(UserRecode userRecode, Message message, Channel channel) throws IOException {
        // 调用seckill服务在缓存中进行抢库存
        seckillService.consumeSeckillOrder(userRecode.getSkuId(), userRecode.getUserId());
        //确认收到消息
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);

    }
}
