package com.huatu.ztk.arena.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 相应错误
 * Created by shaojieyue
 * Created time 2016-10-08 14:43
 */
public class Error {

    public static final Error INVALID_PARAM = new Error(100001,"非法的参数");
    public static final Error UNKNOW_ACTION = new Error(100001,"未知的请求");

    public int code;
    private String message;

    private Error(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
