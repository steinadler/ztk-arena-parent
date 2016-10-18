package com.huatu.ztk.arena.bean;

import com.huatu.ztk.commons.ModuleConstants;
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
    public static class Module {
        private int id;
        private String name;
        private int status;//状态，模块是否开放(1：开放 2：未开放)
    }

    /**
     * 查询比赛规则
     * @return
     */
    public static final ArenaConfig getConfig() {
        // TODO: 开放的模块放在前边
        List modules = new ArrayList();
        Module m = Module.builder().id(-1).name("智能推送").status(1).build(); //默认开放
        Module m1 = Module.builder().id(392).name("常识判断").status(1).build(); //暂时开放
        Module m2 = Module.builder().id(435).name("言语理解").status(1).build(); //暂时开放
        Module m3 = Module.builder().id(482).name("数量关系").status(2).build(); //暂时关闭
        Module m4 = Module.builder().id(642).name("判断推理").status(2).build(); //暂时关闭
        Module m5 = Module.builder().id(754).name("资料分析").status(2).build(); //暂时关闭
        modules.add(m);
        modules.add(m1);
        modules.add(m2);
        modules.add(m3);
        modules.add(m4);
        modules.add(m5);
        //遍历公务员模块
//        ModuleConstants.GOWUYUAN_MODULES.forEach(module1->{
//            Module m= new Module();
//            m.setId(module1.getId());
//            m.setName(module1.getName());
//            m.setStatus(1);//全部开放
//            modules.add(m);
//        });
        return ArenaConfig.builder()
                .waitTime(60)
                .roomCapacity(2)
                .gameLimitTime(300)
                .questionCount(5)
                .modules(modules)
                .build();
    }
}
