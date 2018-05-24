package com.hxjb.secondkill.service;

import com.hxjb.secondkill.mq.SecKillSender;
import com.hxjb.secondkill.util.JsonUtils;
import com.hxjb.secondkill.util.SnowflakeIdWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class SecondKillService {

    private static Logger logger = LoggerFactory.getLogger(SecondKillService.class);


    private static String keyPrefix = "tq";

    @Autowired
    private StringRedisTemplate template;


    @Autowired
    SecKillSender secKillSender;


    public Mono<String> makeOrder(String goodsId, String userId) {
        String stockKey = keyPrefix + ":stock:" + goodsId;
        String buyListKey = keyPrefix + ":buyList:" + goodsId;
        return template.execute(new SessionCallback<Mono<String>>() {
            @Override
            public <K, V> Mono<String> execute(RedisOperations<K, V> redisOperations) throws DataAccessException {
                String result = "0";
                // 监听orderCount字段 如果在事务提交的时候发现有其它client修改了orderCount值的话
                // 提交就会失败 List<Object> exec 就会返回null
                template.watch(stockKey);
                // redis中get set操作具有原子性不用担心并发读写问题
                String orderCount = template.opsForValue().get(stockKey);
                Integer count = Integer.valueOf(orderCount);
                if (count > 0) {
                    // 开启事务
                    redisOperations.multi();
                    // 库存减1
                    template.opsForValue().increment(stockKey, -1);
                    // 提交事务
                    List<Object> exec = redisOperations.exec();
                    // 提交事务失败则进行重新提交请求
                    if (exec == null || exec.size() == 0) {
                        // logger.info("并发冲突执行抢购失败逻辑--------");
                        makeOrder(goodsId, userId);
                    } else {
                        for (Object o : exec) {
                            logger.info("抢购成功，还剩--->" + o);
                            SnowflakeIdWorker idWorker = new SnowflakeIdWorker(0, 0);
                            long orderNum = idWorker.nextId();
                            template.opsForList().leftPush(buyListKey, userId);
                            // 添加默认订单状态到redis中
                            template.opsForValue().set("orderState:" + userId + ":" + goodsId, "1");
                            // 同步改异步，这块选择rabbitmq
                            Map<String, Object> payload = new HashMap<>();
                            payload.put("userId", userId);
                            payload.put("goodsId", goodsId);
                            payload.put("orderNum", orderNum);
                            secKillSender.send(payload);
                            result = o + "";
                        }
                    }
                } else {
                    logger.info("卖完了");
                }
                return Mono.justOrEmpty(result);
            }
        });
    }

    public boolean checkBuyLimit(String goodsId, String userId, Integer limit) {
        String buyListKey = keyPrefix + ":buyList:" + goodsId;
        // 此处我们利用redis set 处理一个用户只能购买固定件数件商品
        List<String> buyList = template.opsForList().range(buyListKey, 0, -1);

        if (buyList.stream().filter(item -> item.equals(userId)).count() >= limit) {
            logger.info("不能重复抢购");
            return false;
        }
        return true;
    }

    public String getOrderState(String goodsId, String userId) {
        String key = "orderState:" + userId + ":" + goodsId;
        return template.opsForValue().get(key);
    }

    public boolean checkIsSelling(String goodsId) {
        String info = template.opsForValue().get(keyPrefix + ":id:" + goodsId);
        Map map = JsonUtils.fromJSON(info, Map.class);
        Map<String, Object> infoMap = (Map<String, Object>) map.get("infoMap");
        SimpleDateFormat sdf = new SimpleDateFormat("");
        try {
            long beginTime = sdf.parse(infoMap.get("begin_time") + "").getTime();
            long endTime = sdf.parse(infoMap.get("end_time") + "").getTime();
            return System.currentTimeMillis() >= beginTime && System.currentTimeMillis() <= endTime;
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }
}
