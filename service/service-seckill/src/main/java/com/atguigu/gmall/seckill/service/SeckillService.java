package com.atguigu.gmall.seckill.service;

import com.atguigu.gmall.model.activity.SeckillGoods;

import java.util.List;

public interface SeckillService {
    void putGoods();

    List<SeckillGoods> findAll();

    SeckillGoods getItem(Long skuId);

    String seckillOrder(Long skuId,String userId);

    void consumeSeckillOrder(Long skuId, String userId);
}
