package com.atguigu.gmall.list.service;

import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;

public interface ListService {

    void onSale(String skuId);

    void cancelSale(String skuId);

    void hotScore(String skuId);

    SearchResponseVo list(SearchParam searchParam);
}
