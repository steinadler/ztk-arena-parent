package com.huatu.ztk.arena.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 竞技场基础配置
 * Created by shaojieyue
 * Created time 2016-10-12 10:15
 */

@Data
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Builder
public class ArenaConfig {
    private int waitTime;//等待时间,单位:秒
    private int roomCapacity;//房间最大人数
    private int gameLimitTime;//比赛限时,单位:秒
    private int questionCount;//单场比赛试题个数

    /**
     * 查询比赛规则
     * @return
     */
    public static final ArenaConfig getConfig(){
        return ArenaConfig.builder()
                .waitTime(8)
                .roomCapacity(4)
                .gameLimitTime(16*60)
                .questionCount(20)
                .build();
    }
}
