package com.huatu.ztk.arena.task;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
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
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
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
import java.util.concurrent.*;

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
    //influxdb数据库
    public static final String INFLUXDB_DATABASE = "metrics";
    //开始竞技场游戏人次measurement
    public static final String START_ARENA_MEASUREMENT = "start_arena";
    //加入竞技场人次
    public static final String JOIN_ARENA_MEASUREMENT = "join_arena";
    public static final int JOIN_ARENT_ACTION = 1;
    public static final int START_AREAN_ACTION = 2;
    /**
     * 任务是否运行的表示
     */
    private volatile boolean running = true;

    private InfluxDB influxDB = InfluxDBFactory.connect("http://192.168.100.19:8086","ztkinfluxdb","ztkinfluxdb");
    LinkedBlockingQueue<Metric> queue = new LinkedBlockingQueue<Metric>(20000);
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

    @Autowired
    private DistributedLock distributedLock;

    @PostConstruct
    public void init() {
        startInfluxdbTask();
        for (ArenaConfig.Module module : ArenaConfig.getConfig().getModules()) {
            startWork(module);
        }
        //添加停止任务线程
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                running = false;//停止任务

                //遍历释放锁
                for (ArenaConfig.Module module : ArenaConfig.getConfig().getModules()) {
                    final String workLockKey = RedisArenaKeys.getWorkLockKey(module.getId());
                    final int maxHoldTime = ArenaConfig.getConfig().getWaitTime() * 1000+4000;
                    distributedLock.tryReleaseLock(workLockKey,maxHoldTime);
                }

            }
        }));
    }

    private void startInfluxdbTask() {
        //将加入游戏人次和开始游戏人次数据写入influxdb,用于报表统计
        new Thread(new Runnable() {
            @Override
            public void run() {

                for (;running;) {
                    try {
                        TimeUnit.MINUTES.sleep(1);//休眠一段时间
                        if(queue.size()>50 && running){//批量处理减少提交到influxdb的次数
                            Map<String,Integer> joinCountMap = Maps.newHashMap();
                            Map<String,Integer> startCountMap = Maps.newHashMap();
                            for (;;){//循环取出metric
                                final Metric metric = queue.poll();
                                if (metric == null) {
                                    break;
                                }

                                if (metric.action == JOIN_ARENT_ACTION) {//加入竞技场
                                    Integer count = joinCountMap.getOrDefault(metric.getModuleName(), 0);
                                    joinCountMap.put(metric.getModuleName(),count+metric.getCount());
                                }else if (metric.action == START_AREAN_ACTION) {//离开竞技场
                                    Integer count = startCountMap.getOrDefault(metric.getModuleName(), 0);
                                    startCountMap.put(metric.getModuleName(),count+metric.getCount());
                                }else {
                                    logger.info("unkonw action,data={}",metric);
                                }
                            }

                            final BatchPoints.Builder builder = BatchPoints.database(INFLUXDB_DATABASE);
                            for (String moduleName : startCountMap.keySet()) {

                                final Point point = Point.measurement(START_ARENA_MEASUREMENT)
                                        .tag("module", moduleName)
                                        .addField("count", startCountMap.get(moduleName)).build();
                                builder.point(point);//添加统计点
                            }

                            for (String moduleName : joinCountMap.keySet()) {
                                final Point point = Point.measurement(JOIN_ARENA_MEASUREMENT)
                                        .tag("module", moduleName)
                                        .addField("count", joinCountMap.get(moduleName)).build();
                                builder.point(point);//添加统计点
                            }

                            final BatchPoints batchPoints = builder.build();
                            //数据写入
                            influxDB.write(batchPoints);
                            logger.info("write data to influxdb, point size={}",batchPoints.getPoints().size());
                        }
                    }catch (Exception e){
                        logger.error("ex",e);
                    }
                }
            }
        }).start();
    }

    private void startWork(ArenaConfig.Module module) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //创建房间
                ArenaRoom arenaRoom = null;
                while (running) {
                    final String workLockKey = RedisArenaKeys.getWorkLockKey(module.getId());
                    final int maxHoldTime = ArenaConfig.getConfig().getWaitTime() * 1000+4000;
                    try {
                        if (!distributedLock.getLock(workLockKey,maxHoldTime)) {
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
                        distributedLock.updateLock(workLockKey);
                        if (arenaRoom == null) {
                            //创建房间
                            arenaRoom = arenaRoomService.create(module.getId());
                        }
                        final long arenaRoomId = arenaRoom.getId();
                        final String roomUsersKey = RedisArenaKeys.getRoomUsersKey(arenaRoomId);
                        final String arenaUsersKey = RedisArenaKeys.getArenaUsersKey(module.getId());
                        final SetOperations<String, String> setOperations = redisTemplate.opsForSet();
                        long start = Long.MAX_VALUE;//开始时间,默认不过期
                        //拥有足够人数和等待超时,则跳出循环
                        while (setOperations.size(roomUsersKey) < ArenaConfig.getConfig().getRoomCapacity() && System.currentTimeMillis() - start < ArenaConfig.getConfig().getWaitTime() * 1000 - 3000) {
                            final String userId = setOperations.pop(arenaUsersKey);
                            if (StringUtils.isBlank(userId)) {
                                Thread.sleep(1000);//没有玩家则休眠一段时间
                                distributedLock.updateLock(workLockKey);
                                continue;
                            }

                            //添加加入房间metric,用来记录加入游戏的人次
                            queue.offer(new Metric(module.getName(),JOIN_ARENT_ACTION,1));
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
                            for (int i = 0; i < robotSize; i++) {
                                final String robotId = setOperations.pop(robotsKey);
                                if (StringUtils.isNoneBlank(robotId)) {
                                    robots.add(Long.valueOf(robotId));//添加到机器人列表
                                    addUserToArena(arenaRoomId,roomUsersKey,robotId);
                                }
                            }
                            logger.info("add robot to arenaId={}, robotSize={}",arenaRoomId,robots.size());


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
                        //添加开始游戏metric,用来记录开始游戏的人次
                        queue.offer(new Metric(module.getName(),START_AREAN_ACTION,users.length - robots.size()));
                        logger.info("arenaId={},users={} start game.", arenaRoomId, users);
                    } catch (Exception e) {
                        logger.error("ex", e);
                    }
                }
                logger.info("moduleId={} work stoped", module.getId());
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
        logger.info("moduleId={} work started.",module.getId());
    }

    class Metric{
        private String moduleName;//模块名称
        private int action;//动作
        private int count;
        public Metric(String moduleName, int action) {
            this.moduleName = moduleName;
            this.action = action;
            this.count = 1;
        }

        public Metric(String moduleName, int action, int count) {
            this.moduleName = moduleName;
            this.action = action;
            this.count = count;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public String getModuleName() {
            return moduleName;
        }

        public void setModuleName(String moduleName) {
            this.moduleName = moduleName;
        }

        public int getAction() {
            return action;
        }

        public void setAction(int action) {
            this.action = action;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Metric{");
            sb.append("moduleName='").append(moduleName).append('\'');
            sb.append(", action=").append(action);
            sb.append('}');
            return sb.toString();
        }
    }
}
