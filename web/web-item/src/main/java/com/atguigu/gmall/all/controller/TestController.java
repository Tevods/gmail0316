package com.atguigu.gmall.all.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
public class TestController {
    @RequestMapping("test")
    public String test(ModelMap modelMap){
        List<String> list = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            list.add("元素"+i);
        }

        modelMap.put("value",1);
        modelMap.put("list",list);
        modelMap.put("num",1);
        return "test";
    }
}
