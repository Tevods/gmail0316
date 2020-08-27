package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Map;

public interface SpuService {
    IPage<SpuInfo> getSpuPage(Page<SpuInfo> pageParam, String category3Id);

    List<BaseSaleAttr> baseSaleAttrList();

    void saveSpuInfo(SpuInfo spuInfo);

    List<SpuSaleAttr> spuSaleAttrList(String spuId);

    List<SpuImage> spuImageList(String spuId);

    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId);

    Map<String, String> getSkuValueIdsMap(Long spuId);
}
