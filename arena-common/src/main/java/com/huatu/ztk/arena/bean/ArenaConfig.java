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
    public static class Module{
        private int id;
        private String name;

    }
    /**
     * 查询比赛规则
     * @return
     */
    public static final ArenaConfig getConfig(){
        final Module module = new Module();
        module.setId(-1);
        module.setName("智能推送");
        List modules = new ArrayList();
        modules.add(module);

        //遍历公务员模块
        ModuleConstants.GOWUYUAN_MODULES.forEach(module1->{
            Module m= new Module();
            m.setId(module1.getId());
            m.setName(module1.getName());
            modules.add(m);
        });
        return ArenaConfig.builder()
                .waitTime(60)
                .roomCapacity(2)
                .gameLimitTime(300)
                .questionCount(5)
                .modules(modules)
                .build();
    }
}
