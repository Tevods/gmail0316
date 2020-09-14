package com.atguigu.gmall.payment.service;

import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;

public interface AlipayService {
    String tradePagePay(OrderInfo orderInfo);

    void save(PaymentInfo paymentInfo);

    PaymentInfo checkPaySuccess(PaymentInfo paymentInfo);

    void paySuccess(PaymentInfo paymentInfo);

    boolean checkPayStatus(String outTradeNo);
}
