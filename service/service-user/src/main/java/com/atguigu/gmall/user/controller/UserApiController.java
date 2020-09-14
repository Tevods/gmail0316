package com.atguigu.gmall.user.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.service.UserAddressService;
import com.atguigu.gmall.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/user/passport")
public class UserApiController {

    @Autowired
    UserService userService;
    @Autowired
    UserAddressService userAddressService;
    /**
     * 校验token
     * @param token
     * @return
     */
    @RequestMapping("/inner/verify/{token}")
    public String verify(@PathVariable("token") String token){
        String userId = userService.verify(token);
        return userId;
    }

    /**
     * 页面ajax异步登录
     * @param userInfo
     * @return
     */
    @RequestMapping("login")
    public Result login(@RequestBody UserInfo userInfo){
        Map<String,Object> map = userService.login(userInfo);
        if (map != null){
            return Result.ok(map);
        }
        return Result.fail();
    }

    @RequestMapping("inner/getUserAddrByUserId/{userId}")
    List<UserAddress> getUserAddrByUserId(@PathVariable("userId") String userId){
        return userAddressService.getUserAddrByUserId(userId);
    }
}
