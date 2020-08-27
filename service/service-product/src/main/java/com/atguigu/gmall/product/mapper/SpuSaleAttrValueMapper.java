package com.atguigu.gmall.product.mapper;

import com.atguigu.gmall.model.product.SpuSaleAttrValue;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface SpuSaleAttrValueMapper extends BaseMapper<SpuSaleAttrValue> {
    List<Map<String, Object>> selectSkuValueIdsMap(Long spuId);
}
