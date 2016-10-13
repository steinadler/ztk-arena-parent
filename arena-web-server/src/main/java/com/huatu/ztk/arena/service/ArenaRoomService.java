package com.huatu.ztk.arena.service;

import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;
import com.huatu.ztk.arena.bean.*;
import com.huatu.ztk.arena.common.ArenaErrors;
import com.huatu.ztk.arena.common.ArenaRoomType;
import com.huatu.ztk.arena.common.RedisArenaKeys;
import com.huatu.ztk.arena.dao.ArenaRoomDao;
import com.huatu.ztk.arena.dao.UserArenaRecordDao;
import com.huatu.ztk.commons.*;
import com.huatu.ztk.commons.exception.BizException;
import com.huatu.ztk.commons.exception.CommonErrors;
import com.huatu.ztk.paper.api.PracticeCardDubboService;
import com.huatu.ztk.paper.api.PracticeDubboService;
import com.huatu.ztk.paper.bean.AnswerCard;
import com.huatu.ztk.paper.bean.PracticePaper;
import com.huatu.ztk.paper.common.AnswerCardType;
import com.huatu.ztk.user.bean.UserDto;
import com.huatu.ztk.user.dubbo.UserDubboService;
import org.apache.commons.collections.comparators.ComparableComparator;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.types.RedisClientInfo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

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

    /**
     * 随机创建一个房间
     *
     * @return
     */
    public ArenaRoom create(Integer moduleId){
        int type = ArenaRoomType.RANDOM_POINT;//默认是随机知识点
        String roomName = "竞技-综合知识点";

        final Module module = ModuleConstants.getModuleById(moduleId);
        if (module != null) {
            roomName = "竞技-" + module.getName();
            type = ArenaRoomType.SPECIFIED_POINT;
        } else {//查询不到,说明是随机知识点
            //随机选取模块
            moduleId = ModuleConstants.GOWUYUAN_MODULE_IDS.get(RandomUtils.nextInt(0, ModuleConstants.GOWUYUAN_MODULE_IDS.size()));
        }
        //此处生成的PracticePaper没有组成试卷name，应参考微信组试卷逻辑
        final PracticePaper practicePaper = practiceDubboService.create(SubjectType.SUBJECT_GONGWUYUAN,moduleId,ArenaConfig.getConfig().getQuestionCount());
        final ValueOperations valueOperations = redisTemplate.opsForValue();
        final String roomIdKey = RedisArenaKeys.getRoomIdKey();

        if (!redisTemplate.hasKey(roomIdKey)) {//初始化id
            valueOperations.set(roomIdKey, "23448564");
        }

        int delta = RandomUtils.nextInt(1, 4);//随机步长
        final Long id = valueOperations.increment(roomIdKey, delta);
        final ArenaRoom arenaRoom = ArenaRoom.builder()
                .time(ArenaConfig.getConfig().getGameLimitTime())
                .practicePaper(practicePaper)
                .qcount(practicePaper.getQcount())
                .playerIds(new ArrayList<Long>())
                .practices(new ArrayList<Long>())
                .build();
        arenaRoom.setCreateTime(System.currentTimeMillis());
        arenaRoom.setId(id);
        arenaRoom.setType(type);
        arenaRoom.setModule(roomName);
        arenaRoom.setName(roomName);
        arenaRoom.setStatus(ArenaRoomStatus.CREATED);
        arenaRoomDao.insert(arenaRoom);
        return arenaRoom;
    }


    public ArenaRoom findById(long roomId, long uid) throws BizException {
/*        final ArenaRoom arenaRoom = arenaRoomDao.findById(roomId);
        if (arenaRoom == null) {
            return arenaRoom;
        }
        if (!arenaRoom.getPlayerIds().contains(uid)) {
            throw new BizException(CommonErrors.PERMISSION_DENIED);
        }*/
        ArenaRoom arenaRoom =  new ArenaRoom();
        //设置房间基本信息
        arenaRoom.setId(23449972);
        arenaRoom.setTime(300);//比赛限时,单位:秒
        arenaRoom.setType(3);
        arenaRoom.setStatus(3);
        arenaRoom.setModule("智能推送");
        arenaRoom.setName("竞技赛场—智能推送—201605102434");
        arenaRoom.setCreateTime(1467861939980L);
        arenaRoom.setWinner(34693);

        ////设置各玩家uid
        List<Long> playerIds = Lists.newArrayList();
        playerIds.add(uid);
        playerIds.add(34693L); //胜者
        playerIds.add(12345L);
        arenaRoom.setPlayerIds(playerIds);

        //设置各玩家信息
        List<Player> players = Lists.newArrayList();
        Player player1 = findPlayer(uid);
        Player player2 = Player.builder().uid(34693).nick("奋斗的小爆爆")
                .avatar("http://tiku.huatu.com/cdn/images/vhuatu/avatars/l/lMIkOc5PsQFCSrO94xAxR4U9ULf.jpg").build();
        Player player3 = Player.builder().uid(12345).nick("采梦abcd")
                .avatar("http://tiku.huatu.com/cdn/images/vhuatu/avatars/default.png").build();
        players.add(player1);
        players.add(player2);
        players.add(player3);
        arenaRoom.setPlayers(players);
        //设置各玩家对应的练习id
        List<Long> practices = Lists.newArrayList();
        practices.add(24330159L);
        practices.add(24330124L);
        practices.add(24330108L);
        arenaRoom.setPractices(practices);
        //设置房间比赛用题数量
        arenaRoom.setQcount(5);
        //设置竞技场状态--已结束
        arenaRoom.setStatus(ArenaRoomStatus.FINISHED);
        //设置竞技试卷
        PracticePaper practicePaper = practiceDubboService.create(1, 392, 5);
        arenaRoom.setPracticePaper(practicePaper);
        //设置竞技结果
        List<ArenaResult> results = Lists.newArrayList();
        ArenaResult result1 = ArenaResult.builder().uid(uid).rcount(5).elapsedTime(200).build();
        ArenaResult result2 = ArenaResult.builder().uid(34693).rcount(4).elapsedTime(250).build();
        ArenaResult result3 = ArenaResult.builder().uid(12345).rcount(3).elapsedTime(230).build();
        results.add(result1);
        results.add(result2);
        results.add(result3);
        arenaRoom.setResults(results);
        return arenaRoom;
    }


    /**
     * 添加新的竞技结果
     *
     * @param id
     */
    public void addArenaResult(long id) {
        AnswerCard answerCard = practiceCardDubboService.findById(id);
        if (answerCard == null) {
            logger.error("practiceId={} not exist", id);
            return;
        }

        if (answerCard.getType() != AnswerCardType.ARENA_PAPER) {//只处理竞技场的答题卡
            return;
        }

        final long practiceId = answerCard.getId();

        //查询该练习对应的竞技场房间
        final ArenaRoom arenaRoom = arenaRoomDao.findByPracticeId(practiceId);
        if (arenaRoom == null) {
            logger.error("practiceId={} not find it`s arena room.");
            return;
        }

        //竞技结果
        List<ArenaResult> results = Optional.of(arenaRoom.getResults()).orElse(new ArrayList<>());

        final long uid = answerCard.getUserId();
        //遍历已有结果,防止重复处理
        for (ArenaResult result : results) {
            if (result.getUid() == uid) {//已经处理过的,不需要再进行处理
                logger.warn(" practiceId={} is in ArenaRoom results,so skip it.");
                return;
            }
        }

        final UserDto userDto = userDubboService.findById(uid);
        final ArenaResult arenaResult = ArenaResult.builder()
                .elapsedTime(answerCard.getExpendTime())
                .rcount(answerCard.getRcount())
                .nick(userDto.getNick())
                .uid(uid)
                .build();

        //添加新的竞技结果
        results.add(arenaResult);
        //更新竞技排名
        arenaRoom.setResults(results);

        //答题结果已经够数,说明都已经交卷
        if (arenaRoom.getResults().size() >= arenaRoom.getPlayerIds().size()) {
            //状态设置为已完成
            arenaRoom.setStatus(ArenaRoomStatus.FINISHED);
            int maxRcount = -1;
            ArenaResult winner = null;//胜者id
            for (ArenaResult result : results) {//遍历答题结果,获取计算出胜者id
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
        }
        arenaRoomDao.save(arenaRoom);

        //删除用户的房间状态
        redisTemplate.delete(RedisArenaKeys.getUserRoomKey(uid));
        logger.info("add arena result roomId={}, data={}", arenaRoom.getId(), JsonUtil.toJson(arenaResult));
        //更新用户竞技记录
        updateUserArenaRecord(arenaRoom.getId(), answerCard, userDto);

        //所有的竞技结果已经处理完,需要对第一名进行胜场+1
        if (arenaRoom.getStatus() == ArenaRoomStatus.FINISHED) {
            final ZSetOperations<String, String> zSetOperations = redisTemplate.opsForZSet();
            //第一名的用户胜利场次+1
            final String arenaDayRankKey = RedisArenaKeys.getArenaDayRankKey(DateFormatUtils.format(System.currentTimeMillis(), "yyyymmdd"));
            zSetOperations.incrementScore(arenaDayRankKey, arenaRoom.getWinner() + "", 1);
            redisTemplate.expire(arenaDayRankKey, 20, TimeUnit.DAYS);//记录20天有效
        }
    }

    /**
     * 更新用户竞技记录
     *
     * @param arenaId
     * @param answerCard
     * @param userDto
     */
    private void updateUserArenaRecord(long arenaId, AnswerCard answerCard, UserDto userDto) {
        logger.info("update userId={} UserArenaRecord,arenaId={}", answerCard.getId(), arenaId);
    }

    /**
     * 分页查询我的竞技记录
     *
     * @param uid    用户id
     * @param cursor 游标
     * @return
     */
    public PageBean<ArenaRoomSimple> history(long uid, long cursor, int size, int cardType) {
        List<ArenaRoomSimple> records = Lists.newArrayList();
        ArenaRoomSimple record1 = new ArenaRoomSimple();
        record1.setId(23449963);
        record1.setType(2);
        record1.setStatus(3); //房间状态--比赛已结束
        record1.setModule("智能推送");
        record1.setName("竞技赛场—智能推送—201605102433");
        record1.setCreateTime(1467868477455L);
        //前端在竞技历史界面展示胜负时，根据userId匹配，如当前userId与winner相同，则表示该参赛者为该房间的胜者，
        record1.setWinner(34218);

        ArenaRoomSimple record2 = new ArenaRoomSimple();
        record2.setId(23449972);
        record2.setType(3);
        record2.setStatus(3);
        record2.setModule("智能推送");
        record2.setName("竞技赛场—智能推送—201605102434");
        record2.setCreateTime(1467861939980L);
        record2.setWinner(34693);

        ArenaRoomSimple record3 = new ArenaRoomSimple();
        record3.setId(23449981);
        record3.setType(4);
        record3.setStatus(3);
        record3.setModule("智能推送");
        record3.setName("竞技赛场—智能推送—201605102435");
        record3.setCreateTime(1467861943606L);
        record3.setWinner(35548);

        records.add(record1);
        records.add(record2);
        records.add(record3);

        long newCursor = cursor;
        if (records.size() > 0) {//最晚的一条练习的id作为下次请求的游标
            newCursor = records.get(records.size() - 1).getId();
        }
        PageBean pageBean = new PageBean(records, newCursor, -1);

        return pageBean;
    }

    public Player findPlayer(long uid) {
        final UserDto userDto = userDubboService.findById(uid);
        if (userDto == null) {
            return null;
        }
        final Player player = Player.builder()
                .uid(userDto.getId())
                .avatar(userDto.getAvatar())
                .nick(userDto.getNick())
                .build();
        return player;
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
        List<UserArenaRecord> records = Lists.newArrayList();
        for (String uidStr : strings) {
            //获胜场数
            final int winCount = zSetOperations.score(arenaDayRankKey, uidStr).intValue();
            final Player player = findPlayer(Long.valueOf(uidStr));
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
        //获胜场数
        final int winCount = zSetOperations.score(arenaDayRankKey, uid + "").intValue();
        //我的排行,redis rank从0算起
        Long rank = Optional.ofNullable(zSetOperations.reverseRank(arenaDayRankKey, uid + "")).orElse(20000L) + 1;
        final Player player = findPlayer(uid);
        final UserArenaRecord arenaRecord = UserArenaRecord.builder()
                .uid(player.getUid())
                .player(player)
                .winCount(winCount)
                .rank(rank.intValue())
                .build();
        return arenaRecord;
    }

}
