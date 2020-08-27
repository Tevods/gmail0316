package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.client.ItemFeignClient;
import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.Map;

@Controller
public class ItemController {
    @Autowired
    private ItemFeignClient itemFeignClient;

    @RequestMapping("{skuId}.html")
    public String item(@PathVariable String skuId, Model model){
        Map<String,Object> map = new HashMap<>();
        Result result = itemFeignClient.getItem(skuId);
        map = (Map<String,Object>) result.getData();
        model.addAllAttributes(map);
        return "item/index";
    }
}
