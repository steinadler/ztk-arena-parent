package com.huatu.ztk.arena.netty;

import com.huatu.ztk.commons.JsonUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by shaojieyue
 * Created time 2016-10-08 12:02
 */

@ChannelHandler.Sharable
public class RequestDecoder extends MessageToMessageDecoder<String> {
    private static final Logger logger = LoggerFactory.getLogger(RequestDecoder.class);

    /**
     * Decode from one message to an other. This method will be called for each written message that can be handled
     * by this encoder.
     *
     * @param ctx the {@link ChannelHandlerContext} which this {@link MessageToMessageDecoder} belongs to
     * @param msg the message to decode to an other one
     * @param out the {@link List} to which decoded messages should be added
     * @throws Exception is thrown if an error accour
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, String msg, List<Object> out) throws Exception {
        logger.info("receive message->{}",msg);
        final Request request = JsonUtil.toObject(msg, Request.class);
        out.add(request);
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
