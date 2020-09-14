package com.atguigu.gmall.order.service;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.order.OrderInfo;

import java.util.List;

public interface OrderService {
    OrderInfo save(OrderInfo orderInfo);

    Result saveOrderInfoAndDetail(OrderInfo order, String userId, List<CartInfo> cartCheckedList);

    OrderInfo getOrderInfo(String orderId);

    OrderInfo updateProcessStatus(Long orderId);
}
