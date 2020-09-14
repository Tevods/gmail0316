package com.atguigu.gmall.common.service.impl;

import com.atguigu.gmall.common.service.RabbitService;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RabbitServiceImpl implements RabbitService {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public void sendMessage(String exchange,String routingKey,Object message){
        rabbitTemplate.convertAndSend(exchange,routingKey,message);
    }

    @Override
    public void sendDelayMessage(String exchange, String routing, String outTradeNo, int delayTime) {
        rabbitTemplate.convertAndSend(exchange,routing,outTradeNo,new MessagePostProcessor() {
            @Override
            public Message postProcessMessage(Message message) throws AmqpException {
                message.getMessageProperties().setDelay(delayTime*1000);// 默认毫秒计算
                return message;
            }
        });
    }

}
