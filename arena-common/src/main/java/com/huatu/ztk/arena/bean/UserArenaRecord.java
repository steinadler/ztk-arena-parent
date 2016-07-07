package com.huatu.ztk.arena.bean;


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * 用户竞技记录
 * Created by shaojieyue
 * Created time 2016-07-05 09:00
 */

@Data
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Builder
@Document(collection = "ztk_user_arena_record")
public class UserArenaRecord {
    @Id//用户id作为id
    private long uid;//用户id
    private String nick;//昵称
    private int arenaCount;//竞技次数
    private int winCount;//胜场次数
    private double avgScore;//竞技平均分
    @Getter(onMethod = @__({ @JsonIgnore }))
    private List<Long> arenas;
}
