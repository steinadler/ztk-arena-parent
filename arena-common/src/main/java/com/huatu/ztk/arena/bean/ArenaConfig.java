package com.huatu.ztk.arena.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

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
    private List<Module> modules;//竞技模块
    @Data
    @Builder
    class Module{
        private int id;
        private String name;

    }
    /**
     * 查询比赛规则
     * @return
     */
    public static final ArenaConfig getConfig(){
        final Module module = Module.builder().id(-1).name("智能推送").build();
        List modules = new ArrayList();
        modules.add(module);
        return ArenaConfig.builder()
                .waitTime(10)
                .roomCapacity(4)
                .gameLimitTime(300)
                .questionCount(5)
                .modules(modules)
                .build();
    }
}
