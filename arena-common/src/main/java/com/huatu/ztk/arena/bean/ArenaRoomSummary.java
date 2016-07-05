package com.huatu.ztk.arena.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 竞技赛场统计
 * Created by shaojieyue
 * Created time 2016-07-04 21:53
 */

@Data
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Builder
public class ArenaRoomSummary {
    private long playerCount;//在线人数
    private long roomCount;//总的房间数
    private long goingCount;//进行中的房间数
    private long freeCount;//空闲中的房间数
}
