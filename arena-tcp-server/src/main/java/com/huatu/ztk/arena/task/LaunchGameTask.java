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
import java.util.Set;

/**
 * Created by shaojieyue
 * Created time 2016-10-09 17:11
 */
public class LaunchGameTask implements MessageListener{
    private static final Logger logger = LoggerFactory.getLogger(LaunchGameTask.class);

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    @Autowired
    private UserDubboService userDubboService;

    @Override
    public void onMessage(Message message) {
        Map data = null;
        try {
            final String text = new String(message.getBody());
            logger.info("receive 11111111 ={}",text);
            data = JsonUtil.toMap(text);
        }catch (Exception e){
            logger.error("ex",e);
            return;
        }
        int action = MapUtils.getInteger(data,"action",-1);
        if (action == Actions.JOIN_NEW_ARENA) {//新用户加入房间动作
            final long roomId = MapUtils.getLong(data, "roomId",-1L);
            final long newPlayerId = MapUtils.getLong(data, "uid",-1L);
            final SetOperations<String, String> setOperations = redisTemplate.opsForSet();
            final String roomUsersKey = RedisArenaKeys.getRoomUsersKey(roomId);
            final long[] users = setOperations.members(roomUsersKey).stream().mapToLong(uid->Long.valueOf(uid)).toArray();
            List<Player> players = Lists.newArrayList();
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
                    players.add(player);
                }
            }

            //新用户通知
            final Channel channel1 = UserChannelCache.getChannel(newPlayerId);
            if (channel1 != null) {
                try {
                    channel1.writeAndFlush(SuccessReponse.newJoinPalyer(players));
                }catch (Exception e){
                    logger.error("ex",e);
                }
            }

            //通知老用户
            for (long uid : users) {//通知已经加入的用户,有新用户加入
                final Channel channel = UserChannelCache.getChannel(uid);
                if (channel == null) {//== null说明用户长连接不存在该服务上
                    continue;
                }
                try {
                    channel.writeAndFlush(SuccessReponse.newJoinPalyer(Lists.newArrayList(newPlayer)));
                }catch (Exception e){
                    logger.error("ex",e);
                }
            }

        }else if (action == Actions.LEAVE_GAME) {//用户离开房间

        }else if (action == Actions.START_GAME) {//开始游戏动作

        }

    }
}
