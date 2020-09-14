package com.atguigu.gmall.order.client;

import com.atguigu.gmall.model.order.OrderInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient(value = "service-order")
public interface OrderFeignClient {


    @RequestMapping("api/order/inner/genTradeNo/{userId}")
    String genTradeNo(@PathVariable("userId") String userId);

    @RequestMapping("api/order/inner/getOrderInfo/{orderId}")
    OrderInfo getOrderInfo(@PathVariable("orderId") String orderId);
}
