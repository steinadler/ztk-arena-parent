package com.huatu.ztk.arena.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.huatu.ztk.arena.bean.*;
import com.huatu.ztk.arena.common.ArenaRoomType;
import com.huatu.ztk.arena.common.RedisArenaKeys;
import com.huatu.ztk.arena.dao.ArenaRoomDao;
import com.huatu.ztk.arena.dubbo.ArenaPlayerDubboService;
import com.huatu.ztk.commons.*;
import com.huatu.ztk.paper.api.PracticeCardDubboService;
import com.huatu.ztk.paper.api.PracticeDubboService;
import com.huatu.ztk.paper.bean.AnswerCard;
import com.huatu.ztk.paper.bean.PracticePaper;
import com.huatu.ztk.paper.common.AnswerCardType;
import com.huatu.ztk.user.dubbo.UserDubboService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * 竞技场服务层
 * Created by shaojieyue
 * Created time 2016-07-05 10:25
 */

@Service
public class ArenaRoomService {
    private static final Logger logger = LoggerFactory.getLogger(ArenaRoomService.class);
    public static final int TODAY_MAX_RANK_COUNT = 20;
    @Resource(name = "redisTemplate")
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ArenaRoomDao arenaRoomDao;

    @Autowired
    private PracticeDubboService practiceDubboService;

    @Autowired
    private PracticeCardDubboService practiceCardDubboService;

    @Autowired
    private UserDubboService userDubboService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ArenaPlayerDubboService arenaPlayerDubboService;

    /**
     * 随机创建一个房间
     *
     * @return
     */
    public ArenaRoom create(Integer moduleId) {
        int type = ArenaRoomType.RANDOM_POINT;//默认是随机知识点
        String roomName = "竞技-智能推送" + "-" + DateFormatUtils.format(System.currentTimeMillis(), "yyyyMMddHHmm");
        String moduleName = "智能推送";
        final Module module = ModuleConstants.getModuleById(moduleId);
        if (module != null) {
            roomName = "竞技-" + module.getName() + "-" + DateFormatUtils.format(System.currentTimeMillis(), "yyyyMMddHHmm");
            moduleName = module.getName();
            type = ArenaRoomType.SPECIFIED_POINT;
        } else {//查询不到,说明是随机知识点
            //随机选取模块
            moduleId = ModuleConstants.GOWUYUAN_MODULE_IDS.get(RandomUtils.nextInt(0, ModuleConstants.GOWUYUAN_MODULE_IDS.size()));
        }

        final PracticePaper practicePaper = practiceDubboService.create(SubjectType.SUBJECT_GONGWUYUAN, moduleId, ArenaConfig.getConfig().getQuestionCount());
        final ValueOperations valueOperations = redisTemplate.opsForValue();
        final String roomIdKey = RedisArenaKeys.getRoomIdKey();

        if (!redisTemplate.hasKey(roomIdKey)) {//初始化id
            valueOperations.set(roomIdKey, "23448564");
        }

        int delta = RandomUtils.nextInt(1, 4);//随机步长
        final Long id = valueOperations.increment(roomIdKey, delta);
        final ArenaRoom arenaRoom = ArenaRoom.builder()
                .limitTime(ArenaConfig.getConfig().getGameLimitTime())
                .practicePaper(practicePaper)
                .qcount(practicePaper.getQcount())
                .playerIds(new ArrayList<Long>())
                .practices(new ArrayList<Long>())
                .build();
        arenaRoom.setCreateTime(System.currentTimeMillis());
        arenaRoom.setId(id);
        arenaRoom.setType(type);
        arenaRoom.setModule(moduleName);
        arenaRoom.setName(roomName);
        arenaRoom.setStatus(ArenaRoomStatus.CREATED);
        arenaRoomDao.insert(arenaRoom);
        return arenaRoom;
    }

    /**
     * 查询竞技记录详情
     *
     * @param arenaId 竞技房间id
     * @return
     */
    public ArenaRoom findById(long arenaId) {
        final ArenaRoom arenaRoom = arenaRoomDao.findById(arenaId);
        if (arenaRoom == null) {
            return arenaRoom;
        }
        //设置参赛人员个人信息(mongo为节省空间未存个人信息数据)
        List<Long> playerIds = arenaRoom.getPlayerIds();
        arenaRoom.setPlayers(arenaPlayerDubboService.findBatch(playerIds));
        return arenaRoom;
    }


