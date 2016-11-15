package com.huatu.ztk.arena.task;

import com.google.common.primitives.Longs;
import com.huatu.ztk.arena.bean.ArenaConfig;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 简单的分布式锁
 * 该锁只是简单实现,不适用于严格的场合
 * Created by shaojieyue
 * Created time 2016-11-15 20:07
 */

@Component
public class DistributedLock {

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    /**
     * 试着释放锁
     * 只有锁是属于自己时才会释放锁
     * @param lockKey
     */
    public void tryReleaseLock(String lockKey,long maxHoldTime){
        if (getLock(lockKey,maxHoldTime)) {
            redisTemplate.delete(lockKey);
        }
    }

    /**
     * 更新锁内容
     * @param lockKey
     */
    public void updateLock(String lockKey) {
        final String lockValue = getLockValue();
        redisTemplate.opsForValue().set(lockKey, lockValue);
    }

    /**
     * 重置并尝试获取锁
     * @param workLockKey
     * @return
     */
    public boolean resetAndGetLock(String workLockKey){
        redisTemplate.delete(workLockKey);
        return redisTemplate.opsForValue().setIfAbsent(workLockKey, getLockValue()).booleanValue();
    }


    /**
     * 获取任务锁
     * @return
     */
    public boolean getLock(String lockKey,long maxHoldTime) {
        //获取锁内容
        String value = redisTemplate.opsForValue().get(lockKey);
        if (StringUtils.isBlank(value)) {//如果为空,说明没人占用锁,则尝试占用锁
            redisTemplate.opsForValue().setIfAbsent(lockKey, getLockValue());
            value = redisTemplate.opsForValue().get(lockKey);//获取最新锁内容
        }
        if (value.startsWith(getServerMark())) {//判断自己是否是锁的拥有者
            return true;
        }

        final String[] strings = value.split(",");
        if (strings.length !=2) {//如果锁内容格式不正确则获尝试获取锁
            return resetAndGetLock(lockKey);
        }
        //拥有者最后更新锁的时间
        final Long lastUpdateTime = Longs.tryParse(strings[1]);
        //如果锁的拥有者长时间不更新锁内容,说明拥有者已经出现故障,则尝试获取锁
        if (lastUpdateTime == null || System.currentTimeMillis() - lastUpdateTime > maxHoldTime) {
            return resetAndGetLock(lockKey);
        }
        return false;
    }

    private String getServerMark() {
        return System.getProperty("server_name") + System.getProperty("server_ip");
    }

    private String getLockValue() {
        return getServerMark() + "," + System.currentTimeMillis();
    }
}
