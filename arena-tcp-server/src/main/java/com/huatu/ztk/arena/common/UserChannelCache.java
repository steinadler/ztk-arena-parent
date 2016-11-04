package com.huatu.ztk.arena.common;

import com.codahale.metrics.annotation.CachedGauge;
import com.google.common.cache.Cache;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

import static com.google.common.cache.CacheBuilder.newBuilder;

/**
 * Created by shaojieyue
 * Created time 2016-10-08 21:17
 */

@Component
public class UserChannelCache {
    private static final Logger logger = LoggerFactory.getLogger(UserChannelCache.class);
    private final Cache<Long, Channel> USER_CHANNEL_CACHE =
            newBuilder()
                    .expireAfterAccess(2,TimeUnit.HOURS)
                    .removalListener(new RemovalListener<Long,Channel>(){
                        @Override
                        public void onRemoval(RemovalNotification<Long, Channel> notification) {
                            final RemovalCause removalCause = notification.getCause();
                            final Channel channel = notification.getValue();
                            if (channel == null) {
                                return;
                            }
                            if (!channel.isActive()) {//连接不可用,直接关闭即可
                                channel.close();
                                return;
                            }

                            if (removalCause == RemovalCause.REPLACED) {//值被替换
                                channel.close();//如果被替换,说明存在新的连接,把此连接关闭即可
                            }else if(removalCause == RemovalCause.COLLECTED){//引用被回收
                                channel.close();//直接关闭连接
                            }else if (removalCause == RemovalCause.EXPLICIT) {//用户手动移除缓存
                                channel.close();//直接关闭连接
                            }else {
                                USER_CHANNEL_CACHE.put(notification.getKey(),channel);
                            }
                        }
                    })
                    .build();

    public final Channel getChannel(long uid){
        final Channel channel = USER_CHANNEL_CACHE.getIfPresent(uid);
        return channel;
    }



    public synchronized Channel putChannel(long uid,Channel channel){
        Channel old = getChannel(uid);
        USER_CHANNEL_CACHE.put(uid,channel);
        return old;
    }

    @CachedGauge(name = "arenaActive",timeout = 1,timeoutUnit = TimeUnit.MINUTES)
    public long channelSize(){
        return USER_CHANNEL_CACHE.size();
    }

    /**
     * 移除channel
     * 注意的是,只是当传入的和cache里的channel为同一个时
     * 才把channel移除,防止并发下出错
     * @param uid
     * @param channel
     */
    public synchronized final void remove(long uid,Channel channel){
        final Channel cachedChannel = USER_CHANNEL_CACHE.getIfPresent(uid);
        if (cachedChannel == channel) {//要移除的是当前连接
            //从缓存移除连接
            USER_CHANNEL_CACHE.invalidate(uid);
        }
    }

}
