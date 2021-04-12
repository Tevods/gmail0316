package com.atguigu.gmall.order.controller;

import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.enums.OrderStatus;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.atguigu.gmall.user.client.UserFeignClient;
import org.aspectj.weaver.ast.Or;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("api/order")
public class OrderApiController {

    @Autowired
    CartFeignClient cartFeignClient;
    @Autowired
    ProductFeignClient productFeignClient;
    @Autowired
    UserFeignClient userFeignClient;
    @Autowired
    OrderService orderService;
    @Autowired
    RedisTemplate redisTemplate;
    /**
     * 提交订单
     * 1.通过获取购物车中选中的商品生成订单
     * @param order 前端传递过来的部分购物车信息
     * @param tradeNo 外部支付的订单号
     * @return
     */
    @RequestMapping("auth/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo order, String tradeNo, HttpServletRequest request){
        String userId = request.getHeader("userId");

        if (checkTradeNo(tradeNo,userId)){
            List<CartInfo> cartCheckedList = cartFeignClient.getCartCheckedList(userId);
            Result result = orderService.saveOrderInfoAndDetail(order,userId,cartCheckedList);
            return result;
        }
        return Result.fail();
    }

    @RequestMapping("inner/saveSeckillOrder")
    OrderInfo saveSeckillOrder(@RequestBody OrderInfo orderInfo){
        OrderInfo orderInfoResult = orderService.save(orderInfo);
        return orderInfoResult;
    }

    @RequestMapping("inner/genTradeNo/{userId}")
    String genTradeNo(@PathVariable("userId") String userId){
        String tradeNo = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set("user:" + userId + ":tradeNo", tradeNo);
        return tradeNo;
    }

    @RequestMapping("inner/getOrderInfo/{orderId}")
    OrderInfo getOrderInfo(@PathVariable("orderId") String orderId){
        return orderService.getOrderInfo(orderId);
    }

    public boolean checkTradeNo(String tradeNo,String userId){
        boolean b = false;
        String tradeNoFromCache = (String) redisTemplate.opsForValue().get("user:" + userId + ":tradeNo" );
        if (tradeNo.equals(tradeNoFromCache)){
            b = true;
        }
        return b;
    }



}
