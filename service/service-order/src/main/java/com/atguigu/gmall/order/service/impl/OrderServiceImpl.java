package com.atguigu.gmall.order.service.impl;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.HttpClient;
import com.atguigu.gmall.common.util.HttpClientUtil;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.enums.OrderStatus;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import net.bytebuddy.asm.Advice;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    OrderInfoMapper orderInfoMapper;
    @Autowired
    OrderDetailMapper orderDetailMapper;
    @Autowired
    ProductFeignClient productFeignClient;

    @Override
    public OrderInfo save(OrderInfo orderInfo) {
        orderInfoMapper.insert(orderInfo);

        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setOrderId(orderInfo.getId());
            orderDetailMapper.insert(orderDetail);
        }

        return orderInfo;
    }

    /**
     * 保存订单信息
     * @param order
     * @param userId
     * @param cartCheckedList
     * @return
     */
    @Override
    public Result saveOrderInfoAndDetail(OrderInfo order, String userId, List<CartInfo> cartCheckedList) {
        String deliveryAddress = order.getDeliveryAddress();// 地址id
        String consignee = order.getConsignee();// 收件人
        String consigneeTel = order.getConsigneeTel();//收件人电话
        String paymentWay = order.getPaymentWay(); //支付方式
        String orderComment = order.getOrderComment();
        if (cartCheckedList!=null && cartCheckedList.size()>0){
            OrderInfo orderInfo = new OrderInfo();
            orderInfo.setConsignee(consignee);
            orderInfo.setConsigneeTel(consigneeTel);
            orderInfo.setDeliveryAddress(deliveryAddress); //地址
            orderInfo.setOrderStatus(OrderStatus.UNPAID.getComment()); //订单状态

            orderInfo.setUserId(Long.parseLong(userId));
            orderInfo.setPaymentWay(paymentWay); //支付方式
            orderInfo.setOrderComment(orderComment); //订单备注
            // 外部订单号
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            String timeFormat = sdf.format(new Date());
            long currentTimeMillis = System.currentTimeMillis();
            orderInfo.setOutTradeNo("atguigu"+currentTimeMillis+timeFormat);
            orderInfo.setCreateTime(new Date());
            // 订单有效时间
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE,1);
            Date expireTime = calendar.getTime();
            orderInfo.setExpireTime(expireTime); // 过期时间 - 当前时间 = 倒计时
            orderInfo.setProcessStatus(ProcessStatus.UNPAID.getComment()); //订单进度
            orderInfo.setImgUrl(cartCheckedList.get(0).getImgUrl());
            //TODO 物流编号 父订单编号
            List<OrderDetail> orderDetails = new ArrayList<>();
            BigDecimal totalAmount = new BigDecimal(0);
            for (CartInfo cartInfo : cartCheckedList) {
                OrderDetail orderDetail = new OrderDetail();
                BeanUtils.copyProperties(cartInfo,orderDetail);
                orderDetail.setOrderPrice(cartInfo.getCartPrice());
                totalAmount = cartInfo.getCartPrice().add(totalAmount);
                orderDetails.add(orderDetail);
                //校验库存 价格
                BigDecimal price = productFeignClient.getPrice(cartInfo.getSkuId()+"");
                int i = price.compareTo(cartInfo.getSkuPrice());
                if (i != 0){
                    return Result.fail();
                }
                //校验库存
                /*String stockUrl = "http://localhost:9001/hasStock?skuId="+cartInfo.getSkuId()+"&num="+cartInfo.getSkuNum();
                String hasStock = HttpClientUtil.doGet(stockUrl);
                if (StringUtils.isNotEmpty(hasStock)){
                    int isStock = new BigDecimal(hasStock).compareTo(new BigDecimal(0));
                    if (isStock == 0){
                        return Result.fail();
                    }
                }*/

            }
            orderInfo.setTotalAmount(totalAmount);
            orderInfo.setOrderDetailList(orderDetails);

            OrderInfo orderInfoSave = this.save(orderInfo);

            //TODO 删除购物车的东西

            return Result.ok(orderInfoSave.getId());
        }
        return Result.fail();
    }

    @Override
    public OrderInfo getOrderInfo(String orderId) {
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        QueryWrapper<OrderDetail> wrapper = new QueryWrapper<>();
        wrapper.eq("order_id",orderId);
        List<OrderDetail> orderDetails = orderDetailMapper.selectList(wrapper);
        orderInfo.setOrderDetailList(orderDetails);
        return orderInfo;
    }

    @Override
    public OrderInfo updateProcessStatus(Long orderId) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setProcessStatus(ProcessStatus.PAID.getComment());
        orderInfo.setOrderStatus(OrderStatus.PAID.getComment());
        orderInfo.setId(orderId);
        // 订单详细列表
        orderInfoMapper.updateById(orderInfo);

        QueryWrapper<OrderDetail> wrapper = new QueryWrapper<>();
        wrapper.eq("order_id",orderId);
        List<OrderDetail> orderDetails = orderDetailMapper.selectList(wrapper);

        orderInfo.setOrderDetailList(orderDetails);

        return orderInfo;
    }
}
