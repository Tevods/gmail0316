package com.atguigu.gmall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.cache.GmallCache;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.mapper.*;
import com.atguigu.gmall.product.service.SkuInfoService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class SkuInfoServiceImpl implements SkuInfoService {
    @Autowired
    private SkuInfoMapper skuInfoMapper;
    @Autowired
    private SkuImageMapper skuImageMapper;
    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;
    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private ListFeignClient listFeignClient;

    @Override
    public void saveSkuInfo(SkuInfo skuInfo) {
        //插入skuInfo
        skuInfoMapper.insert(skuInfo);
        Long skuId = skuInfo.getId();
        //插入picture信息
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        //TODO 测试null数据插入的话会有什么效果
        for (SkuImage skuImage : skuImageList) {
            skuImage.setSkuId(skuId);
            skuImageMapper.insert(skuImage);
        }
        //关联平台属性信息
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        for (SkuAttrValue skuAttrValue : skuAttrValueList) {
            skuAttrValue.setSkuId(skuId);
            skuAttrValueMapper.insert(skuAttrValue);
        }
        //关联销售属性信息
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
            skuSaleAttrValue.setSkuId(skuId);
            skuSaleAttrValue.setSpuId(skuInfo.getSpuId());
            skuSaleAttrValueMapper.insert(skuSaleAttrValue);
        }
    }

    @Override
    public IPage<SkuInfo> list(Page<SkuInfo> pageParam) {
        return  skuInfoMapper.selectPage(pageParam,null);
    }

    @Override
    public void onSale(Long skuId) {
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo.setIsSale(1);
        skuInfoMapper.updateById(skuInfo);
        //将信息放到es中去
        listFeignClient.onSale(skuId+"");
    }

    @Override
    public void cancelSale(Long skuId) {
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo.setIsSale(0);
        //将商品信息存放到es中去
        skuInfoMapper.updateById(skuInfo);
        listFeignClient.cancelSale(skuId+"");
    }


    @Override
    public BigDecimal getPrice(String skuId) {
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        BigDecimal price = skuInfo.getPrice();
        return price;
    }

    @GmallCache
    @Override
    public SkuInfo getSkuInfo(String skuId) {
        SkuInfo skuInfo = new SkuInfo();
        // 查询db
        System.out.println("被代理方法执行，查询skuInfo的db");
        skuInfo = getSkuInfoFromDb(skuId);
        return skuInfo;
    }


    public SkuInfo getSkuInfoForCache(String skuId) {
        //TODO 熟悉Redis缓存
        System.out.println(Thread.currentThread().getName()+"访问sku详细信息");
        SkuInfo skuInfo = null;
        //先从缓存中读取数据
        String skuStrFromCache = (String) redisTemplate.opsForValue().get("sku:" + skuId + ":info");
        if (StringUtils.isEmpty(skuStrFromCache)){
            System.out.println(Thread.currentThread().getName()+"缓存中没有数据，申请分布式锁");
            String lockId = UUID.randomUUID().toString(); //为了保证每个分布式锁不一样？？
            //设置锁
            Boolean lock = redisTemplate.opsForValue().setIfAbsent("sku:" + skuId + ":lock", lockId, 10, TimeUnit.SECONDS);
            //加锁是为了解决访问数据库的问题
            if (lock){
                //没有，从数据库读取数据
                System.out.println(Thread.currentThread().getName()+"缓存中没有数据，拿到了分布式锁，开始访问db");
                //开始访问db
                skuInfo = getSkuInfoFromDb(skuId);
                //保存数据到redis
                if (skuInfo!=null){
                    redisTemplate.opsForValue().set("sku:" + skuId + ":info", JSON.toJSONString(skuInfo));
                }else {
                    //生成Null的redis数据，防止redis缓存穿透，同时设置过期时间
                    redisTemplate.opsForValue().set("sku:" + skuId + ":info",JSON.toJSONString(new SkuInfo()),10,TimeUnit.SECONDS);
                }
                //归还锁
                System.out.println(Thread.currentThread().getName()+"归还分布式锁");
                //???为什么要休眠
                    //删除本线程的锁，判断后删除
                    /*String lockIdFromCache = (String) redisTemplate.opsForValue().get("sku:"+skuId+":lock");
                    if (StringUtils.isNotBlank(lockIdFromCache) && lockIdFromCache.equals(lockId)){
                        redisTemplate.delete("sku:"+skuId+":lock");
                    }*/
                    //使用lua脚本删除锁
                String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                // 设置lua脚本返回的数据类型
                DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
                // 设置lua脚本返回类型为Long
                redisScript.setResultType(Long.class);
                redisScript.setScriptText(script);
                redisTemplate.execute(redisScript, Arrays.asList("sku:"+skuId+":lock"),lockId);
            }else {
                //没获取到锁的自旋
                System.out.println(Thread.currentThread().getName()+"没有获取到分布式锁，开始自旋");
                return getSkuInfo(skuId);
            }
        }else {
            //缓存中有数据
            skuInfo = JSON.parseObject(skuStrFromCache,SkuInfo.class); //将缓存的数据提取出来
        }
        return skuInfo;
    }

    private SkuInfo getSkuInfoFromDb(String skuId){
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        QueryWrapper<SkuImage> wrapper = new QueryWrapper<>();
        wrapper.eq("sku_id",skuId);
        List<SkuImage> skuImages = skuImageMapper.selectList(wrapper);
        skuInfo.setSkuImageList(skuImages);
        return skuInfo;
    }


}
