package com.huatu.ztk.arena.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.io.Serializable;

/**
 * 房间核心属性
 * Created by shaojieyue
 * Created time 2016-10-11 17:39
 */
@Data
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class ArenaRoomSimple implements Serializable{
    private static final long serialVersionUID = 1L;
    @Id
    long id;//房间号
    private int type;//房间类型
    private int moduleId;//模块id
    private String module;//考试模块
    private String name;//房间名称
    private long winner;//胜者id
    private int status;//房间状态
    public long createTime;//创建时间
}
