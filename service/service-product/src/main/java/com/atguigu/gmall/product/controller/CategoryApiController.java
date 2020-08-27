package com.atguigu.gmall.product.controller;



import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseCategory1;
import com.atguigu.gmall.model.product.BaseCategory2;
import com.atguigu.gmall.model.product.BaseCategory3;
import com.atguigu.gmall.product.service.BaseCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("admin/product")
public class CategoryApiController {

    @Autowired
    private BaseCategoryService baseCategoryService;

    @GetMapping("getCategory1")
    public Result getCategory1(){
        List<BaseCategory1> list = baseCategoryService.getCategory1();
        return Result.ok(list);
    }


    @GetMapping("getCategory2/{category1Id}")
    public Result getCategory2(@PathVariable("category1Id") String category1Id){
        List<BaseCategory2> list = baseCategoryService.getCategory2(category1Id);
        return Result.ok(list);
    }

    @GetMapping("getCategory3/{category2Id}")
    public Result getCategory3(@PathVariable("category2Id") String category2Id){
        List<BaseCategory3> list = baseCategoryService.getCategory3(category2Id);
        return Result.ok(list);
    }





}
