package com.atguigu.gmall.user.service.impl;

import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.mapper.UserServiceMapper;
import com.atguigu.gmall.user.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    UserServiceMapper userServiceMapper;

    @Autowired
    RedisTemplate redisTemplate;

    @Override
    public Map<String, Object> login(UserInfo userInfo) {
        Map<String,Object> map = null;
        QueryWrapper<UserInfo> wrapper = new QueryWrapper<>();
        String name = userInfo.getLoginName();
        String passwd = DigestUtils.md5DigestAsHex(userInfo.getPasswd().getBytes());
        wrapper.eq("login_name",name);
        wrapper.eq("passwd",passwd);
        UserInfo selectUserInfo = userServiceMapper.selectOne(wrapper);
        if (selectUserInfo != null){
            map = new HashMap<>();
            String token = UUID.randomUUID().toString().replace("-","");
            map.put("name",selectUserInfo.getName());
            map.put("nickName",selectUserInfo.getNickName());
            map.put("token",token);
            redisTemplate.opsForValue().set("user:login:"+token,selectUserInfo.getId().toString());
        }

        return map;
    }

    @Override
    public String verify(String token) {
        String userId = (String) redisTemplate.opsForValue().get(RedisConst.USER_LOGIN_KEY_PREFIX + token);
        return userId;
    }


}
