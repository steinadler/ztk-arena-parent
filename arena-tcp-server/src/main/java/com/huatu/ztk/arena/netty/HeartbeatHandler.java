package com.huatu.ztk.arena.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * 心跳事件处理
 * Created by shaojieyue
 * Created time 2016-09-30 17:18
 */
public class HeartbeatHandler extends ChannelInboundHandlerAdapter {
    private static final ByteBuf HEARTBEAT_SEQUENCE = Unpooled.unreleasableBuffer(
            Unpooled.copiedBuffer("HEARTBEAT", CharsetUtil.UTF_8));  //2

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
            ctx.writeAndFlush(HEARTBEAT_SEQUENCE.duplicate())
                    .addListener(ChannelFutureListener.CLOSE_ON_FAILURE);  //3
        } else {//事件不是一个 IdleStateEvent 的话，就将它传递给下一个处理程序
            super.userEventTriggered(ctx, evt);  //4
        }
    }
}
