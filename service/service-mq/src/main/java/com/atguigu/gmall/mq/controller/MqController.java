package com.atguigu.gmall.mq.controller;

import com.atguigu.gmall.common.service.RabbitService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/mq")
public class MqController {
    @Autowired
    RabbitService rabbitService;

    @RequestMapping("sendMessage")
    public String sendMessage(){
        String exchange = "exchange.confirm";
        String routingKey = "routing.confirm";
        String message = "我是被发送的消息";
        rabbitService.sendMessage(exchange, routingKey, message);
        return "success";
    }
}
