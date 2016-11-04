package com.huatu.ztk.arena.netty;

import com.huatu.ztk.commons.JsonUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by shaojieyue
 * Created time 2016-10-08 13:48
 */

public class ReponseEncoder extends MessageToMessageEncoder<Response> {
    public static final AttributeKey<Long> uidAttributeKey = AttributeKey.valueOf("uid");
    private static final Logger logger = LoggerFactory.getLogger(ReponseEncoder.class);
    /**
     * Encode from one message to an other. This method will be called for each written message that can be handled
     * by this encoder.
     *
     * @param ctx the {@link ChannelHandlerContext} which this {@link MessageToMessageEncoder} belongs to
     * @param response the message to encode to an other one
     * @param out the {@link List} into which the encoded response should be added
     *            needs to do some kind of aggragation
     * @throws Exception is thrown if an error accour
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, Response response, List<Object> out) throws Exception {
        final Long uid = ctx.channel().attr(uidAttributeKey).get();
        final String message = JsonUtil.toJson(response);
        logger.info("write to uid={} message={}",uid,message);
        out.add(message);
    }
}
