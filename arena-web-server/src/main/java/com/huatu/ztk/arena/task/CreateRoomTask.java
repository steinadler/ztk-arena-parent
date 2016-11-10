package com.huatu.ztk.arena.task;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.primitives.Longs;
import com.huatu.ztk.arena.bean.ArenaConfig;
import com.huatu.ztk.arena.bean.ArenaRoom;
import com.huatu.ztk.arena.bean.ArenaRoomStatus;
import com.huatu.ztk.arena.common.Actions;
import com.huatu.ztk.arena.common.RedisArenaKeys;
import com.huatu.ztk.arena.dubbo.ArenaDubboService;
import com.huatu.ztk.arena.service.ArenaRoomService;
import com.huatu.ztk.paper.api.PracticeCardDubboService;
import com.huatu.ztk.paper.bean.PracticeCard;
import com.huatu.ztk.paper.common.AnswerCardType;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 *
 * 创建房间task
 * Created by shaojieyue
 * Created time 2016-10-08 21:36
 */

@Component
@Scope("singleton")
public class CreateRoomTask {
    private static final Logger logger = LoggerFactory.getLogger(CreateRoomTask.class);
    //最小玩家人数
    public static final int MIN_COUNT_PALYER_OF_ROOM = 2;
    /**
     * 任务是否运行的表示
     */
    private volatile boolean running = true;

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    @Autowired
    private ArenaRoomService arenaRoomService;

    @Autowired
    private PracticeCardDubboService practiceCardDubboService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ArenaDubboService arenaDubboService;

    @Autowired
    private RobotSubmitTask robotSubmitTask;

