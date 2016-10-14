package com.huatu.ztk.arena.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;

/**
 *
 * 心跳事件处理
 * Created by shaojieyue
 * Created time 2016-09-30 17:18
 */
public class HeartbeatHandler extends SimpleChannelInboundHandler<String> {
    public static final String PING = "PING";//发送心跳内容
    private static final ByteBuf HEARTBEAT_PING = Unpooled.unreleasableBuffer(
            Unpooled.copiedBuffer(PING+"\r\n", CharsetUtil.UTF_8));  //2
    public static final String PONG = "PONG";//心跳响应
    private static final ByteBuf HEARTBEAT_PONG = Unpooled.unreleasableBuffer(
            Unpooled.copiedBuffer(PONG+"\r\n", CharsetUtil.UTF_8));  //2

    /**
     * <strong>Please keep in mind that this method will be renamed to
     * {@code messageReceived(ChannelHandlerContext, I)} in 5.0.</strong>
     * <p>
     *
     * @param ctx the {@link ChannelHandlerContext} which this {@link SimpleChannelInboundHandler}
     *            belongs to
     * @param msg the message to handle
     * @throws Exception is thrown if an error occurred
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        if (msg.equals(PONG)) {//心跳响应
            //如果是心跳做任何处理
            return;
        }else if (msg.equals(PING)) {//对方要求发送心跳
            ctx.writeAndFlush(HEARTBEAT_PONG);
        }else {
            ctx.fireChannelRead(msg);//传递到下一个handler
        }

    }

    /**
     * Calls {@link ChannelHandlerContext#fireUserEventTriggered(Object)} to forward
     * <p>
     * Sub-classes may override this method to change behavior.
     *
     * @param ctx
     * @param evt
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            //发送的心跳并添加一个侦听器，如果发送操作失败将关闭连接
            ctx.writeAndFlush(HEARTBEAT_PING.duplicate())
                    .addListener(ChannelFutureListener.CLOSE_ON_FAILURE);  //3
        } else {//事件不是一个 IdleStateEvent 的话，就将它传递给下一个处理程序
            super.userEventTriggered(ctx, evt);  //4
        }
    }
}
