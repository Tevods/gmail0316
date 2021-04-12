package com.atguigu.gmall.seckill.service.impl;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.common.util.MD5;
import com.atguigu.gmall.model.activity.OrderRecode;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.user.UserRecode;
import com.atguigu.gmall.seckill.mapper.SeckillGoodsMapper;
import com.atguigu.gmall.seckill.service.SeckillService;
import com.atguigu.gmall.seckill.util.CacheHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;


@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    RabbitService rabbitService;


    @Override
    public void putGoods() {
        // 查询mysql秒杀数据
        List<SeckillGoods> seckillGoods = seckillGoodsMapper.selectList(null);
        for (SeckillGoods seckillGood : seckillGoods) {
            // 放入redis秒杀商品列表，hash
            redisTemplate.opsForHash().put(RedisConst.SECKILL_GOODS,seckillGood.getSkuId()+"",seckillGood);
            //redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).put(seckillGood.getSkuId(),seckillGood);

            Integer num = seckillGood.getNum();
            for (int i = 0; i < num; i++) {
                // 生成秒杀商品库存，list
                redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX+seckillGood.getSkuId()).leftPush(seckillGood.getSkuId()+"");
            }
            // 发布通知消息，通知全体微服务秒杀入库
            redisTemplate.convertAndSend("seckillpush",seckillGood.getSkuId()+":1");// 0 代表下架，1代表上架
        }


    }

    @Override
    public List<SeckillGoods> findAll() {

        List<SeckillGoods> seckillGoods = (List<SeckillGoods>)redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).values();

        return seckillGoods;
    }

    @Override
    public SeckillGoods getItem(Long skuId) {

        SeckillGoods seckillGoods = (SeckillGoods)redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).get(skuId+"");

        return seckillGoods;
    }

    @Override
    public String seckillOrder(Long skuId,String userId) {

        String publish = (String)CacheHelper.get(skuId + "");

        if(publish.equals("1")){
            Boolean b = redisTemplate.opsForValue().setIfAbsent(RedisConst.SECKILL_USER + userId, 1, 10, TimeUnit.SECONDS);
            if(b){
                // 发送抢单的消息队列
                // MessageSeckillUserRecode
                UserRecode messageSeckillUserRecode = new UserRecode();
                messageSeckillUserRecode.setSkuId(skuId);
                messageSeckillUserRecode.setUserId(userId);
                rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_SECKILL_USER,MqConst.ROUTING_SECKILL_USER,messageSeckillUserRecode);
            }
            return "success";
        }else {
            return "non";
        }

    }

    @Override
    public void consumeSeckillOrder(Long skuId, String userId) {

        // 抢库存
        String skuIdFromCache = (String)redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + skuId).rightPop();
        if(StringUtils.isEmpty(skuIdFromCache)){
            // 失败，则发布没有库存的订阅消息
            redisTemplate.convertAndSend("seckillpush",skuId+":0");
        }else {
            // 成功，则生成预订单
            OrderRecode orderRecode = new OrderRecode();
            orderRecode.setUserId(userId);
            orderRecode.setSeckillGoods(this.getSeckillGoods(skuId));
            orderRecode.setNum(1);
            orderRecode.setOrderStr(MD5.encrypt(skuId+userId));//生成下单码

            //订单数据存入Reids
            redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).put(orderRecode.getUserId(), orderRecode);
        }

    }

    private SeckillGoods getSeckillGoods(Long skuId) {

        SeckillGoods seckillGoods = (SeckillGoods)redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).get(skuId+"");

        return seckillGoods;
    }
}
