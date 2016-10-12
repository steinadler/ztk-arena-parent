package com.huatu.ztk.arena.netty;

import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by shaojieyue
 * Created time 2016-10-08 11:57
 */

@Data
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class Request {
    private String ticket;//请求和响应的标示,以此来让请求和响应对应起来
    private int action;
    private Map<String,String> params;
}
