package com.huatu.ztk.arena.common;

import com.google.common.cache.Cache;
import io.netty.channel.Channel;

import java.util.concurrent.TimeUnit;

import static com.google.common.cache.CacheBuilder.newBuilder;

/**
 * Created by shaojieyue
 * Created time 2016-10-08 21:17
 */
public class UserChannelCache {
    private static final Cache<Long, Channel> USER_CHANNEL_CACHE =
            newBuilder()
                    .expireAfterAccess(1, TimeUnit.HOURS)//缓存时间
                    .maximumSize(1000)//最大1000
                    .build();

    public static final Channel getChannel(long uid){
        final Channel channel = USER_CHANNEL_CACHE.getIfPresent(uid);
        return channel;
    }

    public static final Channel putChannel(long uid,Channel channel){
        Channel old = UserChannelCache.getChannel(uid);
        USER_CHANNEL_CACHE.put(uid,channel);
        return old;
    }
}
