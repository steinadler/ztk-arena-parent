package com.huatu.ztk.arena.common;

import com.huatu.ztk.commons.spring.ErrorResult;

/**
 * Created by shaojieyue
 * Created time 2016-07-05 15:11
 */
public class ArenaErrors {

    /**
     * 用户已经存在房间内
     */
    public static final ErrorResult USER_IN_ROOM = ErrorResult.create(1610001,"用户已经存在房间内");


    /**
     * 退出房间错误,房间号不匹配
     */
    public static final ErrorResult USER_QUIT_ROOM_NOT_MATCH = ErrorResult.create(1610002,"退出房间错误,房间号不匹配");

    /**
     * 房间不存在
     */
    public static final ErrorResult ROOM_NOT_EXIST = ErrorResult.create(1610003,"房间不存在");


    /**
     * 正在进行和已结束的,不允许退出
     */
    public static final ErrorResult FINISHED_ONGOING_CAN_NOT_QUIT = ErrorResult.create(1610004,"正进行的或已结束的不允许退出");

    /**
     *正在进行的和已经结束的不允许加入
     */
    public static final ErrorResult FINISHED_ONGOING_CAN_NOT_JOIN = ErrorResult.create(1610004,"正进行的或已结束的不允许加入");

    /**
     * 房间人员已满,加入失败
     * 没有空闲的座位
     */
    public static final ErrorResult ROOM_NO_FREE_SEAT = ErrorResult.create(1610005,"房间人员已满,加入失败");

    /**
     * 进入房间失败,请重试
     */
    public static final ErrorResult JOIN_ROOM_FAIL = ErrorResult.create(1610006,"进入房间失败,请重试");

    /**
     * 没有足够的人数
     */
    public static final ErrorResult NOT_ENOUGH_PLAYER = ErrorResult.create(1610007,"没有足够的玩家");

    /**
     * 房间状态异常
     */
    public static final ErrorResult ROOM_STATUS_EXCEPTION = ErrorResult.create(1610007,"房间状态异常");






}
