package com.huatu.ztk.arena.bean;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

import java.io.Serializable;

/**
 * 用户竞技记录
 * Created by shaojieyue
 * Created time 2016-07-05 09:00
 */

@Data
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Builder
public class UserArenaRecord implements Serializable{
    private static final long serialVersionUID = 1L;
    @Id//用户id作为id
    private long uid;//用户id
    @Transient
    private Player player;//玩家详情,不进行mongo存储
    private int winCount;//胜场次数
    private int rank;//排行 从1开始
}
