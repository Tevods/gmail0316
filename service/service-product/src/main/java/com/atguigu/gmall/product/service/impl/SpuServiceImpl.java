package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.mapper.*;
import com.atguigu.gmall.product.service.SpuService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SpuServiceImpl implements SpuService {

    @Autowired
    private SpuMapper spuMapper;
    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;
    @Autowired
    private SpuImageMapper spuImageMapper;
    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;
    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Override
    public IPage<SpuInfo> getSpuPage(Page<SpuInfo> pageParam, String category3Id) {
        QueryWrapper<SpuInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("category3_id",category3Id);
        return spuMapper.selectPage(pageParam, queryWrapper);
    }

    @Override
    public List<BaseSaleAttr> baseSaleAttrList() {
        return baseSaleAttrMapper.selectList(null);
    }

    @Override
    public void saveSpuInfo(SpuInfo spuInfo) {
        spuMapper.insert(spuInfo);
        Long spuId = spuInfo.getId();

        //保存图片
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if (spuImageList!=null){
            for (SpuImage spuImage : spuImageList) {
                spuImage.setSpuId(spuId);
                spuImageMapper.insert(spuImage);
            }
        }
        //保存销售属性
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if (null!=spuSaleAttrList){
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                spuSaleAttr.setSpuId(spuId);
                spuSaleAttrMapper.insert(spuSaleAttr);
                //保存销售属性值
                String saleAttrName = spuSaleAttr.getSaleAttrName();
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                    //
                    spuSaleAttrValue.setSaleAttrName(saleAttrName);
                    spuSaleAttrValue.setSpuId(spuId);
                    spuSaleAttrValueMapper.insert(spuSaleAttrValue);
                }
            }
        }
    }

    @Override
    public List<SpuSaleAttr> spuSaleAttrList(String spuId) {
        //TODO 非空判断
        //TODO XML改造
        QueryWrapper<SpuSaleAttr> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("spu_id",spuId);
        List<SpuSaleAttr> spuSaleAttrList = spuSaleAttrMapper.selectList(queryWrapper);
        for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
            QueryWrapper<SpuSaleAttrValue> valueQueryWrapper = new QueryWrapper<>();
            valueQueryWrapper.eq("spu_id",spuId); //商品spu的id
            valueQueryWrapper.eq("base_sale_attr_id",spuSaleAttr.getBaseSaleAttrId()); //颜色/版本/型号
            List<SpuSaleAttrValue> spuSaleAttrValues = spuSaleAttrValueMapper.selectList(valueQueryWrapper);
            spuSaleAttr.setSpuSaleAttrValueList(spuSaleAttrValues);
        }
        return spuSaleAttrList;
    }

    @Override
    public List<SpuImage> spuImageList(String spuId) {
        QueryWrapper<SpuImage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("spu_id",spuId);
        List<SpuImage> spuImageList = spuImageMapper.selectList(queryWrapper);
        return spuImageList;
    }
}
