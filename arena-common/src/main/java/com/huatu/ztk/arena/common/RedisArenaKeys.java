package com.huatu.ztk.arena.common
        ;

/**
 * 竞技场 redis key
 * Created by shaojieyue
 * Created time 2016-07-05 10:29
 */
public class RedisArenaKeys {
    /**
     * 在线人数
     */
    public static final String ARENA_ONLINE_COUNT  =  "arena_online_count";


    /**
     * 进行中的房间列表
     */
    public static final String ONGOING_ROOM_LIST = "ongoing_room_list";

    /**
     * 获取房间空余人数的key
     * @return
     */
    public static final String getRoomFreePlayersKey(){
        return "room_free_player";
    }

    /**
     * 在线人数 key
     * @return
     */
    public static final String getArenaOnlineCount(){
        return ARENA_ONLINE_COUNT;
    }

    /**
     * 进行中的房间列表 key
     * @return
     */
    public static final String getOngoingRoomList(){
        return ONGOING_ROOM_LIST;
    }


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


}
