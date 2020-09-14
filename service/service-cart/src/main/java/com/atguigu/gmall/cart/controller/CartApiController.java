package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;

@RestController
@RequestMapping("api/cart")
public class CartApiController {

    @Autowired
    CartService cartService;

    @RequestMapping("cartTest")
    public String cartTest(HttpServletRequest request){
        String userId = request.getHeader("userId");
        return null;
    }

    @RequestMapping("cartList")
    public Result cartList(HttpServletRequest request){
        // 前台异步请求商品信息，userId和userTempId是天台自行对ajax请求头添加的
        String userId = "";
        userId = request.getHeader("userTempId");
        if (StringUtils.isNotEmpty(request.getHeader("userId"))){
            userId = request.getHeader("userId");
        }

        List<CartInfo> cartInfoList = cartService.cartList(userId);

        return Result.ok(cartInfoList);
    }

    @RequestMapping("addCart")
    void addCart(@RequestBody CartInfo cartInfo,HttpServletRequest request){
        String userId = "";
        // 获取UserId
        userId = request.getHeader("userTempId");
        if(StringUtils.isNotEmpty(request.getHeader("userId"))){
            userId = request.getHeader("userId");
        }

        cartService.addCart(cartInfo,userId);
    }

    @RequestMapping("checkCart/{skuId}/{isChecked}")
    public Result checkCart(@PathVariable Long skuId,@PathVariable Integer isChecked,HttpServletRequest request){
        String userId = "";
        // 获取UserId
        userId = request.getHeader("userTempId");
        if(StringUtils.isNotEmpty(request.getHeader("userId"))){
            userId = request.getHeader("userId");
        }

        cartService.checkCart(skuId,isChecked,userId);
        return Result.ok();
    }

    @RequestMapping("inner/getCartCheckedList/{userId}")
    List<CartInfo> getCartCheckedList(@PathVariable("userId") String userId){
        List<CartInfo> cartInfoList = cartService.cartList(userId);
        Iterator<CartInfo> iterator = cartInfoList.iterator();
        if (iterator.hasNext()){
            if (0 == iterator.next().getIsChecked()){
                iterator.remove();
            }
        }
        return cartInfoList;
    }
}
