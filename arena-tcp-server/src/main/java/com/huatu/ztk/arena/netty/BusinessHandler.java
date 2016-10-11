package com.huatu.ztk.arena.netty;

import com.google.common.collect.Maps;
import com.huatu.ztk.arena.bean.ArenaRoom;
import com.huatu.ztk.arena.bean.ArenaRoomStatus;
import com.huatu.ztk.arena.common.Actions;
import com.huatu.ztk.arena.common.RedisArenaKeys;
import com.huatu.ztk.arena.dubbo.AreanDubboService;
import com.huatu.ztk.arena.util.ApplicationContextProvider;
import com.huatu.ztk.commons.JsonUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.HashMap;

/**
 * Created by shaojieyue
 * Created time 2016-09-30 17:53
 */
public class BusinessHandler extends SimpleChannelInboundHandler<Request> {
    private static final Logger logger = LoggerFactory.getLogger(BusinessHandler.class);
    public static final AttributeKey<Long> uidAttributeKey = AttributeKey.valueOf("uid");
    private AreanDubboService areanDubboService = ApplicationContextProvider.getApplicationContext().getBean(AreanDubboService.class);
    private RedisTemplate<String,String> redisTemplate = ApplicationContextProvider.getApplicationContext().getBean("redisTemplate",RedisTemplate.class);

    /**
     * <strong>Please keep in mind that this method will be renamed to
     * {@code messageReceived(ChannelHandlerContext, I)} in 5.0.</strong>
     * <p>
     * Is called for each message of type {@link }.
     *
     * @param ctx the {@link ChannelHandlerContext} which this {@link SimpleChannelInboundHandler}
     *            belongs to
     * @param request the message to handle
     * @throws Exception is thrown if an error occurred
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Request request) throws Exception {
        logger.info("receive request:{}", JsonUtil.toJson(request));
        final Long uid = ctx.channel().attr(uidAttributeKey).get();
        switch (request.getAction()) {
            case Actions.JOIN_NEW_ARENA: {
                try {
                    if (!StringUtils.isNumeric(request.getParams().get("moduleId"))) {
                        ctx.writeAndFlush(ErrorResponse.INVALID_PARAM);
                        return;
                    }
                    proccessJoinNewArena(ctx, uid,Integer.valueOf(request.getParams().get("moduleId")));
                }catch (Exception e){
                    logger.error("ex",e);
                    ctx.writeAndFlush(ErrorResponse.JOIN_GAME_FAIL);
                }
                break;
            }

            case Actions.LEAVE_GAME: {
                try {
                    proccessLeaveGame(ctx, uid);
                }catch (Exception e){
                    logger.error("ex",e);
                    ctx.writeAndFlush(ErrorResponse.LEAVE_GAME_FAIL);
                }
                break;
            }

            default:{//非法的请求
                ctx.writeAndFlush(ErrorResponse.UNKNOW_ACTION);
            }
        }
    }

    /**
     * 处理离开房间业务
     * @param ctx
     * @param uid
     */
    private void proccessLeaveGame(ChannelHandlerContext ctx, Long uid) {
        final String userRoomKey = RedisArenaKeys.getUserRoomKey(uid);
        redisTemplate.delete(userRoomKey);//删除用户正在进行的游戏
        //删除用户等待列表
        redisTemplate.opsForSet().remove(RedisArenaKeys.getArenaUsersKey(-1),uid+"");
        ctx.writeAndFlush(SuccessReponse.leaveGameSuccess());
    }

    /**
     * 处理添加房间业务
     * @param ctx
     * @param uid
     */
    private void proccessJoinNewArena(ChannelHandlerContext ctx, Long uid,int moduleId) {
        final String userRoomKey = RedisArenaKeys.getUserRoomKey(uid);
        if (redisTemplate.hasKey(userRoomKey)) {//用户存在未完成的房间
            final Long roomId = Long.valueOf(redisTemplate.opsForValue().get(userRoomKey));
            final ArenaRoom arenaRoom = areanDubboService.findById(roomId);
            if (arenaRoom!=null && arenaRoom.getStatus() != ArenaRoomStatus.FINISHED) {//该房间未关闭,关闭的房间还是可以加入新房间
                final HashMap<Object, Object> data = Maps.newHashMap();
                final int index = arenaRoom.getPlayerIds().indexOf(uid);
                if (index > 0) {//该房间存在该用户
                    long practiceId = arenaRoom.getPractices().get(index);
                    if (practiceId > 0) {//练习存在
                        data.put("practiceId",practiceId);
                        data.put("roomId",arenaRoom.getId());
                        ctx.writeAndFlush(SuccessReponse.existGame(data));
                        return;
                    }else {
                        logger.error("roomId={} exist error practice id",arenaRoom.getId());
                    }
                }else {
                    logger.error("userId={} not in roomId={}",uid,roomId);
                }
            }
        }
        //用户加入游戏等待
        redisTemplate.opsForSet().add(RedisArenaKeys.getArenaUsersKey(moduleId),uid+"");
        ctx.writeAndFlush(SuccessReponse.joinGameSuccess());
    }

    /**
     * Calls {@link ChannelHandlerContext#fireExceptionCaught(Throwable)} to forward
     * <p>
     * Sub-classes may override this method to change behavior.
     *
     * @param ctx
     * @param cause
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.writeAndFlush(ErrorResponse.INVALID_PARAM);
    }
}
