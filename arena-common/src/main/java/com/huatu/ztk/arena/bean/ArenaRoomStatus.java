package com.huatu.ztk.arena.bean;

/**
 * 竞技场状态
 * Created by shaojieyue
 * Created time 2016-07-04 21:37
 */
public class ArenaRoomStatus {
    /**
     * 已创建
     */
    public static final int CREATED =1;
    /**
     * 进行中,表示竞技场里面的用户正在做题
     */
    public static final int RUNNING =2;

    /**
     * 已结束
     */
    public static final int FINISHED =3;
}
