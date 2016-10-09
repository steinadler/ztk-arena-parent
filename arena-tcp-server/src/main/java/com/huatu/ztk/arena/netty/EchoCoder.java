package com.huatu.ztk.arena.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by shaojieyue
 * Created time 2016-10-08 13:56
 */
public class EchoCoder extends MessageToMessageDecoder {
    private static final Logger logger = LoggerFactory.getLogger(EchoCoder.class);

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
    protected void decode(ChannelHandlerContext ctx, Object msg, List out) throws Exception {
        logger.info("rec->{}",msg);
        out.add(msg);
    }
}
