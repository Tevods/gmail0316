package com.atguigu.gmall.product.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.model.product.BaseAttrInfo;
import com.atguigu.gmall.model.product.BaseAttrValue;
import com.atguigu.gmall.product.mapper.BaseAttrInfoMapper;
import com.atguigu.gmall.product.mapper.BaseAttrValueMapper;
import com.atguigu.gmall.product.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.product.service.BaseAttrInfoService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BaseAttrInfoServiceImpl implements BaseAttrInfoService {

    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;
    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;
    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Override
    public List<BaseAttrInfo> attrInfoList(String category1Id, String category2Id, String category3Id) {
        QueryWrapper<BaseAttrInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("category_id",category3Id);
        wrapper.eq("category_level",3);
        List<BaseAttrInfo> baseAttrInfoList = baseAttrInfoMapper.selectList(wrapper);
        //将商品属性类别和商品各个类别的详细信息一起查询出来
        for (BaseAttrInfo baseAttrInfo : baseAttrInfoList) {
            long attrId = baseAttrInfo.getId();
            QueryWrapper<BaseAttrValue> wrapper1 = new QueryWrapper<>();
            wrapper1.eq("attr_id",attrId);
            List<BaseAttrValue> baseAttrValues = baseAttrValueMapper.selectList(wrapper1);
            baseAttrInfo.setAttrValueList(baseAttrValues);
        }
        return baseAttrInfoList;
    }

    @Override
    public List<BaseAttrValue> getAttrValueList(String attrId) {
        QueryWrapper<BaseAttrValue> wrapper = new QueryWrapper<>();
        wrapper.eq("attr_id",attrId);
        List<BaseAttrValue> baseAttrValues = baseAttrValueMapper.selectList(wrapper);
        return baseAttrValues;
    }

    @Override
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {

        Long attrId = baseAttrInfo.getId();
        if (attrId!=null){
            //修改属性表
            baseAttrInfoMapper.updateById(baseAttrInfo);
            //删除属性值
            QueryWrapper<BaseAttrValue> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("attr_id",attrId);
            baseAttrValueMapper.delete(queryWrapper);
            //无论使新增还是修改都需要新增属性值
            insertAttrInfoAndValue(baseAttrInfo,attrId);
        }else {
            //如果属性表不存再就新增属性表
            baseAttrInfoMapper.insert(baseAttrInfo);
            //无论使新增还是修改都需要新增属性值
            insertAttrInfoAndValue(baseAttrInfo);
        }

    }
    //当attr存在时
    private void insertAttrInfoAndValue(BaseAttrInfo baseAttrInfo,Long attrId){
        //插入属性值
        for (BaseAttrValue baseAttrValue : baseAttrInfo.getAttrValueList()) {
            baseAttrValue.setAttrId(attrId);
            baseAttrValueMapper.insert(baseAttrValue);
        }
    }
    //当attr不存在时
    private void insertAttrInfoAndValue(BaseAttrInfo baseAttrInfo){
        //插入属性值
        QueryWrapper<BaseAttrInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("attr_name",baseAttrInfo.getAttrName());
        wrapper.eq("category_id",baseAttrInfo.getCategoryId());
        wrapper.eq("category_level",baseAttrInfo.getCategoryLevel());
        BaseAttrInfo newBaseAttrInfo = baseAttrInfoMapper.selectOne(wrapper);
        for (BaseAttrValue baseAttrValue : baseAttrInfo.getAttrValueList()) {
            baseAttrValue.setAttrId(newBaseAttrInfo.getId());
            baseAttrValueMapper.insert(baseAttrValue);
        }
    }



    @Override
    public List<BaseAttrInfo> getBaseAttr(String skuId) {
        return skuAttrValueMapper.selectBaseAttrInfoListBySkuId(skuId);
    }

}
