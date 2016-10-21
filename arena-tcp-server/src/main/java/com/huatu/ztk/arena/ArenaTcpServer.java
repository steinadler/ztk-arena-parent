package com.huatu.ztk.arena;

import com.huatu.ztk.RegisterServer;
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
import io.netty.util.ResourceLeakDetector;
import io.netty.util.Timeout;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import java.util.concurrent.TimeUnit;

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
    private RegisterServer registerServer;
    public ArenaTcpServer(String host, int port, String etcdConnString) {
        this.host = host;
        this.port = port;
        registerServer = new RegisterServer(host,port,System.getProperty("server_name"),etcdConnString);
    }

    public static void main(String[] args) throws Exception {
        int port = Integer.valueOf(args[0]);
        String host = System.getProperty("server_ip");
        final String etcdConnString = "http://etcd01:2379,http://etcd02:2379,http://etcd03:2379";
        ApplicationContext factory = new FileSystemXmlApplicationContext(
                new String[] { "classpath:/applicationContext.xml" });

        new ArenaTcpServer(host,port,etcdConnString).run();
    }

    public void run() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(); // (1)
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        //用户做业务处理的线程组
        EventLoopGroup businessGroup = new NioEventLoopGroup(50);
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap(); // (2)
            final StringDecoder stringDecoder = new StringDecoder();
            final RequestDecoder requestDecoder = new RequestDecoder();
            final ReponseEncoder reponseEncoder = new ReponseEncoder();
            final StringEncoder stringEncoder = new StringEncoder();
            final LineBasedFrameEncoder lineBasedFrameEncoder = new LineBasedFrameEncoder();
            InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
            //注意LogLevel.INFO表示,netty会把接收和写出的消息打印为INFO级别,至于该消息能不能展示
            //要看我们的日志配置文件配置的级别
            final LoggingHandler loggingHandler = new LoggingHandler(LogLevel.INFO);
            serverBootstrap.group(bossGroup, workerGroup)
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
                            ch.pipeline().addLast(businessGroup,new BusinessHandler());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)          // (5)
                    .childOption(ChannelOption.SO_KEEPALIVE, true) // (6)
                    .childOption(ChannelOption.TCP_NODELAY, true);

            // Bind and start to accept incoming connections.
            ChannelFuture f = serverBootstrap.bind(host,port).addListener(new GenericFutureListener(){

                /**
                 * Invoked when the operation associated with the {@link Future} has been completed.
                 *
                 * @param future the source {@link Future} which called this callback
                 */
                @Override
                public void operationComplete(Future future) throws Exception {
                    if (future.isSuccess()) {
                        //服务启动后,注册到服务中心
                        registerServer.register();
                    }
                }
            }).sync(); // (7)
            logger.info("tcp server start at tcp://{}:{}",host,port);

            //添加hook,服务结束前,先下线服务
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    registerServer.unregister();
                }
            }));
            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
            f.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            businessGroup.shutdownGracefully();
        }
    }
}
