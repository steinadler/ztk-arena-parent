package com.huatu.ztk.arena.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 用户竞技场统计
 * Created by shaojieyue
 * Created time 2016-10-13 21:27
 */

@Data
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Builder
@Document(collection = "ztk_arena_summary")
public class ArenaUserSummary {
    @Id
    @Getter(onMethod = @__({ @JsonIgnore}))
    private String id;//id结构 uid+yyyyMMdd
    private long uid;//用户id
    private int winCount;//胜利场次
    private int failCount;//失败场次
}