    /**
     * 添加新的竞技结果
     *
     * @param practiceId
     */
    public void addArenaResult(long practiceId) {
        AnswerCard answerCard = practiceCardDubboService.findById(practiceId);
        if (answerCard == null) {
            logger.error("practiceId={} not exist", practiceId);
            return;
        }

        if (answerCard.getType() != AnswerCardType.ARENA_PAPER) {//只处理竞技场的答题卡
            return;
        }

        //查询该练习对应的竞技场房间
        final ArenaRoom arenaRoom = arenaRoomDao.findByPracticeId(practiceId);
        if (arenaRoom == null) {
            logger.error("practiceId={} not find it`s arena room.");
            return;
        }

        if (arenaRoom.getStatus() == ArenaRoomStatus.FINISHED) {//已经结束,则不需要处理
            logger.info("arenaId={} are finished,no process,practiceId={}", arenaRoom.getId(), practiceId);
            return;
        }

        //竞技结果
        ArenaResult[] results = Optional.ofNullable(arenaRoom.getResults()).orElse(new ArenaResult[arenaRoom.getPractices().size()]);

        final long uid = answerCard.getUserId();
        int userIndex = arenaRoom.getPlayerIds().indexOf(uid);
        if (userIndex < 0) {
            logger.error("user not in arenaRoom={},practiceId={},uid={}", arenaRoom.getId(), practiceId, uid);
            return;
        }

        //遍历已有结果,防止重复处理
        if (Arrays.stream(results).anyMatch(result -> result != null && result.getUid() == uid)) {//已经处理过的,不需要再进行处理
            logger.warn(" practiceId={} is in ArenaRoom results,so skip it.", practiceId);
            return;
        }

        final ArenaResult arenaResult = ArenaResult.builder()
                .elapsedTime(answerCard.getExpendTime())
                .rcount(answerCard.getRcount())
                .uid(uid)
                .build();

        //添加新的竞技结果
        results[userIndex] = arenaResult;
        //更新竞技排名
        arenaRoom.setResults(results);

        arenaRoomDao.save(arenaRoom);

        //删除用户的房间状态
        redisTemplate.delete(RedisArenaKeys.getUserRoomKey(uid));
        logger.info("add arena result arenaId={}, data={}", arenaRoom.getId(), JsonUtil.toJson(arenaResult));
        //更新用户竞技记录
        updateUserArenaRecord(arenaRoom.getId(), answerCard, uid);
        if (Arrays.stream(results).filter(result -> result != null).count() == arenaRoom.getPlayerIds().size()) {//说明都已经交卷
            closeArena(arenaRoom.getId());//关闭房间
        }
    }

    /**
     * 关闭竞技场
     *
     * @param arenaId 竞技场id
     */
    public void closeArena(long arenaId) {
        final ArenaRoom arenaRoom = arenaRoomDao.findById(arenaId);
        ArenaResult[] arenaResults = Optional.ofNullable(arenaRoom.getResults()).orElse(new ArenaResult[arenaRoom.getPractices().size()]);
        //存在未交卷的用户
        if (Arrays.stream(arenaResults).filter(result -> result != null).count() < arenaRoom.getPlayerIds().size()) {//存在未交卷的
            for (int i = 0; i < arenaResults.length; i++) {
                if (arenaResults[i] == null) {//未交卷
                    final Long practiceId = arenaRoom.getPractices().get(i);
                    AnswerCard answerCard = practiceCardDubboService.findById(practiceId);
                    final ArenaResult arenaResult = ArenaResult.builder()
                            .elapsedTime(answerCard.getExpendTime())
                            .rcount(answerCard.getRcount())
                            .uid(answerCard.getUserId())
                            .build();
                    //设置用户竞技结果
                    arenaResults[i] = arenaResult;
                }
            }
            arenaRoom.setResults(arenaResults);
        }

        //设置为已结束状态
        arenaRoom.setStatus(ArenaRoomStatus.FINISHED);

        int maxRcount = -1;
        ArenaResult winner = null;//胜者id
        for (ArenaResult result : arenaRoom.getResults()) {//遍历答题结果,获取计算出胜者id
            if (result.getRcount() > maxRcount) {//正确数量多,取新的
                maxRcount = result.getRcount();
                winner = result;
            } else if (result.getRcount() == maxRcount) {//两人答对数量一样
                if (result.getElapsedTime() < winner.getElapsedTime()) {//当两人答对数量一致,那么用时短的获胜
                    winner = result;
                }
            }
        }
        //设置胜者id
        arenaRoom.setWinner(winner.getUid());
        arenaRoomDao.save(arenaRoom);
        //所有的竞技结果已经处理完,需要对第一名进行胜场+1，其他人胜场设置为0
        final ZSetOperations<String, String> zSetOperations = redisTemplate.opsForZSet();
        //第一名的用户胜利场次+1
        final String arenaDayRankKey = RedisArenaKeys.getArenaDayRankKey(DateFormatUtils.format(System.currentTimeMillis(), "yyyymmdd"));
        zSetOperations.incrementScore(arenaDayRankKey, arenaRoom.getWinner() + "", 1);
        //其他参赛者胜场+0，保证用户查询我的今日排行时不为null
        List<Long> playerIds = arenaRoom.getPlayerIds();
        for (Long playerId : playerIds) {
            if (playerId != winner.getUid()) {
                zSetOperations.incrementScore(arenaDayRankKey, playerId + "", 0);
            }
        }
        redisTemplate.expire(arenaDayRankKey, 20, TimeUnit.DAYS);//记录20天有效

        //发送消息,通知系统,给用户发送查看竞技结果通知
        Map data = Maps.newHashMap();
        data.put("arenaId", arenaRoom.getId());
        //发送竞技场关闭通知
        rabbitTemplate.convertAndSend("close_arena_exchange", "", data);

    }