    @PostConstruct
    public void init() {
        for (ArenaConfig.Module module : ArenaConfig.getConfig().getModules()) {
            startWork(module.getId());
        }
        //添加停止任务线程
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                running = false;//停止任务

                //遍历释放锁
                for (ArenaConfig.Module module : ArenaConfig.getConfig().getModules()) {
                    tryReleaseLock(module.getId());
                }

            }
        }));
    }

    private void startWork(Integer moduleId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //创建房间
                ArenaRoom arenaRoom = null;
                while (running) {
                    try {
                        if (!getLock(moduleId)) {
                            TimeUnit.SECONDS.sleep(1);
                            //没有获取到锁,则sleep后继续尝试
                            continue;
                        }
                    }catch (Exception e){
                        logger.error("ex",e);
                        continue;
                    }
                    try {
                        //获取到锁就更新锁内容,来告诉其他服务,自己还存活着
                        updateLock(moduleId);
                        if (arenaRoom == null) {
                            //创建房间
                            arenaRoom = arenaRoomService.create(moduleId);
                        }
                        final long arenaRoomId = arenaRoom.getId();
                        final String roomUsersKey = RedisArenaKeys.getRoomUsersKey(arenaRoomId);
                        final String arenaUsersKey = RedisArenaKeys.getArenaUsersKey(moduleId);
                        final SetOperations<String, String> setOperations = redisTemplate.opsForSet();
                        long start = Long.MAX_VALUE;//开始时间,默认不过期
                        //拥有足够人数和等待超时,则跳出循环
                        while (setOperations.size(roomUsersKey) < ArenaConfig.getConfig().getRoomCapacity() && System.currentTimeMillis() - start < ArenaConfig.getConfig().getWaitTime() * 1000 - 3000) {
                            final String userId = setOperations.pop(arenaUsersKey);
                            if (StringUtils.isBlank(userId)) {
                                Thread.sleep(1000);//没有玩家则休眠一段时间
                                updateLock(moduleId);//更新锁状态
                                continue;
                            }
                            addUserToArena(arenaRoomId, roomUsersKey, userId);
                            //超时时间从第一个加入房间用户开始算起
                            if (setOperations.size(roomUsersKey) == 1) {
                                start = System.currentTimeMillis();//开始超时倒计时
                            }
                        }

                        final Long finalSize = setOperations.size(roomUsersKey);
                        Set<Long> robots = Sets.newHashSet();
                        if (finalSize < MIN_COUNT_PALYER_OF_ROOM) {//没有达到最小玩家人数
                            final String robotsKey = RedisArenaKeys.getRobotsKey();

                            //添加机器人,随机
                            final long robotSize = RandomUtils.nextLong(1, ArenaConfig.getConfig().getRoomCapacity() - finalSize + 1);
                            logger.info("add robot to arenaId={}, robotSize={}",arenaRoomId,robotSize);
                            for (int i = 0; i < robotSize; i++) {
                                final String robotId = setOperations.pop(robotsKey);
                                if (StringUtils.isNoneBlank(robotId)) {
                                    robots.add(Long.valueOf(robotId));//添加到机器人列表
                                    addUserToArena(arenaRoomId,roomUsersKey,robotId);
                                }
                            }


                            final Set<String> users = setOperations.members(roomUsersKey);
                            //再次检查是否达到开始游戏条件,没有的话,则清除用户数据
                            if (users.size() < MIN_COUNT_PALYER_OF_ROOM) {
                                logger.info("playerIds wait time out. users={}", users);
                                redisTemplate.delete(roomUsersKey);//清除用户数据
                                for (String user : users) {
                                    final String userRoomKey = RedisArenaKeys.getUserRoomKey(Long.valueOf(user));
                                    //清除用户占用的房间
                                    redisTemplate.delete(userRoomKey);
                                }
                                continue;
                            }
                        }

                        long[] users = setOperations.members(roomUsersKey).stream().mapToLong(userId -> Long.valueOf(userId)).toArray();
                        //设置有效期,让其自动回收
                        redisTemplate.expire(roomUsersKey, 1, TimeUnit.HOURS);
                        List<Long> practiceIds = Lists.newArrayList();
                        for (Long uid : users) {//为用户创建练习
                            final PracticeCard practiceCard = practiceCardDubboService.create(arenaRoom.getPracticePaper(), -1, AnswerCardType.ARENA_PAPER, uid, arenaRoom.getLimitTime());
                            practiceIds.add(practiceCard.getId());
                            if (robots.contains(uid)) {//该用户是机器人,练习做自动提交
                                robotSubmitTask.addNewRobotPractice(practiceCard);
                            }
                        }

                        Update update = Update.update("playerIds", users)
                                .set("practices", practiceIds)
                                .set("createTime", System.currentTimeMillis())//重新设置开始时间,倒计时时间以此为起始时间
                                .set("status", ArenaRoomStatus.RUNNING);
                        //更新房间数据
                        arenaDubboService.updateById(arenaRoomId, update);

                        arenaRoom = null;//设置为null,表示该房间已经被占用
                        Map data = Maps.newHashMap();
                        data.put("arenaId", arenaRoomId);
                        data.put("action", Actions.SYSTEM_START_GAME);
                        data.put("uids", users);
                        data.put("practiceIds", practiceIds);//用户对应的练习列表
                        //通过mq发送游戏就绪通知
                        rabbitTemplate.convertAndSend("game_notify_exchange", "", data);
                        logger.info("arenaId={},users={} start game.", arenaRoomId, users);
                    } catch (Exception e) {
                        logger.error("ex", e);
                    }
                }
                logger.info("moduleId={} work stoped", moduleId);
            }

            private void addUserToArena(long arenaRoomId, String roomUsersKey, String userId) {
                final SetOperations<String, String> setOperations = redisTemplate.opsForSet();
                //把用户加入游戏
                setOperations.add(roomUsersKey, userId);

                final String userRoomKey = RedisArenaKeys.getUserRoomKey(Long.valueOf(userId));
                //设置用户正在进入的房间
                redisTemplate.opsForValue().set(userRoomKey, arenaRoomId + "");
                logger.info("add userId={} to arenaId={}", userId, arenaRoomId);
                Map data = Maps.newHashMap();
                data.put("action", Actions.USER_JOIN_NEW_ARENA);
                //发送加入游戏通知
                data.put("uid", Long.valueOf(userId));
                data.put("arenaId", arenaRoomId);
                //通过mq发送新人进入通知
                rabbitTemplate.convertAndSend("game_notify_exchange", "", data);
            }


        }).start();
        logger.info("moduleId={} work started.",moduleId);
    }

    /**
     * 试着释放锁
     * 只有锁是属于自己时才会释放锁
     * @param moduleId
     */
    public void tryReleaseLock(int moduleId){
        if (getLock(moduleId)) {
            final String workLockKey = RedisArenaKeys.getWorkLockKey(moduleId);
            redisTemplate.delete(workLockKey);
        }
    }

    /**
     * 更新锁内容
     * @param moduleId
     */
    private void updateLock(int moduleId) {
        final String workLockKey = RedisArenaKeys.getWorkLockKey(moduleId);
        final String lockValue = getLockValue();
        redisTemplate.opsForValue().set(workLockKey, lockValue);
    }

    /**
     * 重置并尝试获取锁
     * @param workLockKey
     * @return
     */
    private boolean resetAndGetLock(String workLockKey){
        redisTemplate.delete(workLockKey);
        return redisTemplate.opsForValue().setIfAbsent(workLockKey, getLockValue()).booleanValue();
    }


    /**
     * 获取任务锁
     * @return
     */
    public boolean getLock(int moduleId) {
        final String workLockKey = RedisArenaKeys.getWorkLockKey(moduleId);
        //获取锁内容
        String value = redisTemplate.opsForValue().get(workLockKey);
        if (StringUtils.isBlank(value)) {//如果为空,说明没人占用锁,则尝试占用锁
            redisTemplate.opsForValue().setIfAbsent(workLockKey, getLockValue());
            value = redisTemplate.opsForValue().get(workLockKey);//获取最新锁内容
        }
        if (value.startsWith(getServerMark())) {//判断自己是否是锁的拥有者
            return true;
        }

        final String[] strings = value.split(",");
        if (strings.length !=2) {//如果锁内容格式不正确则获尝试获取锁
            return resetAndGetLock(workLockKey);
        }
        //拥有者最后更新锁的时间
        final Long lastUpdateTime = Longs.tryParse(strings[1]);
        //如果锁的拥有者长时间不更新锁内容,说明拥有者已经出现故障,则尝试获取锁
        if (lastUpdateTime == null || System.currentTimeMillis() - lastUpdateTime > ArenaConfig.getConfig().getWaitTime()*1000) {
            return resetAndGetLock(workLockKey);
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
