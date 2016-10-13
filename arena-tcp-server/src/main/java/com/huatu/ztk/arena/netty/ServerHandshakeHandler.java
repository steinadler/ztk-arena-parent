package com.huatu.ztk.arena.netty;

import com.google.common.base.Strings;
import com.huatu.ztk.arena.bean.Player;
import com.huatu.ztk.arena.common.Actions;
import com.huatu.ztk.arena.common.UserChannelCache;
import com.huatu.ztk.arena.dubbo.ArenaPlayerDubboService;
import com.huatu.ztk.arena.util.ApplicationContextProvider;
import com.huatu.ztk.user.service.UserSessionService;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by shaojieyue
 * Created time 2016-10-08 16:58
 */
public class ServerHandshakeHandler extends SimpleChannelInboundHandler<Request> {
    private static final Logger logger = LoggerFactory.getLogger(ServerHandshakeHandler.class);
    public static final String TOKEN_KEY = "token";
    final ArenaPlayerDubboService playerDubboService = ApplicationContextProvider.getApplicationContext().getBean(ArenaPlayerDubboService.class);
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
        if (request.getAction() != Actions.USER_AUTHENTICATION) {//不是登录请求
            authenticationFail(ctx,request);
            return;
        }

        //请求参数有误
        if (request.getParams() == null || Strings.isNullOrEmpty(request.getParams().get(TOKEN_KEY).toString())) {
            authenticationFail(ctx,request);
            return;
        }

        final UserSessionService sessionService = ApplicationContextProvider.getApplicationContext().getBean(UserSessionService.class);
        final long uid = sessionService.getUid(request.getParams().get(TOKEN_KEY).toString());
        if (uid > 0) {//>0说明用户session处于有效状态
            final Player player = playerDubboService.findById(uid);
            //发回消息
            final SuccessReponse response = SuccessReponse.loginSuccessResponse();
            response.setData(player);
            ctx.writeAndFlush(wapperResponse(response,request));
            ctx.channel().attr(BusinessHandler.uidAttributeKey).set(uid);
            ctx.pipeline().remove(this);//认证成功后,移除该handler
            //把当前连接加入到cache,如果存在旧的连接,则返回旧连接
            final Channel oldChannel = UserChannelCache.putChannel(uid, ctx.channel());
            if (oldChannel != null) {//存在旧的连接
                oldChannel.close();//关闭旧连接
            }
        }else {//身份校验失败
            authenticationFail(ctx,request);
        }
    }

    private Response wapperResponse(Response response,Request request){
        if (StringUtils.isNoneBlank(request.getTicket())) {
            response.setTicket(request.getTicket());
        }
        return response;
    }

    private void authenticationFail(ChannelHandlerContext ctx,Request request) {
        //发送结果
        ctx.writeAndFlush(wapperResponse(ErrorResponse.AUTHENTICATION_FAIL,request));
        //关闭channel，断开连接
        ctx.channel().close();
    }
}
