package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.BaseCategory1;
import com.atguigu.gmall.model.product.BaseCategory2;
import com.atguigu.gmall.product.mapper.BaseCategory1Mapper;
import com.atguigu.gmall.product.mapper.BaseCategory2Mapper;
import com.atguigu.gmall.product.service.BaseCategory1Service;
import com.atguigu.gmall.product.service.BaseCategory2Service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BaseCategory2ServiceImpl implements BaseCategory2Service {

    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;


    @Override
    public List<BaseCategory2> getCategory2(String category1Id) {
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("category1_id",category1Id);
        List<BaseCategory2> category2List = baseCategory2Mapper.selectList(queryWrapper);
        return category2List;
    }
}
