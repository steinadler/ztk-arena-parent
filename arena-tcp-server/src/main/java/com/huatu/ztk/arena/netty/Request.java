package com.huatu.ztk.arena.netty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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
    private int action;
    private Map<String,String> params;
}
