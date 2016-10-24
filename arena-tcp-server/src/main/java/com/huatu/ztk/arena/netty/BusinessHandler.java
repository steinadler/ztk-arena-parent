package com.huatu.ztk.arena.netty;

import com.google.common.collect.Maps;
import com.huatu.ztk.arena.bean.ArenaRoom;
import com.huatu.ztk.arena.bean.ArenaRoomStatus;
import com.huatu.ztk.arena.common.Actions;
import com.huatu.ztk.arena.common.RedisArenaKeys;
import com.huatu.ztk.arena.common.UserChannelCache;
import com.huatu.ztk.arena.dubbo.ArenaDubboService;
import com.huatu.ztk.arena.util.ApplicationContextProvider;
import com.huatu.ztk.commons.JsonUtil;
import com.huatu.ztk.commons.ModuleConstants;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by shaojieyue
 * Created time 2016-09-30 17:53
 */
public class BusinessHandler extends SimpleChannelInboundHandler<Request> {
    private static final Logger logger = LoggerFactory.getLogger(BusinessHandler.class);
    public static final AttributeKey<Long> uidAttributeKey = AttributeKey.valueOf("uid");
    private ArenaDubboService arenaDubboService = ApplicationContextProvider.getApplicationContext().getBean(ArenaDubboService.class);
    private RedisTemplate<String,String> redisTemplate = ApplicationContextProvider.getApplicationContext().getBean("redisTemplate",RedisTemplate.class);
    private RabbitTemplate rabbitTemplate = ApplicationContextProvider.getApplicationContext().getBean("rabbitTemplate",RabbitTemplate.class);

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
        final Long uid = ctx.channel().attr(uidAttributeKey).get();
        Response response = null;
        switch (request.getAction()) {
            case Actions.USER_JOIN_NEW_ARENA: {//用户加入竞技场
                final Integer moduleId = MapUtils.getInteger(request.getParams(), "moduleId");
                try {
                    response = proccessJoinNewArena(ctx, uid,moduleId);
                }catch (Exception e){
                    logger.error("ex",e);
                    response= ErrorResponse.JOIN_GAME_FAIL;
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
                    response = SuccessReponse.existGame(arenaRoom,uid);
                }
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
        //用户存在的房间id
        final String arenaId = redisTemplate.opsForValue().get(userRoomKey);

        if (StringUtils.isBlank(arenaId)) {//为空说明用户目前没有加入房间
            return null;
        }
        return arenaDubboService.findById(Long.valueOf(arenaId));
    }

    /**
     * 处理离开房间业务
     * @param ctx
     * @param uid
     */
    private Response proccessLeaveGame(ChannelHandlerContext ctx, Long uid) {
        final String userRoomKey = RedisArenaKeys.getUserRoomKey(uid);
        final String arenaIdStr = redisTemplate.opsForValue().get(userRoomKey);
        final SetOperations<String, String> setOperations = redisTemplate.opsForSet();
        if (StringUtils.isNoneBlank(arenaIdStr)) {//说明用户已经加入房间
            final Long arenaId = Long.valueOf(arenaIdStr);
            final String roomUsersKey = RedisArenaKeys.getRoomUsersKey(arenaId);
            setOperations.remove(roomUsersKey,uid.toString());//从房间中该删除该用户
            Map data = Maps.newHashMap();
            data.put("arenaId",arenaId);
            data.put("uid",uid);
            data.put("action",Actions.USER_LEAVE_GAME);
            //发送用户离开房间通知
            rabbitTemplate.convertAndSend("game_notify_exchange","",data);
        }else {//没有则说明用户还处于等待池中
            //此处遍历是可以的,正常来说,用户加入游戏就会存在于房间中,所以很小几率在等待池,
            //也就是说,这段代码应该不会运行
            //删除所有模块的
            for (Integer moduleId : ModuleConstants.GOWUYUAN_MODULE_IDS) {
                setOperations.remove(RedisArenaKeys.getArenaUsersKey(moduleId),uid+"");
            }
            //删除智能推送的
            setOperations.remove(RedisArenaKeys.getArenaUsersKey(-1),uid+"");
        }
        redisTemplate.delete(userRoomKey);//删除用户正在进行的房间标示
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
        final String arenaId = redisTemplate.opsForValue().get(userRoomKey);
        if (StringUtils.isNoneBlank(arenaId)) {//用户存在未完成的房间
            final Long roomId = Long.valueOf(arenaId);
            final ArenaRoom arenaRoom = arenaDubboService.findById(roomId);
            if (arenaRoom != null && arenaRoom.getStatus() != ArenaRoomStatus.FINISHED) {//该房间未关闭,关闭的房间还是可以加入新房间
                return SuccessReponse.existGame(arenaRoom,uid);
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
        cause.printStackTrace();
        ctx.writeAndFlush(ErrorResponse.INVALID_PARAM);
    }
}
