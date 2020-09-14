package com.atguigu.gmall.payment.service.impl;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.config.DelayedMqConfig;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.payment.service.AlipayService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AlipayServiceImpl implements AlipayService {

    @Autowired
    AlipayClient alipayClient;
    @Autowired
    PaymentInfoMapper paymentInfoMapper;
    @Autowired
    RabbitService rabbitService;

    /**
     * 向支付宝发送订单
     * @param orderInfo
     * @return
     */
    @Override
    public String tradePagePay(OrderInfo orderInfo) {
        String form = "";
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();

        request.setNotifyUrl(AlipayConfig.notify_payment_url);// 设置异步通知地址
        request.setReturnUrl(AlipayConfig.return_payment_url);// 设置同步回调地址

        Map<String,Object> map = new HashMap<>();
        map.put("out_trade_no",orderInfo.getOutTradeNo());
        map.put("product_code","FAST_INSTANT_TRADE_PAY");
        map.put("total_amount",0.01);


        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        map.put("subject",orderDetailList.get(0).getSkuName().substring(0,20));

        request.setBizContent(JSON.toJSONString(map));
        try {
            AlipayTradePagePayResponse response = alipayClient.pageExecute(request);
            form = response.getBody();
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        return form;
    }

    /**
     * 保存订单流程信息同时检查订单支付状态
     * @param paymentInfo
     */
    @Override
    public void save(PaymentInfo paymentInfo) {
        paymentInfoMapper.insert(paymentInfo);

        // 发送检查支付结果
        String exchange = "exchange.delay";
        String routing = "routing.delay";
        String outTradeNo = paymentInfo.getOutTradeNo();
        rabbitService.sendDelayMessage(exchange,routing,outTradeNo,15);
    }

    @Override
    public PaymentInfo checkPaySuccess(PaymentInfo paymentInfo) {
        QueryWrapper<PaymentInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("out_trade_no",paymentInfo.getOutTradeNo());
        PaymentInfo paymentInfo1 = paymentInfoMapper.selectOne(wrapper);

        if (paymentInfo1!=null){
            return paymentInfo1;
        }

        return null;
    }

    @Override
    public void paySuccess(PaymentInfo paymentInfo) {
        QueryWrapper<PaymentInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("out_trade_no",paymentInfo.getOutTradeNo());
        paymentInfoMapper.update(paymentInfo,wrapper);
    }

    @Override
    public boolean checkPayStatus(String outTradeNo) {
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        Map<String,Object> map = new HashMap<>();
        map.put("out_trade_no",outTradeNo);
        request.setBizContent(JSON.toJSONString(map));
        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }


        if (response!=null && response.isSuccess()){
            return true;
        }

        return false;
    }
}
