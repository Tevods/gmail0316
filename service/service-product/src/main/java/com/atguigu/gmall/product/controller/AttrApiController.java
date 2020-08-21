package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseAttrInfo;
import com.atguigu.gmall.model.product.BaseAttrValue;
import com.atguigu.gmall.model.product.BaseCategory1;
import com.atguigu.gmall.product.service.BaseInfoService;
import com.sun.xml.internal.bind.v2.TODO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("admin/product")
public class AttrApiController {

    @Autowired
    private BaseInfoService baseInfoService;

    @GetMapping("attrInfoList/{category1Id}/{category2Id}/{category3Id}")
    public Result attrInfoList(@PathVariable("category1Id") String category1Id,
                               @PathVariable("category2Id") String category2Id,
                               @PathVariable("category3Id") String category3Id){
        List<BaseAttrInfo> baseAttrInfoList = baseInfoService.attrInfoList(category1Id,category2Id,category3Id);
        return Result.ok(baseAttrInfoList);
    }

    @GetMapping("getAttrValueList/{attrId}")
    public Result getAttrValueList(@PathVariable("attrId") String attrId){
        List<BaseAttrValue> baseAttrValueList = baseInfoService.getAttrValueList(attrId);
        return Result.ok(baseAttrValueList);
    }

    @PostMapping("saveAttrInfo")
    public Result saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo){
        baseInfoService.saveAttrInfo(baseAttrInfo);
        return Result.ok();
    }
}
