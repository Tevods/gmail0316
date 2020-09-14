package com.atguigu.gmall.payment.controller;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.enums.PaymentWay;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.order.client.OrderFeignClient;
import com.atguigu.gmall.payment.service.AlipayService;
import org.apache.http.HttpRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

@RestController
@RequestMapping("api/payment")
public class PaymentApiController {


    @Autowired
    AlipayService alipayService;
    @Autowired
    OrderFeignClient orderFeignClient;
    @Autowired
    RabbitService rabbitService;

    @RequestMapping("alipay/submit/{orderId}")
    public String alipay(@PathVariable String orderId, HttpServletRequest httpServletRequest){

        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        String form = alipayService.tradePagePay(orderInfo);
        // 保存支付信息
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setSubject(orderInfo.getOrderDetailList().get(0).getSkuName());
        paymentInfo.setOrderId(Long.parseLong(orderId));
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID.toString());
        paymentInfo.setPaymentType(PaymentType.ALIPAY.getComment());
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        /*paymentInfo.setTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setCallbackContent();
        paymentInfo.setCallbackTime();*/
        alipayService.save(paymentInfo);

        return form;
    }

//    http://api.gmall.com/api/payment/alipay/callback/return
    @RequestMapping("alipay/callback/return")
    public String aliCallback(HttpServletRequest request){
        String trade_no = request.getParameter("trade_no");
        String callback_content = request.getQueryString();
        String out_trade_no = request.getParameter("out_trade_no");


        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(out_trade_no);
        paymentInfo.setCallbackTime(new Date());
        paymentInfo.setCallbackContent(callback_content);
        paymentInfo.setTradeNo(trade_no);
        paymentInfo.setPaymentStatus(PaymentStatus.PAID.toString());

        PaymentInfo paymentInfoSelect = alipayService.checkPaySuccess(paymentInfo);
        String paymentStatus = paymentInfoSelect.getPaymentStatus();
        Long orderId = paymentInfoSelect.getOrderId();
        if (paymentStatus!=null && !paymentStatus.equals(PaymentStatus.PAID.toString())){
            alipayService.paySuccess(paymentInfo);
            // 同时发送同步订单信息的消息给订单模块
            rabbitService.sendMessage("exchange.syncOrderInfo","routing.syncOrderInfo",orderId);
        }

        // 返回支付成功界面
        return "<form name='punchout_form' method='get' action='http://payment.gmall.com/success'>\n" +
                "</form>\n" +
                "<script>document.forms[0].submit();</script>";
    }
}
