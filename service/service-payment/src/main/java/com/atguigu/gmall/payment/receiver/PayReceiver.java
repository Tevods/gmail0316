package com.atguigu.gmall.payment.receiver;

import com.alipay.api.domain.OuterTargetingItem;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.config.DelayedMqConfig;
import com.atguigu.gmall.payment.service.AlipayService;
import com.rabbitmq.client.Channel;
import net.bytebuddy.asm.Advice;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@Configuration
public class PayReceiver {
    @Autowired
    AlipayService alipayService;
    @Autowired
    RabbitService rabbitService;

    @RabbitListener(queues = "queue.delay.1")
    public void paySuccess(String outTradeNo, Message message, Channel channel){
        // 调用支付宝服务检查支付结果
        System.out.println("调用支付宝服务");
        boolean flag = alipayService.checkPayStatus(outTradeNo);

//        // 发送消息更改支付结果
//        if (flag){
//            rabbitService.sendMessage();
//        }
    }
}
