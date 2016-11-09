package com.huatu.ztk.arena.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created by shaojieyue
 * Created time 2016-09-22 16:15
 */

@Controller
@RequestMapping(value = "/system/")
public class SystemController {
    private static final Logger logger = LoggerFactory.getLogger(SystemController.class);

    /**
     * 健康检查接口
     */
    @RequestMapping(value = "health", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public void health(){

    }
}
