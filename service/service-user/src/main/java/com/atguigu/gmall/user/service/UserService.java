package com.atguigu.gmall.user.service;

import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.model.user.UserInfo;

import java.util.List;
import java.util.Map;

public interface UserService {
    Map<String, Object> login(UserInfo userInfo);

    String verify(String token);

}
