package com.huatu.ztk.arena.common
        ;

/**
 * 竞技场 redis key
 * Created by shaojieyue
 * Created time 2016-07-05 10:29
 */
public class RedisArenaKeys {

    /**
     * 用户参加的房间key
     * value:用户进入的房间
     * @param uid
     * @return
     */
    public static final String getUserRoomKey(long uid){
        final StringBuilder userRoomKey = new StringBuilder("user_room_").append(uid);
        return userRoomKey.toString();
    }

    /**
     * 房间id生成存储key
     * @return
     */
    public static final String getRoomIdKey(){
        return "room_id_key";
    }

    /**
     * 用户日排名key
     * @return
     * @param date 排名日期
     */
    public static final String getArenaDayRankKey(String date){
        return "arena_rank_"+date;
    }

    /**
     * 未开始游戏用户
     * @param moduleId
     * @return
     */
    public static final String getArenaUsersKey(int moduleId) {
        return "arena_users_" + moduleId;
    }

    /**
     * 房间用户列表
     * @param arenaId
     * @return
     */
    public static final String getRoomUsersKey(long arenaId){
        return "room_users_"+arenaId;
    }

    /**
     * 队列任务处理lock key
     * @param module
     * @return
     */
    public static final String getWorkLockKey(int module){
        return "arena_module_lock_"+module;
    }

    /**
     * 定时任务锁
     * @return
     */
    public static final String getScheduledLockKey(){
        return "arena_scheduled_lock";
    }

    /**
     * 获取机器人列表key
     * @return
     */
    public static final String getRobotsKey(){
        return "robots";
    }
}
