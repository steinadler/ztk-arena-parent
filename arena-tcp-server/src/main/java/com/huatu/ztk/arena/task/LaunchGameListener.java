package com.huatu.ztk.arena.task;

import com.google.common.collect.Lists;
import com.huatu.ztk.arena.bean.Player;
import com.huatu.ztk.arena.common.Actions;
import com.huatu.ztk.arena.common.RedisArenaKeys;
import com.huatu.ztk.arena.common.UserChannelCache;
import com.huatu.ztk.arena.netty.SuccessReponse;
import com.huatu.ztk.commons.JsonUtil;
import com.huatu.ztk.user.bean.UserDto;
import com.huatu.ztk.user.dubbo.UserDubboService;
import io.netty.channel.Channel;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import java.util.List;
import java.util.Map;

/**
 * Created by shaojieyue
 * Created time 2016-10-09 17:11
 */
public class LaunchGameListener implements MessageListener{
    private static final Logger logger = LoggerFactory.getLogger(LaunchGameListener.class);
    public static final String ATCION_FIELD = "action";

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    @Autowired
    private UserDubboService userDubboService;

    @Autowired
    private UserChannelCache userChannelCache;

    @Override
    public void onMessage(Message message) {
        Map data = null;
        try {
            final String text = new String(message.getBody());
            logger.info("receive message ={}",text);
            data = JsonUtil.toMap(text);
        }catch (Exception e){
            logger.error("ex",e);
            return;
        }
        int action = MapUtils.getInteger(data,ATCION_FIELD,-1);
        if (action == Actions.USER_JOIN_NEW_ARENA) {//新用户加入房间动作
            proccessNewUserJionArena(data);
        }else if (action == Actions.USER_LEAVE_GAME) {//用户离开房间
            proccessUserLeaveArena(data);
        }else if (action == Actions.SYSTEM_START_GAME) {//开始游戏动作
            proccessStartGame(data);
        }else if (action == Actions.SYSTEM_PRACTICE_STATUS_UPDATE) {
            proccessUserSubmitQuestion(data);
        }else {
            logger.error("unknow action={},data={}",action,data);
        }
    }

    /**
     * 答题记录更新,发送通知
     * @param data
     */
    private void proccessUserSubmitQuestion(Map data) {
        final long[] users = getRoomUsers(MapUtils.getLongValue(data, "arenaId"));
        for (long user : users) {//遍历,挨个发送通知
            final Channel channel = userChannelCache.getChannel(user);
            if (channel == null) {//不存在则不处理
                continue;
            }
            data.remove(ATCION_FIELD);//移除action属性
            //通知用户
            channel.writeAndFlush(SuccessReponse.userSubmitQuestion(data));
        }
    }

    /**
     * 处理该是游戏逻辑
     * @param data
     */
    private void proccessStartGame(Map data) {
        final long arenaId = MapUtils.getLong(data, "arenaId",-1L);
        final List<Object> uids = (List<Object>)MapUtils.getObject(data, "uids");
        final List<Long> practiceIds = (List<Long>)MapUtils.getObject(data, "practiceIds");
        //检查参数合法性
        if (CollectionUtils.isEmpty(uids) || CollectionUtils.isEmpty(practiceIds) || uids.size()!=practiceIds.size()) {
            logger.info("broke data,start game fail,data={}",data);
            return;
        }

        //遍历房间玩家,发送开始游戏通知
        for (int i = 0; i < uids.size(); i++) {
            //此处主要是jackson会优先把uid转为int
            long user = Long.valueOf(uids.get(i).toString());
            long practiceId = practiceIds.get(i);
            final Channel channel = userChannelCache.getChannel(user);
            logger.info("start send start game,userId={},arenaId={}",user,arenaId);
            if (channel == null) {//== null说明用户长连接不存在该服务上
                continue;
            }
            try {
                //发送游戏开始通知
                channel.writeAndFlush(SuccessReponse.startGame(practiceId,arenaId));
            }catch (Exception e){
                logger.error("ex",e);
            }
        }
    }

    /**
     * 处理用户离开房间业务逻辑
     * @param data
     */
    private void proccessUserLeaveArena(Map data) {
        final long arenaId = MapUtils.getLong(data, "arenaId",-1L);
        final long leaveUid = MapUtils.getLong(data, "uid",-1L);
        final long[] users = getRoomUsers(arenaId);
        for (long user : users) {//遍历为每个用户发送,用户离开房间通知
            final Channel channel = userChannelCache.getChannel(user);
            if (channel == null) {//== null说明用户长连接不存在该服务上
                continue;
            }
            try {
                //为其他玩家发送通知
                channel.writeAndFlush(SuccessReponse.userLeaveGame(leaveUid));
            }catch (Exception e){
                logger.error("ex",e);
            }
        }
    }

    /**
     * 处理有用户加入房间逻辑
     * @param data
     */
    private void proccessNewUserJionArena(Map data) {
        final long arenaId = MapUtils.getLong(data, "arenaId",-1L);
        //新玩家id
        final long newPlayerId = MapUtils.getLong(data, "uid",-1L);
        final long[] users = getRoomUsers(arenaId);
        List<Player> oldPlayers = Lists.newArrayList();
        Player newPlayer = null;
        for (long uid : users) {
            final UserDto userDto = userDubboService.findById(uid);
            final Player player = Player.builder()
                    .avatar(userDto.getAvatar())
                    .nick(userDto.getNick())
                    .uid(userDto.getId())
                    .build();
            if (uid == newPlayerId) {//新加入的玩家
                newPlayer = player;
            }else {
                oldPlayers.add(player);
            }
        }

        //新用户通知
        final Channel channel1 = userChannelCache.getChannel(newPlayerId);
        if (channel1 != null && CollectionUtils.isNotEmpty(oldPlayers)) {//房间之前存在玩家,则也给新玩家发送通知
            try {
                //新用户则发送已有的用户
                channel1.writeAndFlush(SuccessReponse.newJoinPalyer(oldPlayers));
            }catch (Exception e){
                logger.error("ex",e);
            }
        }

        //通知老用户
        for (Player player : oldPlayers) {//通知已经加入的用户,有新用户加入
            final Channel channel = userChannelCache.getChannel(player.getUid());
            if (channel == null) {//== null说明用户长连接不存在该服务上
                continue;
            }
            try {
                channel.writeAndFlush(SuccessReponse.newJoinPalyer(Lists.newArrayList(newPlayer)));
            }catch (Exception e){
                logger.error("ex",e);
            }
        }
    }

    /**
     * 查询竞技房间里的人员
     * @param arenaId
     * @return
     */
    private long[] getRoomUsers(long arenaId) {
        final SetOperations<String, String> setOperations = redisTemplate.opsForSet();
        final String roomUsersKey = RedisArenaKeys.getRoomUsersKey(arenaId);
        return setOperations.members(roomUsersKey).stream().mapToLong(uid-> Long.valueOf(uid)).toArray();
    }
}
