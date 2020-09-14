package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.product.mapper.BaseTrademarkMapper;
import com.atguigu.gmall.product.service.BaseTradeMarkService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Wrapper;
import java.util.List;

@Service
public class BaseTradeMarkServiceImpl implements BaseTradeMarkService {
    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;

    @Override
    public BaseTrademark getTrademark(Long tmId) {
        BaseTrademark baseTrademark = new BaseTrademark();
        baseTrademark.setId(tmId);
        return baseTrademarkMapper.selectById(baseTrademark);
    }
}
