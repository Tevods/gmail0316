package com.atguigu.gmall.all.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class PassportController {
    /**
     * 需要携带返回路径给登录的前端的页面获取
     * @param originUrl
     * @param model
     * @return
     */
    @RequestMapping("login.html")
    public String index(String originUrl, Model model){
        model.addAttribute("originUrl",originUrl);
        return "login";
    }
}
