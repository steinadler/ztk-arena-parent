package com.huatu.ztk.arena;

import com.huatu.ztk.commons.web.WebServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * 竞技场入口
 * Created by shaojieyue
 * Created time 2016-07-04 21:56
 */
public class ArenaWebServer {
    private static final Logger logger = LoggerFactory.getLogger(ArenaWebServer.class);

    public static void main(String[] args) throws Exception {
        final Integer port = Integer.valueOf(args[0]);
        String serverIP = System.getProperty("server_ip");
        WebServer webServer = new WebServer(serverIP,port,"/a");
        webServer.setMinThreads(20)
                .setMaxThreads(200)
                .start();
    }
}
