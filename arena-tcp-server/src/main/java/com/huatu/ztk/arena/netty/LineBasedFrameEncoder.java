package com.huatu.ztk.arena.netty;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Created by shaojieyue
 * Created time 2016-10-13 09:03
 */

@ChannelHandler.Sharable
public class LineBasedFrameEncoder extends MessageToMessageEncoder<String> {
    private static final Logger logger = LoggerFactory.getLogger(LineBasedFrameEncoder.class);
    public static final String LINE_SPLIT = "\r\n";
    /**
     * Encode from one message to an other. This method will be called for each written message that can be handled
     * by this encoder.
     *
     * @param ctx the {@link ChannelHandlerContext} which this {@link MessageToMessageEncoder} belongs to
     * @param msg the message to encode to an other one
     * @param out the {@link List} into which the encoded msg should be added
     *            needs to do some kind of aggragation
     * @throws Exception is thrown if an error accour
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, String msg, List<Object> out) throws Exception {
        //如果msg==null,则默认为空字符串
        out.add(Optional.of(msg).orElse("")+LINE_SPLIT);
    }
}
