package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.BaseAttrInfo;
import com.atguigu.gmall.model.product.BaseAttrValue;

import java.util.List;

public interface BaseInfoService {
    List<BaseAttrInfo> attrInfoList(String category1Id, String category2Id, String category3Id);

    List<BaseAttrValue> getAttrValueList(String attrId);

    void saveAttrInfo(BaseAttrInfo baseAttrInfo);

}
