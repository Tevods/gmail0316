package com.atguigu.gmall.cart.service.impl;

import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CartServiceImpl implements CartService {
    @Autowired
    ProductFeignClient productFeignClient;
    @Autowired
    CartInfoMapper cartInfoMapper;
    @Autowired
    RedisTemplate redisTemplate;

    @Override
    public void addCart(CartInfo cartInfo,String userId) {

        SkuInfo skuInfo = productFeignClient.getSkuInfo(cartInfo.getSkuId().toString());

        cartInfo.setUserId(userId);
        cartInfo.setIsChecked(1);
        cartInfo.setSkuPrice(skuInfo.getPrice());
        cartInfo.setCartPrice(cartInfo.getSkuPrice().multiply(new BigDecimal(cartInfo.getSkuNum())));
        cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
        cartInfo.setSkuName(skuInfo.getSkuName());

        // 商品不存在直接添加，存在添加数量
        Long skuId = cartInfo.getSkuId();

        QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("sku_id",skuId);
        wrapper.eq("user_id",userId);
        CartInfo cartInfoAdd = cartInfoMapper.selectOne(wrapper); //原商品信息

        if (cartInfoAdd == null) {
            cartInfoMapper.insert(cartInfo);
            redisTemplate.boundHashOps(RedisConst.USER_KEY_PREFIX
                    + cartInfo.getUserId()
                    + RedisConst.USER_CART_KEY_SUFFIX).put(cartInfo.getSkuId()+"",cartInfo);
        }else {
            // 当相同的商品已经存在
            Integer skuNum = cartInfo.getSkuNum(); //新增商品数量
            Integer skuNumAdd = cartInfoAdd.getSkuNum(); //原商品的数量

            cartInfoAdd.setSkuPrice(cartInfo.getSkuPrice());
            cartInfoAdd.setSkuNum(skuNum+skuNumAdd);
            cartInfoAdd.setCartPrice(cartInfoAdd.getSkuPrice().multiply(new BigDecimal(cartInfoAdd.getSkuNum())));

            cartInfoMapper.updateById(cartInfoAdd);
            //同步修改缓存
            redisTemplate.boundHashOps(RedisConst.USER_KEY_PREFIX
                    + cartInfo.getUserId()
                    + RedisConst.USER_CART_KEY_SUFFIX).put(cartInfo.getSkuId()+"",cartInfoAdd);
        }
    }

    @Override
    public List<CartInfo> cartList(String userId) {

        List<CartInfo> cartInfoList = new ArrayList<>();
        cartInfoList = redisTemplate.opsForHash().values(RedisConst.USER_KEY_PREFIX + userId + RedisConst.USER_CART_KEY_SUFFIX);
        if (cartInfoList == null || cartInfoList.size() == 0){
            cartInfoList = syncCartCache(userId);
        }

        return cartInfoList;
    }

    @Override
    public void checkCart(Long skuId, Integer isChecked,String userId) {
        CartInfo cartInfo = new CartInfo();
        cartInfo.setIsChecked(isChecked);
        QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id",userId);
        wrapper.eq("sku_id",skuId);
        int flag = cartInfoMapper.update(cartInfo,wrapper);
        if (flag>0){
            syncCartCache(userId);
        }
    }

    @Override
    public List<CartInfo> getCartCheckedList(String userId) {

        return null;
    }

    private List<CartInfo> syncCartCache(String userId) {
        List<CartInfo> cartInfoList;
        QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id",userId);
        cartInfoList = cartInfoMapper.selectList(wrapper);
        if (cartInfoList!=null && cartInfoList.size()>0){
            Map<String,Object> map = new HashMap<>();
            cartInfoList.stream().forEach(cartInfo -> {
                SkuInfo skuInfo = productFeignClient.getSkuInfo(cartInfo.getSkuId().toString());
                cartInfo.setSkuPrice(skuInfo.getPrice());
                map.put(cartInfo.getSkuId()+"",cartInfo);
            });
            redisTemplate.boundHashOps(RedisConst.USER_KEY_PREFIX + userId + RedisConst.USER_CART_KEY_SUFFIX).putAll(map);
        }

        return cartInfoList;
    }
}
