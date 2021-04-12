package com.atguigu.gmall.seckill.controller;


import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.util.MD5;
import com.atguigu.gmall.model.activity.OrderRecode;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.enums.OrderStatus;
import com.atguigu.gmall.model.enums.PaymentWay;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.client.OrderFeignClient;
import com.atguigu.gmall.seckill.service.SeckillService;
import com.atguigu.gmall.seckill.util.CacheHelper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/activity/seckill")
public class SeckillApiController {

    @Autowired
    SeckillService seckillService;

    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    OrderFeignClient orderFeignClient;


    @RequestMapping("auth/submitOrder")
    Result submitOrder(HttpServletRequest request, @RequestBody OrderInfo order ){
        String userId = request.getHeader("userId");
        OrderRecode orderRecode = (OrderRecode)redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).get(userId);
        SeckillGoods seckillGoods = orderRecode.getSeckillGoods();

        // 保存订单信息(订单表和订单详情表)
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setProcessStatus(ProcessStatus.UNPAID.getComment());
        orderInfo.setOrderStatus(OrderStatus.UNPAID.getComment());
        orderInfo.setTotalAmount(seckillGoods.getPrice());
        orderInfo.setOrderComment("快点");
        orderInfo.setPaymentWay(PaymentWay.ONLINE.getComment());
        orderInfo.setCreateTime(new Date());
        Calendar instance = Calendar.getInstance();
        instance.add(Calendar.DATE,1);
        Date expireTime = instance.getTime();
        orderInfo.setExpireTime(expireTime);// 当前时间new Date()基础上+1天，过期时间-当前时间=倒计时
        orderInfo.setUserId(Long.parseLong(userId));
        orderInfo.setConsigneeTel(order.getConsigneeTel());
        orderInfo.setConsignee(order.getConsignee());
        orderInfo.setDeliveryAddress(order.getDeliveryAddress());
        orderInfo.setImgUrl(seckillGoods.getSkuDefaultImg());
        // 生成系统外部订单号，用来和支付宝进行交易
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String timeFormat = sdf.format(new Date());
        long currentTimeMillis = System.currentTimeMillis();
        String outTradeNo = "atguigu"+currentTimeMillis+timeFormat;
        orderInfo.setOutTradeNo(outTradeNo);//"atguigu"+毫秒时间戳+时间格式化字符串
        List<OrderDetail> orderDetails = new ArrayList<>();
        OrderDetail orderDetail = new OrderDetail();
        BeanUtils.copyProperties(seckillGoods,orderDetail);
        orderDetails.add(orderDetail);
        orderInfo.setOrderDetailList(orderDetails);

        orderInfo = orderFeignClient.saveSeckillOrder(orderInfo);

        return Result.ok(orderInfo.getId());
    }


    @RequestMapping("auth/getSkuInfo/{skuId}")
    SeckillGoods getSkuInfo(@PathVariable("skuId") String skuId){
        return (SeckillGoods)redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).get(skuId+"");
    }

    @RequestMapping("auth/getPreOrder/{userId}")
    OrderRecode getPreOrder(@PathVariable("userId") String userId){
        OrderRecode orderRecode = (OrderRecode)redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).get(userId);
        return orderRecode;
    }

    @RequestMapping("auth/checkOrder/{skuId}")
    public Result checkOrder(@PathVariable("skuId") Long skuId ,HttpServletRequest request){
        String userId = request.getHeader("userId");

        // B 没货，售罄213
        String publish = (String)CacheHelper.get(skuId+"");
        if(StringUtils.isEmpty(publish)||publish.equals("0")){
            return Result.build(null,ResultCodeEnum.SECKILL_FINISH);
        }

        // C 有预订单，215
        Boolean isPreOrder = redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).hasKey(userId);
        if(isPreOrder){
            return Result.build(null,ResultCodeEnum.SECKILL_SUCCESS);
        }
        // D 有正式订单，218
        // redisTemplate.boundListOps(RedisConst.SECKILL_ORDERS_USERS).leftPush(userId);
        boolean isTrueOrder = redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS).hasKey(userId);
        if(isTrueOrder){
            String orderId = (String)redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS).get(userId);
            return Result.build(orderId,ResultCodeEnum.SECKILL_ORDER_SUCCESS);
        }

        // A 有货，没有用户的预订单，也没有正式订单，正在排队211
        return Result.build(null,ResultCodeEnum.SECKILL_RUN);
    }

    @RequestMapping("/auth/seckillOrder/{skuId}")
    public Result seckillOrder(@PathVariable("skuId") Long skuId , String skuIdStr,HttpServletRequest request){
        String userId = request.getHeader("userId");
        
        if(MD5.encrypt(skuId+userId).equals(skuIdStr)){

            String publish = (String)CacheHelper.get(skuId+"");
            if(!StringUtils.isEmpty(publish)&&publish.equals("1")){
                String status  =  seckillService.seckillOrder(skuId,userId);// 发出参加抢单的消息
                if(status.equals("non")){
                    return Result.build(null,ResultCodeEnum.SECKILL_FINISH);
                }
            }
            return Result.ok();
        }else {
            return Result.build(null, ResultCodeEnum.SECKILL_ILLEGAL);
        }
    }

    @RequestMapping("testPush")
    public Result testPush(){

        String push = (String)CacheHelper.get("30");

        return Result.ok();
    }

    @RequestMapping("/auth/getSeckillSkuIdStr/{skuId}")
    public Result getSeckillSkuIdStr(@PathVariable("skuId") Long skuId, HttpServletRequest request){
        String userId = request.getHeader("userId");
        String skuIdStr = MD5.encrypt(skuId + userId);// 算法的调用，尽量多的包含当前请求的元素
        return Result.ok(skuIdStr);
    }


    @RequestMapping("getItem/{skuId}")
    Result getItem(@PathVariable("skuId") Long skuId){
        SeckillGoods seckillGoods = seckillService.getItem(skuId);

        return Result.ok(seckillGoods);
    }

    @RequestMapping("findAll")
    Result findAll(){

        List<SeckillGoods> seckillGoods =  seckillService.findAll();
        return Result.ok(seckillGoods);
    }


    @RequestMapping("putGoods")
    public Result putGoods(){

        // 调用入库服务
        seckillService.putGoods();

        return Result.ok();
    }

}
