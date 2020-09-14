package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.model.cart.CartInfo;

import java.util.List;

public interface CartService {
    void addCart(CartInfo cartInfo,String userId);

    List<CartInfo> cartList(String userId);

    void checkCart(Long skuId, Integer isChecked,String userId);

    List<CartInfo> getCartCheckedList(String userId);
}
