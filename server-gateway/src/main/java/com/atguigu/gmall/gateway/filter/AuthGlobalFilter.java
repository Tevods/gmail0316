package com.atguigu.gmall.gateway.filter;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.client.UserFeignClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class AuthGlobalFilter implements GlobalFilter {

    AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Value("${authUrls.url}")
    private String authUrls;

    @Autowired
    UserFeignClient userFeignClient;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 获取请求
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        // 获取请求路径
        String uri = request.getURI().toString();
        String path = request.getPath().toString();

        // 不拦截认证中心的请求和图片请求
        if(uri.indexOf("passport")!=-1 || uri.indexOf("ico")!=-1 ||uri.indexOf("css")!=-1||uri.indexOf("js")!=-1||uri.indexOf("png")!=-1||uri.indexOf("jpg")!=-1){
            return chain.filter(exchange);
        }

        /*
         * 黑名单鉴权：
         * 1.访问方式不同，黑名单拦截访问内部接口ajax的异步请求，白名单是页面同步请求
         * 2.返回结果不同，白名单返回页面，黑名单返回json字符串
         */
        boolean inner = antPathMatcher.match("/api/**/inner/**",path);
        if (inner){
            return out(response,ResultCodeEnum.PERMISSION);
        }

        String token = getToken(request);
        String userId = "";
        //校验token是否合法，和redis中存储的token进行比较
        if (StringUtils.isNotEmpty(token)){
            userId = userFeignClient.verify(token);
        }

        boolean auth = antPathMatcher.match("/api/**/auth/**",path);
        if (auth){
            //鉴权 如果没有token获取token调用用户认证中心得不到userId那么这时我们就认为该用户还没有登录
            if (StringUtils.isEmpty(userId)){
                return out(response,ResultCodeEnum.PERMISSION);
            }
        }


        /*
         *
         * 白名单鉴权
         *
         * 1.获取token信息
         * 2.网关中通过feign调用cas服务器,校验返回userId
         */
        String[] split = authUrls.split(",");
        for (String authUrl : split) {
            if (path.indexOf(authUrl)!=-1){
                //鉴权 如果没有token获取token调用用户认证中心得不到userId那么这时我们就认为该用户还没有登录
                if (StringUtils.isEmpty(userId)){
                    //重定向到登录界面
                    response.setStatusCode(HttpStatus.SEE_OTHER);
                    response.getHeaders().set(HttpHeaders.LOCATION,"http://passport.gmall.com/login.html?originUrl="+uri);
                    Mono<Void> voidMono = response.setComplete();
                    return voidMono;
                }

            }
        }

        // program run to this,case 2:
        // 1.程序访问的资源没有被网关拦截
        // 2.用户已经登录
        // 这时将userId放入请求头中方便其他的前台调用
        if (!StringUtils.isEmpty(userId)){
            request.mutate().header("userId",userId).build();
            exchange.mutate().request(request).build();
        }else{
            // 前端cookie不改变，userTempId不会改变
            String userTempId = getUserTempId(request);
            request.mutate().header("userTempId",userTempId).build();
            exchange.mutate().request(request).build();
        }

        return chain.filter(exchange);
    }

    private String getToken(ServerHttpRequest request) {
        String token = "";
        //从cookie中获取token
        MultiValueMap<String, HttpCookie> cookies = request.getCookies();
        if (null != cookies){
            HttpCookie tokenCookie = cookies.getFirst("token");
            if (null != tokenCookie){
                token = tokenCookie.getValue();
            }
        }

        // 登录状态的异步访问
        if (StringUtils.isEmpty(token)){
            token = request.getHeaders().getFirst("token");
        }

        return token;
    }

    private String getUserTempId(ServerHttpRequest request) {
        String userTempId = "";
        //从cookie中获取token
        MultiValueMap<String, HttpCookie> cookies = request.getCookies();
        if (null != cookies){
            HttpCookie userTempIdCookie = cookies.getFirst("userTempId");
            if (null != userTempIdCookie){
                userTempId = userTempIdCookie.getValue();
            }
        }

        // 如果cookie中没有，可能是异步访问，尝试从header中获取，处理异步访问
        if (StringUtils.isEmpty(userTempId)){
            userTempId = request.getHeaders().getFirst("userTempId");
        }

        return userTempId;
    }

    private Mono<Void> out(ServerHttpResponse response, ResultCodeEnum resultCodeEnum) {
        // 返回用户没有权限登录
        Result<Object> result = Result.build(null, resultCodeEnum);
        byte[] bits = JSONObject.toJSONString(result).getBytes(StandardCharsets.UTF_8);
        DataBuffer wrap = response.bufferFactory().wrap(bits);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        // 输出到页面
        return response.writeWith(Mono.just(wrap));
    }
}
