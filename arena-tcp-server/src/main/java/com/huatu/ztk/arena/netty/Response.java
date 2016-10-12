package com.huatu.ztk.arena.netty;

import lombok.*;

/**
 * Created by shaojieyue
 * Created time 2016-10-08 13:39
 */

public abstract class Response {
    @Getter @Setter
    private String ticket;//请求和响应的标示,以此来让请求和响应对应起来
    @Getter @Setter
    protected int code;
    @Getter @Setter
    protected String message;
}
