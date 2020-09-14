package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.order.client.OrderFeignClient;
import com.atguigu.gmall.user.client.UserFeignClient;
import net.bytebuddy.asm.Advice;
import org.aspectj.weaver.ast.Or;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.filter.OrderedFilter;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
public class OrderController {

    @Autowired
    CartFeignClient cartFeignClient;
    @Autowired
    UserFeignClient userFeignClient;
    @Autowired
    OrderFeignClient orderFeignClient;
    /**
     * 订单结算界面
     * 只是数据的呈现
     *
     * @param request
     * @return
     */
    @RequestMapping("trade.html")
    public String trade(HttpServletRequest request, Model model){
        // 获取userId
        String userId = request.getHeader("userId");
        // 从购物车获取商品信息封装后返回
        List<CartInfo> cartInfoList = cartFeignClient.getCartCheckedList(userId);
        // 封装
        Integer totalNum = 0;
        BigDecimal totalAmount = new BigDecimal(0);
        List<OrderDetail> orderDetails = new ArrayList<>();
        for (CartInfo cartInfo : cartInfoList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cartInfo,orderDetail);
            orderDetail.setOrderPrice(cartInfo.getCartPrice());
            totalNum += cartInfo.getSkuNum();
            totalAmount = cartInfo.getCartPrice().add(totalAmount);
        }

        List<UserAddress> addressList = userFeignClient.getUserAddrByUserId(userId);


        //detailArrayList
        model.addAttribute("detailArrayList",orderDetails);
        //totalNum
        model.addAttribute("totalNum",totalNum);
        //totalAmount
        model.addAttribute("totalAmount",totalAmount);
        model.addAttribute("userAddressList",addressList);
        //生成唯一的订单编号
        String tradeNo = orderFeignClient.genTradeNo(userId);
        model.addAttribute("tradeNo",tradeNo);
        return "order/trade";
    }



}
