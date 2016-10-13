package com.huatu.ztk.arena.netty;

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
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

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
        Response response = null;
        switch (request.getAction()) {
            case Actions.USER_JOIN_NEW_ARENA: {//用户加入竞技场
                final Integer moduleId = MapUtils.getInteger(request.getParams(), "moduleId");
                try {
                    response = proccessJoinNewArena(ctx, uid,moduleId);
                }catch (Exception e){
                    logger.error("ex",e);
                    response= ErrorResponse.LEAVE_GAME_FAIL;
                }
                break;
            }

            case Actions.USER_LEAVE_GAME: {//用户离开竞技场
                try {
                    response = proccessLeaveGame(ctx, uid);
                }catch (Exception e){
                    logger.error("ex",e);
                    ctx.writeAndFlush(ErrorResponse.LEAVE_GAME_FAIL);
                }
                break;
            }

            case Actions.USER_EXIST_ARENA:{//查询自己是否存在正在进行的竞技
                //查询用户正在进行的竞技场
                final ArenaRoom arenaRoom = getUserArenaRoom(uid);
                if (arenaRoom == null) {//不存在
                    response = SuccessReponse.noExistGame();
                }else {
                    response = SuccessReponse.existGame(arenaRoom);
                }
                break;
            }

            case Actions.SYSTEM_START_GAME:{//系统通知开始游戏
                final Long practiceId = MapUtils.getLong(request.getParams(), "practiceId");
                final Long arenaId = MapUtils.getLong(request.getParams(), "arenaId");
                response = SuccessReponse.startGame(practiceId,arenaId);
                break;
            }

            default:{//非法的请求
                response = ErrorResponse.UNKNOW_ACTION;
            }
        }

        if (StringUtils.isNoneBlank(request.getTicket())) {
            response.setTicket(request.getTicket());
        }

        ctx.writeAndFlush(response);

    }

    /**
     * 查询用户当前的竞技房间
     * @param uid 用户id
     * @return 没有则返回null
     */
    private ArenaRoom getUserArenaRoom(Long uid) {
        if (uid == null) {
            return null;
        }
        final String userRoomKey = RedisArenaKeys.getUserRoomKey(uid);
        final Long roomId = Long.valueOf(redisTemplate.opsForValue().get(userRoomKey));
        return areanDubboService.findById(roomId);
    }

    /**
     * 处理离开房间业务
     * @param ctx
     * @param uid
     */
    private Response proccessLeaveGame(ChannelHandlerContext ctx, Long uid) {
        final String userRoomKey = RedisArenaKeys.getUserRoomKey(uid);
        redisTemplate.delete(userRoomKey);//删除用户正在进行的游戏
        //删除用户等待列表
        redisTemplate.opsForSet().remove(RedisArenaKeys.getArenaUsersKey(-1),uid+"");
        return SuccessReponse.leaveGameSuccess();
    }

    /**
     * 处理添加房间业务
     * @param ctx
     * @param uid
     */
    private Response proccessJoinNewArena(ChannelHandlerContext ctx, Long uid, Integer moduleId) {
        if (moduleId == null) {
            return ErrorResponse.INVALID_PARAM;
        }

        final String userRoomKey = RedisArenaKeys.getUserRoomKey(uid);
        if (redisTemplate.hasKey(userRoomKey)) {//用户存在未完成的房间
            final Long roomId = Long.valueOf(redisTemplate.opsForValue().get(userRoomKey));
            final ArenaRoom arenaRoom = areanDubboService.findById(roomId);
            if (arenaRoom!=null && arenaRoom.getStatus() != ArenaRoomStatus.FINISHED) {//该房间未关闭,关闭的房间还是可以加入新房间
                final int index = arenaRoom.getPlayerIds().indexOf(uid);
                if (index > 0) {//该房间存在该用户
                    long practiceId = arenaRoom.getPractices().get(index);
                    if (practiceId > 0) {//练习存在
                        return SuccessReponse.existGame(arenaRoom);
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
        return SuccessReponse.joinGameSuccess();
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
