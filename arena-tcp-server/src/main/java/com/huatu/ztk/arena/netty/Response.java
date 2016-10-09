package com.huatu.ztk.arena.netty;

import lombok.*;

/**
 * Created by shaojieyue
 * Created time 2016-10-08 13:39
 */

public abstract class Response {
    @Getter @Setter
    protected int code;
    @Getter @Setter
    protected String message;
}
