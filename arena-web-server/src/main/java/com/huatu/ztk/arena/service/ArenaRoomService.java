package com.huatu.ztk.arena.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.huatu.ztk.arena.bean.*;
import com.huatu.ztk.arena.common.ArenaRoomType;
import com.huatu.ztk.arena.common.RedisArenaKeys;
import com.huatu.ztk.arena.dao.ArenaRoomDao;
import com.huatu.ztk.arena.dubbo.ArenaDubboService;
import com.huatu.ztk.arena.dubbo.ArenaPlayerDubboService;
import com.huatu.ztk.commons.*;
import com.huatu.ztk.commons.exception.BizException;
import com.huatu.ztk.paper.api.PracticeCardDubboService;
import com.huatu.ztk.paper.api.PracticeDubboService;
import com.huatu.ztk.paper.bean.AnswerCard;
import com.huatu.ztk.paper.bean.PracticePaper;
import com.huatu.ztk.paper.common.AnswerCardType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
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
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ArenaPlayerDubboService arenaPlayerDubboService;

    @Autowired
    private ArenaDubboService arenaDubboService;

    @Autowired
    private ArenaRewardService arenaRewardService;

    /**
     * 随机创建一个房间
     *
     * @return
     */
    public ArenaRoom create(Integer moduleId) {
        int type = ArenaRoomType.RANDOM_POINT;//默认是随机知识点
        String roomName = "竞技-智能推送" + "-" + DateFormatUtils.format(System.currentTimeMillis(), "yyyyMMddHHmm");
        String moduleName = "智能推送";
        List<Module> modules = ModuleConstants.getModulesBySubject(SubjectType.GWY_XINGCE);
        final int tmpId = moduleId;
        final Module module = modules.stream().filter(m -> m.getId() == tmpId).findFirst().orElse(null);
        if (module != null) {
            roomName = "竞技-" + module.getName() + "-" + DateFormatUtils.format(System.currentTimeMillis(), "yyyyMMddHHmm");
            moduleName = module.getName();
            type = ArenaRoomType.SPECIFIED_POINT;
        } else {//查询不到,说明是随机知识点
            //随机选取模块
            moduleId = modules.get(RandomUtils.nextInt(0, modules.size())).getId();
        }
        //创建竞技试卷
        final PracticePaper practicePaper = practiceDubboService.create(SubjectType.GWY_XINGCE, moduleId, ArenaConfig.getConfig().getQuestionCount());
        practicePaper.setName(roomName);//需要设置竞技练习的名字
        final ValueOperations valueOperations = redisTemplate.opsForValue();
        final String arenaIdKey = RedisArenaKeys.getRoomIdKey();
        if (!redisTemplate.hasKey(arenaIdKey)) {//初始化id
            valueOperations.set(arenaIdKey, "23448564");
        }

        int delta = RandomUtils.nextInt(1, 4);//随机步长
        final Long id = valueOperations.increment(arenaIdKey, delta);
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
        arenaRoom.setModuleId(moduleId);
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
        return arenaDubboService.findById(arenaId);
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


        final long uid = answerCard.getUserId();
        int userIndex = arenaRoom.getPlayerIds().indexOf(uid);
        if (userIndex < 0) {
            logger.error("user not in arenaRoom={},practiceId={},uid={}", arenaRoom.getId(), practiceId, uid);
            return;
        }

        //遍历已有结果,防止重复处理
        if (arenaRoom.getResults()!=null && Arrays.stream(arenaRoom.getResults()).anyMatch(result -> result != null && result.getUid() == uid)) {//已经处理过的,不需要再进行处理
            logger.warn(" practiceId={} is in ArenaRoom results,so skip it.", practiceId);
            return;
        }

        final ArenaResult arenaResult = ArenaResult.builder()
                .elapsedTime(answerCard.getExpendTime())
                .rcount(answerCard.getRcount())
                .uid(uid)
                .build();


        Update update = new Update();
        update.push("results",arenaResult);
        final ArenaRoom arenaRoomUpdated = arenaRoomDao.updateById(arenaRoom.getId(), update);
        //删除用户的房间状态
        redisTemplate.delete(RedisArenaKeys.getUserRoomKey(uid));
        logger.info("add arena result arenaId={}, data={}", arenaRoom.getId(), JsonUtil.toJson(arenaResult));
        //计算目前已经出现的竞技结果数量,需要判断非null 和排重
        final long count = Arrays.stream(arenaRoomUpdated.getResults()).filter(result -> result != null).mapToLong(result -> {
            return result.getUid();
        }).distinct().count();
        if (count == arenaRoom.getPlayerIds().size()) {//说明都已经交卷
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
        if (arenaRoom.getStatus() == ArenaRoomStatus.FINISHED) {//已经结束,则不需要处理
            return;
        }
        ArenaResult[] arenaResults = Optional.ofNullable(arenaRoom.getResults()).orElse(new ArenaResult[0]);
        final List<ArenaResult> arenaResultList = Arrays.stream(arenaResults).filter(result -> result!=null).collect(Collectors.toList());
        for (int i = 0; i < arenaRoom.getPlayerIds().size(); i++) {
            long playerId = arenaRoom.getPlayerIds().get(i);
            //没有交卷
            if (arenaResultList.stream().noneMatch(result -> result.getUid()==playerId)) {
                final Long practiceId = arenaRoom.getPractices().get(i);
                try {
                    logger.info("practiceId={} not submit practice,system submit it.");
                    //首先帮用户提交试卷
                    practiceCardDubboService.submitAnswers(practiceId,arenaRoom.getPlayerIds().get(i),Lists.newArrayList(),true,-9);
                } catch (BizException e) {
                    e.printStackTrace();
                }

                AnswerCard answerCard = practiceCardDubboService.findById(practiceId);
                final ArenaResult arenaResult = ArenaResult.builder()
                        .elapsedTime(answerCard.getExpendTime())
                        .rcount(answerCard.getRcount())
                        .uid(answerCard.getUserId())
                        .build();
                //设置用户竞技结果
                arenaResultList.add(arenaResult);
            }
        }

        //去掉重复的竞技结果
        ArenaResult[] finalArenaResults = arenaResultList.stream().collect(Collectors.toMap(ArenaResult::getUid,result -> result)).values().stream().toArray(ArenaResult[]::new);

        //设置为已结束状态
        arenaRoom.setStatus(ArenaRoomStatus.FINISHED);
        Arrays.parallelSort(finalArenaResults,new Comparator<ArenaResult>(){
            @Override
            public int compare(ArenaResult o1, ArenaResult o2) {
                final int sub = o2.getRcount() - o1.getRcount();
                if (sub != 0) {//优先取答题正确数量的为胜者
                    return sub;
                }

                //答题数量一致,则取答题时间少的为胜者
                return o1.getElapsedTime() - o2.getElapsedTime();
            }
        });

        //用户id列表重新排序,保证和arenaResults一一对应
        final List<Long> uids = Arrays.stream(finalArenaResults).map(arenaResult -> arenaResult.getUid()).collect(Collectors.toList());
        //练习id列表重新排序,保证和arenaResults一一对应
        final List<Long> practices = uids.stream().map(uid -> {
            return arenaRoom.getPractices().get(arenaRoom.getPlayerIds().indexOf(uid));
        }).collect(Collectors.toList());

        ArenaResult winner = finalArenaResults[0];//胜者
        arenaRoom.setPlayerIds(uids);
        arenaRoom.setPractices(practices);
        arenaRoom.setResults(finalArenaResults);
        //设置胜者id
        arenaRoom.setWinner(winner.getUid());
        arenaRoomDao.save(arenaRoom);
        //所有的竞技结果已经处理完,需要对第一名进行胜场+1，其他人胜场设置为0
        final ZSetOperations<String, String> zSetOperations = redisTemplate.opsForZSet();
        //第一名的用户胜利场次+1
        final String arenaDayRankKey = RedisArenaKeys.getArenaDayRankKey(DateFormatUtils.format(System.currentTimeMillis(), "yyyyMMdd"));
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

        //发送加积分
        arenaRewardService.sendArenaWinMsg(winner.getUid());

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
            //如果查询到下一页数据为空，则将最后一次查询所用的游标返回回去
            return new PageBean(records, cursor, -1) ;
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
        final String arenaDayRankKey = RedisArenaKeys.getArenaDayRankKey(DateFormatUtils.format(date, "yyyyMMdd"));
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
        final String arenaDayRankKey = RedisArenaKeys.getArenaDayRankKey(DateFormatUtils.format(date, "yyyyMMdd"));
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
