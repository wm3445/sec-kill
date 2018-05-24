package com.hxjb.secondkill.controller;


import com.hxjb.secondkill.service.SecondKillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value = "/v1.0")
public class SecondKillController {

    @Autowired
    SecondKillService secondKillService;

    @RequestMapping(value = "/secondKill", method = RequestMethod.GET)
    public Mono<String> secondKill(
            String orderNum,
            String goodsId,
            String userId,
            @RequestParam(defaultValue = "1") Integer limit
    ) {
        if (!secondKillService.checkIsSelling(goodsId)){
            return Mono.empty();
        }
        if (null != orderNum && "1".equals(secondKillService.getOrderState(orderNum,userId))) {
            return Mono.empty();
        }
        if (!secondKillService.checkBuyLimit(goodsId,userId,limit)) {
            return Mono.justOrEmpty("您已经超过购买限制了");
        }
        return Mono.create(item -> item.success(secondKillService.makeOrder(goodsId, userId)));
    }


}
