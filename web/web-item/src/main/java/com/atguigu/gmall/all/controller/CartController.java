package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.model.cart.CartInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * 1.商品点击添加购物车，同步访问添加页面
 * 2.网关判断是否登录，登录传递userId
 * 3.未登录生成userTempId，userTempId传递给addU
 */
@Controller
public class CartController {
    @Autowired
    CartFeignClient cartFeignClient;

    /**
     * 前台添加购物车请求调用
     * 1.获取网关传递的userId
     * 2.或通过userTempId获取临时的userId
     * @param request
     * @return
     */
    @RequestMapping("addCart.html")
    public String addCart(HttpServletRequest request,Long skuId,Integer skuNum){

        String userId = "";
        userId = request.getHeader("userTempId");
        if (!StringUtils.isEmpty(request.getHeader("userId"))){
            userId = request.getHeader("userId");
        }
        // 当俩种情况存在一种的时候，添加商品信息
        if (!StringUtils.isEmpty(userId)){
            // 将商品的skuId和数量传递给后台
            CartInfo cartInfo = new CartInfo();
            cartInfo.setSkuId(skuId);
            cartInfo.setSkuNum(skuNum);
            cartFeignClient.addCart(cartInfo);
        }

        return "redirect:http://cart.gmall.com/cart/addCart.html"; //重定向到商品静态添加页面
    }

    /**
     * 购物车列表
     * @param request
     * @return
     */
    @RequestMapping("cart.html")
    public String cart(HttpServletRequest request){
        String userId = "";
        userId = request.getHeader("userTempId");
        if (StringUtils.isNotEmpty(request.getHeader("userId"))){
            userId = request.getHeader("userId");
        }

        return "cart/index";
    }
}
