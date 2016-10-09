package com.huatu.ztk.arena.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 玩家实体
 * Created by shaojieyue
 * Created time 2016-10-09 17:33
 */
@Data
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Builder
public class Player {
    private long uid;//用户id
    private String nick;//昵称
    private String avatar;//头像地址
}
