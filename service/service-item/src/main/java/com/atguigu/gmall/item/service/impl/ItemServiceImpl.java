package com.atguigu.gmall.item.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.product.BaseCategoryView;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@Service
public class ItemServiceImpl implements ItemService {
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;
    @Autowired
    private ListFeignClient listFeignClient;

    @Override
    public Map<String, Object> getItem(String skuId) {
        // 商品详情汇总封装基础数据
        Map<String,Object> map = new HashMap<>();
        long start = System.currentTimeMillis();
        map = getStringObjectMapThread(skuId);
//        map = getStringObjectMap(skuId);
        long end = System.currentTimeMillis();
        System.out.println((end - start) + "ms");
        return map;
    }

    private Map<String, Object> getStringObjectMapThread(String skuId) {
        // 商品详情汇总封装基础数据
        Map<String,Object> map = new HashMap<>();
        // 通过多线程访问获取数据
        CompletableFuture<SkuInfo> completableFutureSkuInfo = CompletableFuture.supplyAsync(() -> {
            // 查询sku详细信息sku_info
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            map.put("skuInfo",skuInfo);
            return skuInfo;
        },threadPoolExecutor);

        CompletableFuture<Void> completableFuturePrice = CompletableFuture.runAsync(()->{
            // 商品价格查询
            BigDecimal price = new BigDecimal("0");
            price = productFeignClient.getPrice(skuId);
            map.put("price",price);
        },threadPoolExecutor);

        CompletableFuture<Void> completableFutureCategoryView = completableFutureSkuInfo.thenAcceptAsync(skuInfo -> {
            // 查询分类列表
            BaseCategoryView baseCategoryView = productFeignClient.getCategoryView(285L); //TODO 商品分类列表还是手动添加的
            map.put("categoryView",baseCategoryView);
        },threadPoolExecutor);

        CompletableFuture<Void> completableFutureSaleAttrList = completableFutureSkuInfo.thenAcceptAsync(skuInfo -> {
            // 查询销售属性
            List<SpuSaleAttr> spuSaleAttrList = productFeignClient.getSpuSaleAttrListCheckBySku(Long.parseLong(skuId),skuInfo.getSpuId());
            map.put("spuSaleAttrList",spuSaleAttrList);
        },threadPoolExecutor);

        CompletableFuture<Void> completableFutureMap = completableFutureSkuInfo.thenAcceptAsync(skuInfo -> {
            // 选中的切换操作
            Map<String,String> skuValueIdsMap = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
            map.put("valuesSkuJson", JSON.toJSONString(skuValueIdsMap));
        },threadPoolExecutor);

        // 商品热度更新
        CompletableFuture<Void> completableFutureHotScore = CompletableFuture.runAsync(()->{
            listFeignClient.hotScore(skuId);
        },threadPoolExecutor);

        CompletableFuture.allOf(completableFutureSkuInfo
                ,completableFuturePrice
                ,completableFutureCategoryView
                ,completableFutureMap
                ,completableFutureSaleAttrList
                ,completableFutureHotScore).join();

        return map;
    }

    private Map<String, Object> getStringObjectMap(String skuId) {
        // 商品详情汇总封装基础数据
        Map<String,Object> map = new HashMap<>();
        // 商品价格查询 如何知道前端需要的是一个map？
        BigDecimal price = new BigDecimal("0");
        price = productFeignClient.getPrice(skuId);
        map.put("price",price);
        // 查询sku详细信息sku_info
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        map.put("skuInfo",skuInfo);
        // 查询分类列表
        BaseCategoryView baseCategoryView = productFeignClient.getCategoryView(285L);
        map.put("categoryView",baseCategoryView);
        // 查询销售属性
        List<SpuSaleAttr> spuSaleAttrList = productFeignClient.getSpuSaleAttrListCheckBySku(Long.parseLong(skuId),skuInfo.getSpuId());
        map.put("spuSaleAttrList",spuSaleAttrList);
        // 选中的切换操作
        Map<String,String> skuValueIdsMap = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
        map.put("valuesSkuJson", JSON.toJSONString(skuValueIdsMap));
        return map;
    }
}
