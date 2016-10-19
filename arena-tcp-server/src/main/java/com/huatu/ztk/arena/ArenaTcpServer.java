package com.huatu.ztk.arena;

import com.huatu.ztk.arena.netty.*;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

/**
 * 竞技场tcp实时通讯服务
 * Created by shaojieyue
 * Created time 2016-09-30 16:28
 */
public class ArenaTcpServer {
    private static final int MAX_FRAME_LENGTH = 1024 * 1024;
    private static final Logger logger = LoggerFactory.getLogger(ArenaTcpServer.class);
    private String host;
    private int port;

    public ArenaTcpServer(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static void main(String[] args) throws Exception {
        int port = Integer.valueOf(args[0]);
        String host = System.getProperty("server_ip");
        ApplicationContext factory = new FileSystemXmlApplicationContext(
                new String[] { "classpath:/applicationContext.xml" });
        new ArenaTcpServer(host,port).run();
    }

    public void run() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(); // (1)
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap(); // (2)
            final StringDecoder stringDecoder = new StringDecoder();
            final RequestDecoder requestDecoder = new RequestDecoder();
            final ReponseEncoder reponseEncoder = new ReponseEncoder();
            final StringEncoder stringEncoder = new StringEncoder();
            final LineBasedFrameEncoder lineBasedFrameEncoder = new LineBasedFrameEncoder();
            final LoggingHandler loggingHandler = new LoggingHandler(LogLevel.DEBUG);
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class) // (3)
                    .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(loggingHandler);
                            ch.pipeline().addLast(new LineBasedFrameDecoder(MAX_FRAME_LENGTH));
                            ch.pipeline().addLast(stringEncoder);
                            ch.pipeline().addLast(lineBasedFrameEncoder);
                            ch.pipeline().addLast(reponseEncoder);
                            ch.pipeline().addLast(new IdleStateHandler(0,0,5));
                            ch.pipeline().addLast(stringDecoder);
                            ch.pipeline().addLast(new HeartbeatHandler());
                            ch.pipeline().addLast(requestDecoder);
                            ch.pipeline().addLast(new ServerHandshakeHandler());
                            ch.pipeline().addLast(new BusinessHandler());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)          // (5)
                    .childOption(ChannelOption.SO_KEEPALIVE, true) // (6)
                    .childOption(ChannelOption.TCP_NODELAY, true);

            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(host,port).sync(); // (7)
            logger.info("tcp server start at tcp://{}:{}",host,port);
            // TODO: 9/30/16 register server
            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            // TODO: 9/30/16 exit server
        }
    }
}