    /**
     * 更新用户竞技记录
     *
     * @param arenaId
     * @param answerCard
     * @param userDto
     */
    private void updateUserArenaRecord(long arenaId, AnswerCard answerCard, long userDto) {
        logger.info("update userId={} UserArenaRecord,arenaId={}", answerCard.getId(), arenaId);
    }

    /**
     * 分页查询我的竞技记录
     *
     * @param uid    用户id
     * @param cursor 游标
     * @return
     */
    public PageBean<ArenaRoomSimple> history(long uid, long cursor, int size) {
        List<ArenaRoomSimple> records = arenaRoomDao.findForPage(uid, cursor, size);
        if (CollectionUtils.isEmpty(records)) {
            return null;
        }
        long newCursor = cursor;
        if (records.size() > 0) {//最晚的一条练习的id作为下次请求的游标
            newCursor = records.get(records.size() - 1).getId();
        }
        PageBean pageBean = new PageBean(records, newCursor, -1);

        return pageBean;
    }

    /**
     * 查询今日排行
     *
     * @param date 查询某天的排行情况
     * @return
     */
    public List<UserArenaRecord> findTodayRank(long date) {
        final ZSetOperations<String, String> zSetOperations = redisTemplate.opsForZSet();
        final String arenaDayRankKey = RedisArenaKeys.getArenaDayRankKey(DateFormatUtils.format(date, "yyyymmdd"));
        final Set<String> strings = zSetOperations.reverseRange(arenaDayRankKey, 0, TODAY_MAX_RANK_COUNT - 1);
        //若当天暂未有任何用户参加过竞技比赛，返回null
        if (CollectionUtils.isEmpty(strings)) {
            return null;
        }
        List<UserArenaRecord> records = Lists.newArrayList();
        for (String uidStr : strings) {
            //获胜场数
            final int winCount = zSetOperations.score(arenaDayRankKey, uidStr).intValue();

            final Player player = arenaPlayerDubboService.findById(Long.valueOf(uidStr));
            final UserArenaRecord arenaRecord = UserArenaRecord.builder()
                    .uid(player.getUid())
                    .player(player)
                    .winCount(winCount)
                    .build();
            records.add(arenaRecord);
        }

        //对排行做倒叙排
        records.sort(new Comparator<UserArenaRecord>() {
            @Override
            public int compare(UserArenaRecord o1, UserArenaRecord o2) {
                return o2.getWinCount() - o1.getWinCount();//倒序排
            }
        });
        for (int i = 0; i < records.size(); i++) {//设置名次
            records.get(i).setRank(i + 1);
        }
        return records;
    }

    /**
     * 查询我的某天的竞技记录
     *
     * @param uid  用户id
     * @param date 要查询的日期
     * @return
     */
    public UserArenaRecord findMyTodayRank(long uid, long date) {
        final ZSetOperations<String, String> zSetOperations = redisTemplate.opsForZSet();
        final String arenaDayRankKey = RedisArenaKeys.getArenaDayRankKey(DateFormatUtils.format(date, "yyyymmdd"));
        //获胜场数,若用户未参加过比赛胜场返回为null
        final Optional<Double> winCount = Optional.ofNullable(zSetOperations.score(arenaDayRankKey, uid + ""));
        if (!winCount.isPresent()) {
            return null;
        }
        //我的排行,redis rank从0算起
        final Optional<Long> rank = Optional.ofNullable(zSetOperations.reverseRank(arenaDayRankKey, uid + ""));
        if (!rank.isPresent()) {
            return null;
        }

        final Player player = arenaPlayerDubboService.findById(uid);
        final UserArenaRecord arenaRecord = UserArenaRecord.builder()
                .uid(player.getUid())
                .player(player)
                .winCount(winCount.get().intValue())
                .rank(rank.get().intValue() + 1) //返回排行从1开始
                .build();
        return arenaRecord;
    }

}
