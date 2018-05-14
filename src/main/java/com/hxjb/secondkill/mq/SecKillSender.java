package com.hxjb.secondkill.mq;


import com.hxjb.secondkill.util.JsonUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SecKillSender {

    private static final String queueName = "sec.kill";


    @Autowired
    private AmqpTemplate rabbitTemplate;

    public void send(Map<String,Object> payload) {
        String msg = "";
        try {
            msg = JsonUtils.toJSON(payload);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.rabbitTemplate.convertAndSend(queueName, msg);
    }
}
