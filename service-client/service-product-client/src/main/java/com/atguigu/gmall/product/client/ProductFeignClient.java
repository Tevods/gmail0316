package com.atguigu.gmall.product.client;

import com.atguigu.gmall.model.product.BaseCategoryView;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@FeignClient(value = "service-product")
public interface ProductFeignClient {

    @RequestMapping("api/product/inner/getPrice/{skuId}")
    BigDecimal getPrice(@PathVariable("skuId") String skuId);

    @RequestMapping("api/product/inner/getSkuInfo/{skuId}")
    SkuInfo getSkuInfo(@PathVariable("skuId") String skuId);

    @RequestMapping("api/product/inner/getCategoryView/{category3Id}")
    BaseCategoryView getCategoryView(@PathVariable("category3Id") Long category3Id);

    @RequestMapping("api/product/inner/getSpuSaleAttrListCheckBySku/{skuId}/{spuId}")
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@PathVariable("skuId") Long skuId,@PathVariable("spuId") Long spuId);

    @RequestMapping("api/product/inner/getSkuValueIdsMap/{spuId}")
    Map<String, String> getSkuValueIdsMap(@PathVariable("spuId") Long spuId);
}