package com.huatu.ztk.arena.bean;

import com.huatu.ztk.paper.bean.PracticePaper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
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
public class ArenaRoom {
    @Id
    private long id;//房间号
    private String name;//房间名称
    private String module;//考试模块
    private int time;//答题时间
    private int qcount;//题量
    private int maxPlayerCount;//最大的玩家人数
    private int status;//房间状态
    private List<Long> players;//参加人员列表
    private List<Long> practices;//参加人员对应的练习id
    private PracticePaper practicePaper;//房间对应的练习
    private long createTime;//创建时间
}
