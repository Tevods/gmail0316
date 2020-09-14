package com.atguigu.gmall.cart.client;

import com.atguigu.gmall.model.cart.CartInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@FeignClient(value = "service-cart")
public interface CartFeignClient {
    @RequestMapping("api/cart/cartTest")
    String cartTest();

    @RequestMapping("api/cart/addCart")
    void addCart(CartInfo cartInfo);

    @RequestMapping("api/cart/inner/getCartCheckedList/{userId}")
    List<CartInfo> getCartCheckedList(@PathVariable("userId") String userId);
}
