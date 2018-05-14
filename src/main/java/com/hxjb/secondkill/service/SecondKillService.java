package com.hxjb.secondkill.service;

import com.hxjb.secondkill.mq.SecKillSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SecondKillService {

    private static Logger logger = LoggerFactory.getLogger(SecondKillService.class);


    @Autowired
    private StringRedisTemplate template;


    @Autowired
    SecKillSender secKillSender;


    public String buy(String userId) {
        return template.execute(new SessionCallback<String>() {
            @Override
            public <K, V> String execute(RedisOperations<K, V> redisOperations) throws DataAccessException {
                String result = "0";
                template.watch("orderCount");
                String orderCount = template.opsForValue().get("orderCount");
                Integer count = Integer.valueOf(orderCount);
                if (count > 0) {
                    if (template.opsForSet().isMember("orderInfo",userId)) {
                        logger.info("不能重复抢购");
                        result = "不能重复抢购";
                        return result;
                    }
                    redisOperations.multi();
                    template.opsForValue().increment("orderCount", -1);
                    List<Object> exec = redisOperations.exec();
                    if (exec == null || exec.size() == 0) {
                        // logger.info("并发冲突执行抢购失败逻辑--------");
                        buy(userId);
                    } else {
                        for (Object o : exec) {
                            logger.info("抢购成功，还剩--->" + o);
                            Map<String, Object> payload = new HashMap<>();
                            payload.put("userId", userId);
                            template.opsForSet().add("orderInfo", userId);
                            secKillSender.send(payload);
                            result = o + "";
                        }
                        // 当最会一件商品是清空购买记录set
                        if(count == 1){
                            template.delete("orderInfo");
                        }
                    }
                } else {
                    logger.info("卖完了");
                }
                return result;
            }
        });
    }
}
