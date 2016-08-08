package com.huatu.ztk.arena.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by linkang on 8/8/16.
 */
@RestController
public class ServerCheckController {

    /**
     * 空接口，检测服务器状态
     */
    @RequestMapping(value = "checkServer")
    public void check() {

    }
}

