package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.model.product.BaseCategoryView;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.product.service.BaseCategoryService;
import com.atguigu.gmall.product.service.SkuInfoService;
import com.atguigu.gmall.product.service.SpuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/product")
@CrossOrigin
public class ProductApiController {

    @Autowired
    private SkuInfoService skuInfoService;
    @Autowired
    private BaseCategoryService baseCategoryService;
    @Autowired
    private SpuService spuService;

    @RequestMapping("inner/getPrice/{skuId}")
    public BigDecimal getPrice(@PathVariable("skuId") String skuId){
        BigDecimal price = skuInfoService.getPrice(skuId);
        return price;
    }

    @RequestMapping("inner/getSkuInfo/{skuId}")
    public SkuInfo getSkuInfo(@PathVariable("skuId") String skuId){
        return skuInfoService.getSkuInfo(skuId);
    }

    @RequestMapping("inner/getCategoryView/{category3Id}")
    public BaseCategoryView getCategoryView(@PathVariable("category3Id") Long category3Id){
        BaseCategoryView categoryView = baseCategoryService.getCategoryView(category3Id);
        return categoryView;
    }

    @RequestMapping("inner/getSpuSaleAttrListCheckBySku/{skuId}/{spuId}")
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@PathVariable("skuId") Long skuId,@PathVariable("spuId")  Long spuId){
        //选中方案的解决
        List<SpuSaleAttr> list = spuService.getSpuSaleAttrListCheckBySku(skuId,spuId);
        //这里拿到skuId是作为比较条件
        return list;
    }

    @RequestMapping("inner/getSkuValueIdsMap/{spuId}")
    Map<String, String> getSkuValueIdsMap(@PathVariable("spuId") Long spuId){
        Map<String,String> map = spuService.getSkuValueIdsMap(spuId);
        return map;
    }
}
