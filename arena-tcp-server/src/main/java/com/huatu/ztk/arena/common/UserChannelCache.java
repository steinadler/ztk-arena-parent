package com.huatu.ztk.arena.common;

import com.google.common.cache.Cache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
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
                    .expireAfterAccess(2,TimeUnit.HOURS)
                    .removalListener(new RemovalListener<Long,Channel>(){
                        @Override
                        public void onRemoval(RemovalNotification<Long, Channel> notification) {
                            final Channel channel = notification.getValue();
                            if (channel == null) {
                                return;
                            }
                            if (channel.isActive()) {//如果还是处于活跃状态,则把其再次加入缓存
                                USER_CHANNEL_CACHE.put(notification.getKey(),channel);
                            }
                        }
                    })
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
