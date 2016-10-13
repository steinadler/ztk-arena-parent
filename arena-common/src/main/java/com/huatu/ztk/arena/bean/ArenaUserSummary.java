package com.huatu.ztk.arena.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户竞技场统计
 * Created by shaojieyue
 * Created time 2016-10-13 21:27
 */

@Data
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Builder
public class ArenaUserSummary {
    private long uid;//用户id
    private int winCount;//胜利场次
    private int failCount;//失败场次
}
