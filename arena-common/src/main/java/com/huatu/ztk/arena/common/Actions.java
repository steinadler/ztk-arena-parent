package com.huatu.ztk.arena.common;

/**
 * 竞技场支持的客户端所有命令
 * 注意的是,USER_开头的是用户命令
 * SYSTEM_开头的是系统命令
 * Created by shaojieyue
 * Created time 2016-10-08 11:49
 */
public class Actions {

    /**
     * 身份验证
     */
    public static final int USER_AUTHENTICATION = 10001;

    /**
     * 加入竞技场
     */
    public static final int USER_JOIN_NEW_ARENA =10002;

    /**
     * 离开竞技场
     */
    public static final int USER_LEAVE_GAME =10003;

    /**
     * 查看用户是否存在未做完的竞技练习
     */
    public static final int USER_EXIST_ARENA = 10004;


    /**
     * 系统开始游戏
     */
    public static final int SYSTEM_START_GAME = 11001;

    /**
     * 练习状态发生变化
     */
    public static final int SYSTEM_PRACTICE_STATUS_UPDATE=11002;

    /**
     * 通知用户查看竞技结果
     */
    public static final int SYSTEM_VIEW_ARENA_RESULT=11003;
}
