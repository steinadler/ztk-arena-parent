package com.huatu.ztk.arena.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.huatu.ztk.paper.bean.PracticePaper;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * 竞技房间
 * Created by shaojieyue
 * Created time 2016-07-04 20:08
 */

@Data
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Builder
@Document(collection = "ztk_arena_room")
public class ArenaRoom extends ArenaRoomSimple{
    private int limitTime;//比赛限时
    private int qcount;//题量
    private List<Long> playerIds;//参加人员id列表
    @Transient
    private List<Player> players;//参加人员详情列表,该属性不存入mongo
    private List<Long> practices;//参加人员对应的练习id
    @Getter(onMethod = @__({ @JsonIgnore}))
    private PracticePaper practicePaper;//房间对应的练习
    private ArenaResult[] results;//竞技结果

}
