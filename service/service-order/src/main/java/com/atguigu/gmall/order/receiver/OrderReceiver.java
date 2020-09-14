package com.atguigu.gmall.order.receiver;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.order.service.OrderService;
import com.rabbitmq.client.Channel;
import com.sun.org.apache.xpath.internal.operations.String;
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
public class OrderReceiver {

    @Autowired
    OrderService orderService;

    // "syncOrderInfo.exchange","syncOrderInfo", JSON.toJSONString(paymentInfo)
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "queue.syncOrderInfo",durable = "true",autoDelete = "false")
            ,exchange = @Exchange(value = "exchange.syncOrderInfo",durable = "true",autoDelete = "false")
            ,key = {"routing.syncOrderInfo"}
    ))
    public void paySuccess(Long orderId,Message message, Channel channel) throws IOException {

        try {
            if (orderId != null){
                OrderInfo orderInfo = orderService.getOrderInfo(orderId.toString());
                if (orderInfo != null && ProcessStatus.UNPAID.getComment().equals(orderInfo.getProcessStatus())){
                    OrderInfo orderInfoUpdate =  orderService.updateProcessStatus(orderId);
                    // 发起锁定库存的消息
                    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                }else {
                    channel.basicNack(message.getMessageProperties().getDeliveryTag(),false,false);
                }
            }else {
                channel.basicNack(message.getMessageProperties().getDeliveryTag(),false,false);
            }
        } catch (Exception e){
            channel.basicNack(message.getMessageProperties().getDeliveryTag(),false,true);
        }

    }
}
