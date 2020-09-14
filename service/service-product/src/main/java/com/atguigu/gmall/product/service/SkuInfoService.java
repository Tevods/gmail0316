package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.product.SpuInfo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.awt.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface SkuInfoService {
    void saveSkuInfo(SkuInfo skuInfo);

    IPage<SkuInfo> list(Page<SkuInfo> pageParam);

    void onSale(Long skuId);

    void cancelSale(Long skuId);

    BigDecimal getPrice(String skuId);

    SkuInfo getSkuInfo(String skuId);

}
