package com.atguigu.gmall.common.cache;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@Aspect //切面
public class GmallCacheAspect {

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    RedissonClient redissonClient;

    //环绕通知使用的位置，为存在注解XXX的位置，具体需要看使用注解的是类还是方法
    @Around("@annotation(com.atguigu.gmall.common.cache.GmallCache)")
    public Object cacheAroundAdvice(ProceedingJoinPoint point){ //切点
        Object result = null;
        System.out.println("环绕通知，被执行方法前");
        //获得参数
        Object[] args = point.getArgs();
        //基本信息
        MethodSignature signature = (MethodSignature) point.getSignature();
        //为了获取注解中的前缀
        GmallCache annotation = signature.getMethod().getAnnotation(GmallCache.class);
        String key = annotation.prefix()+":"+args[0];
        //查询缓存
        result = cacheHit(signature,key);
        //执行被代理方法
        if (null == result){
            // 获得分布式锁
            String lockId = UUID.randomUUID().toString();
            Boolean lock = redisTemplate.opsForValue().setIfAbsent(key + ":lock", lockId, 10, TimeUnit.SECONDS);
//            RLock lock1 = redissonClient.getLock(key + ":lock");
//            lock1.lock(10,TimeUnit.SECONDS);
            if (lock){
                try {
                    result = point.proceed(); //执行被代理的方法，访问数据库
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
                //同步数据到缓存
                if (null!=result){
                    redisTemplate.opsForValue().set(key, JSON.toJSONString(result));
                }else {
                    Object obj = null;
                    try {
                        obj = signature.getReturnType().newInstance();
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    //缓存null数据,返回的是查询的对象，也就是与返回值类型有关？？
                    redisTemplate.opsForValue().set(key,JSON.toJSONString(obj),10,TimeUnit.SECONDS);
                }
                // 删除分布式锁
                // 使用lua脚本删除锁
                String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                // 设置lua脚本返回的数据类型
                DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
                // 设置lua脚本返回类型为Long
                redisScript.setResultType(Long.class);
                redisScript.setScriptText(script);
                redisTemplate.execute(redisScript, Arrays.asList(key+":lock"),lockId);
            }else{
                // 没获取到锁，自旋
                return cacheHit(signature,key);
            } //释放锁后
        } //被代理方法执行结束，数据成功缓存
        return result;
    }

    /**
     *
     * @param signature 可以获取方法的返回值
     * @param key 需要获取数据使用
     * @return
     */
    private Object cacheHit(MethodSignature signature,String key){
        String cache = (String) redisTemplate.opsForValue().get(key);

        if (StringUtils.isNotBlank(cache)){
            Class returnType = signature.getReturnType(); //获取。。。的返回值类型
            return JSONObject.parseObject(cache,returnType); //
        }else {
            return null;
        }
    }
}
