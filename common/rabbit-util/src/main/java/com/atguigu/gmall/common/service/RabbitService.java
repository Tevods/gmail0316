package com.atguigu.gmall.common.service;

import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public interface RabbitService{
    void sendMessage(String exchange, String routingkey, Object message);

    void sendDelayMessage(String exchange, String routing, String outTradeNo, int i);
}
