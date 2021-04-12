package com.atguigu.gmall.cart.service.impl;

import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {
    @Autowired
    ProductFeignClient productFeignClient;
    @Autowired
    CartInfoMapper cartInfoMapper;
    @Autowired
    RedisTemplate redisTemplate;

    @Override
    public void addCart(CartInfo cartInfo, String userId) {

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
        wrapper.eq("sku_id", skuId);
        wrapper.eq("user_id", userId);
        CartInfo cartInfoAdd = cartInfoMapper.selectOne(wrapper); //原商品信息

        if (cartInfoAdd == null) {
            cartInfoMapper.insert(cartInfo);
            redisTemplate.boundHashOps(RedisConst.USER_KEY_PREFIX
                    + cartInfo.getUserId()
                    + RedisConst.USER_CART_KEY_SUFFIX).put(cartInfo.getSkuId() + "", cartInfo);
        } else {
            // 当相同的商品已经存在
            Integer skuNum = cartInfo.getSkuNum(); //新增商品数量
            Integer skuNumAdd = cartInfoAdd.getSkuNum(); //原商品的数量

            cartInfoAdd.setSkuPrice(cartInfo.getSkuPrice());
            cartInfoAdd.setSkuNum(skuNum + skuNumAdd);
            cartInfoAdd.setCartPrice(cartInfoAdd.getSkuPrice().multiply(new BigDecimal(cartInfoAdd.getSkuNum())));

            cartInfoMapper.updateById(cartInfoAdd);
            //同步修改缓存
            redisTemplate.boundHashOps(RedisConst.USER_KEY_PREFIX
                    + cartInfo.getUserId()
                    + RedisConst.USER_CART_KEY_SUFFIX).put(cartInfo.getSkuId() + "", cartInfoAdd);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public List<CartInfo> cartList(String userId, String userTempId) {
        // 此时调用方法传递过来的数据是userId和userTempId
        List<CartInfo> cartInfoList = new ArrayList<>();
        // 如果用户id不存在,直接返回临时购物车的数据
        if (StringUtils.isEmpty(userId)) {
            cartInfoList = this.cartList(userTempId);
            return cartInfoList;
        }

        // 用户id存在
        // 且userTempId存在
        // 准备合并购物车
        if (StringUtils.isNotEmpty(userTempId)) {
            List<CartInfo> cartInfoListTemp = this.cartList(userTempId);
            // 临时购物车中有数据
            if (!CollectionUtils.isEmpty(cartInfoListTemp)) {
                cartInfoList = this.mergeToCartList(cartInfoListTemp, userId);
                // 清除购物车数据
                this.deleteCart(userTempId);
            }
            // 临时购物车中没有数据
            if (CollectionUtils.isEmpty(cartInfoList)){
                cartInfoList = this.cartList(userId);
            }
        }

        return cartInfoList;
    }

    /**
     * 清空临时的数据
     * @param userTempId
     */
    private void deleteCart(String userTempId) {
        // 清除数据库
        QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id",userTempId);
        cartInfoMapper.delete(wrapper);
        // 清除缓存
        redisTemplate.delete(RedisConst.USER_KEY_PREFIX + userTempId + RedisConst.USER_CART_KEY_SUFFIX);
    }

    /**
     * 合并购物车
     * @param cartInfoListTemp
     * @param userId
     * @return
     */
    private List<CartInfo> mergeToCartList(List<CartInfo> cartInfoListTemp, String userId) {
        List<CartInfo> cartInfoList = this.cartList(userId);
        if (!CollectionUtils.isEmpty(cartInfoList)){
            Map<Long, CartInfo> cartInfoMap = cartInfoListTemp.stream().collect(Collectors.toMap(CartInfo::getSkuId, cartInfo -> cartInfo));
            for (CartInfo cartInfoNoLogin : cartInfoList) {
                if (cartInfoMap.containsKey(cartInfoNoLogin.getSkuId())){

                    // 合并数据
                    CartInfo cartInfoLogin = cartInfoMap.get(cartInfoNoLogin.getSkuId());
                    cartInfoNoLogin.setSkuNum(cartInfoLogin.getSkuNum() + cartInfoNoLogin.getSkuNum());
                    // 检查商品的未登录状态
                    if (cartInfoNoLogin.getIsChecked() == 1){
                        cartInfoLogin.setIsChecked(1);
                    }

                    cartInfoMapper.updateById(cartInfoNoLogin);
                }else {
                    cartInfoNoLogin.setId(null);
                    cartInfoNoLogin.setUserId(userId);
                    cartInfoMapper.insert(cartInfoNoLogin);
                }
            }
        }

        List<CartInfo> cartInfos = this.syncCartCache(userId);

        return cartInfos;
    }


    /**
     * 查看购物车
     * @param userId
     * @return
     */
    private List<CartInfo> cartList(String userId) {
        List<CartInfo> cartInfos = new ArrayList<>();
        cartInfos = (List<CartInfo>) redisTemplate.opsForHash().values("user:" + userId + ":cart");

        if (null == cartInfos || cartInfos.size() == 0) {
            cartInfos = syncCartCache(userId);

        }
        return cartInfos;
    }


    @Override
    public void checkCart(Long skuId, Integer isChecked, String userId) {
        CartInfo cartInfo = new CartInfo();
        cartInfo.setIsChecked(isChecked);
        QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("sku_id", skuId);
        int flag = cartInfoMapper.update(cartInfo, wrapper);
        if (flag > 0) {
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
        wrapper.eq("user_id", userId);
        cartInfoList = cartInfoMapper.selectList(wrapper);
        if (cartInfoList != null && cartInfoList.size() > 0) {
            Map<String, Object> map = new HashMap<>();
            cartInfoList.stream().forEach(cartInfo -> {
                SkuInfo skuInfo = productFeignClient.getSkuInfo(cartInfo.getSkuId().toString());
                cartInfo.setSkuPrice(skuInfo.getPrice());
                map.put(cartInfo.getSkuId() + "", cartInfo);
            });
            redisTemplate.boundHashOps(RedisConst.USER_KEY_PREFIX + userId + RedisConst.USER_CART_KEY_SUFFIX).putAll(map);
        }

        return cartInfoList;
    }
}
