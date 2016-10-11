package com.huatu.ztk.arena.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 竞技结果
 * Created by shaojieyue
 * Created time 2016-07-06 15:26
 */
@Data
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Builder
public class ArenaResult {
    private long uid;//用户id
    private String nick;//用户昵称
    private int rcount;//做对数量
    private int elapsedTime;//耗时
}
