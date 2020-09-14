package com.atguigu.gmall.mq.receiver;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 配置消费端
 */
@Component
@Configuration
public class ConfirmReceiver {

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "queue.confirm",autoDelete = "false")
            ,exchange = @Exchange(value = "exchange.confirm",autoDelete = "true")
            ,key = {"routing.confirm"}
    ))
    public void process(Message message, Channel channel) throws IOException {
        System.out.println(new String(message.getBody()));

        try {
            int i = 1/0;
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (Exception e) {
            if (message.getMessageProperties().getRedelivered()){
                System.out.println("这是重复消息，拒绝接受");
                channel.basicReject(message.getMessageProperties().getDeliveryTag(),false);
            }else {
                System.out.println("出现异常将消息返回队列，重新发送");
                channel.basicNack(message.getMessageProperties().getDeliveryTag(),false,true);
            }
        }
    }




}
