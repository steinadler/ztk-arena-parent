package com.huatu.ztk.arena.netty;

import com.google.common.collect.Maps;
import com.huatu.ztk.arena.bean.ArenaRoom;
import com.huatu.ztk.arena.bean.Player;
import org.apache.commons.collections.map.HashedMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by shaojieyue
 * Created time 2016-10-08 14:51
 */
public class SuccessReponse extends Response{
    private Object data;

    /**
     * 登录成功
     * @return
     */
    public static final SuccessReponse loginSuccessResponse(){
        return new SuccessReponse(50000,"身份认证成功");
    }

    public static final SuccessReponse joinGameSuccess(){
        return new SuccessReponse(50001,"加入游戏成功");
    }

    public static final SuccessReponse leaveGameSuccess(){
        return new SuccessReponse(50002,"成功离开游戏");
    }

    /**
     * 存在未完成的游戏
     * @param arenaRoom
     * @return
     */
    public static final SuccessReponse existGame(ArenaRoom arenaRoom,long uid){
        Map data =  new HashedMap();
        final int index = arenaRoom.getPlayerIds().indexOf(uid);
        long practiceId =-1;
        if (index > 0) {//该房间存在该用户
            practiceId = arenaRoom.getPractices().get(index);
        }
        data.put("roomId",arenaRoom.getId());//房间号
        data.put("practiceId",practiceId);//练习id
        data.put("players",arenaRoom.getPlayers());//玩家列表
        data.put("status",arenaRoom.getStatus());
        return new SuccessReponse(50003,data);
    }

    /**
     * 不存在未完成的经济房间
     * @return
     */
    public static final SuccessReponse noExistGame(){
        return new SuccessReponse(50004,"不存在未完成的竞技房间");
    }

    /**
     * 新添加用户
     * @param players
     * @return
     */
    public static final SuccessReponse newJoinPalyer(List<Player> players){
        return new SuccessReponse(50005,players);
    }


    /**
     * 开始游戏通知
     * @param practiceId 用户练习id
     * @param arenaId 房间id
     * @return
     */
    public static final SuccessReponse startGame(long practiceId,long arenaId){
        final HashMap<Object, Object> data = Maps.newHashMap();
        data.put("practiceId",practiceId);
        data.put("arenaId",arenaId);
        return new SuccessReponse(50006,data);
    }


    /**
     * 有用户离开房间,要发送通知
     * @param uid
     * @return
     */
    public static final SuccessReponse userLeaveGame(long uid){
        final HashMap<Object, Object> data = Maps.newHashMap();
        data.put("uid",uid);
        return new SuccessReponse(50007,data);
    }

    /**
     * 用户提交答案
     * @param data
     * @return
     */
    public static final SuccessReponse userSubmitQuestion(Map data){
        return new SuccessReponse(50008,data);
    }

    /**
     * 查看竞技结果通知
     * @param arenaId
     * @return
     */
    public static final SuccessReponse arenaView(long arenaId){
        Map data = Maps.newHashMap();
        data.put("arenaId",arenaId);
        return new SuccessReponse(50009,data);
    }

    private SuccessReponse() {
    }

    private SuccessReponse(int code, Object data) {
        this.code = code;
        this.data = data;
    }

    private SuccessReponse(int code,String message) {
        this.code = code;
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
