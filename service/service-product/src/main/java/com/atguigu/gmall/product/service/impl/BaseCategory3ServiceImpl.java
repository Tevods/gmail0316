package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.BaseCategory2;
import com.atguigu.gmall.model.product.BaseCategory3;
import com.atguigu.gmall.product.mapper.BaseCategory2Mapper;
import com.atguigu.gmall.product.mapper.BaseCategory3Mapper;
import com.atguigu.gmall.product.service.BaseCategory2Service;
import com.atguigu.gmall.product.service.BaseCategory3Service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BaseCategory3ServiceImpl implements BaseCategory3Service {

    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;


    @Override
    public List<BaseCategory3> getCategory3(String category2Id) {
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("category2_id",category2Id);
        List<BaseCategory3> category3List = baseCategory3Mapper.selectList(queryWrapper);
        return category3List;
    }
}
