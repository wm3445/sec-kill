package com.hxjb.secondkill.controller;


import com.hxjb.secondkill.service.SecondKillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value = "/v1.0")
public class SecondKillController {

    @Autowired
    SecondKillService secondKillService;

    @RequestMapping(value = "/secondKill", method = RequestMethod.GET)
    public Mono<String> secondKill(String userId) {
        return Mono.create(item -> item.success(secondKillService.buy(userId)));
    }


}
