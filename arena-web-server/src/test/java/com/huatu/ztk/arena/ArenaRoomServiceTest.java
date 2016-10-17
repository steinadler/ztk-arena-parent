package com.huatu.ztk.arena;

import com.huatu.ztk.arena.bean.ArenaConfig;
import com.huatu.ztk.arena.bean.ArenaResult;
import com.huatu.ztk.arena.bean.ArenaRoom;
import com.huatu.ztk.arena.bean.ArenaRoomStatus;
import com.huatu.ztk.arena.dao.ArenaRoomDao;
import com.huatu.ztk.arena.service.ArenaRoomService;
import com.huatu.ztk.paper.api.PracticeCardDubboService;
import com.huatu.ztk.paper.bean.AnswerCard;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;

/**
 * Created by shaojieyue
 * Created time 2016-07-05 12:46
 */
public class ArenaRoomServiceTest extends BaseTest{
    private static final Logger logger = LoggerFactory.getLogger(ArenaRoomServiceTest.class);

    @Autowired
    private ArenaRoomService arenaRoomService;

    @Autowired
    private PracticeCardDubboService practiceCardDubboService;

    @Resource(name = "redisTemplate")
    private RedisTemplate redisTemplate;

    @Autowired
    private ArenaRoomDao arenaRoomDao;

    long uid = 12252065;

    @Test
    public void createTest(){
        int[] counts = new int[]{2,4,8};
        for (int i = 0; i < 60; i++) {
            int count = counts[RandomUtils.nextInt(0,counts.length)];
            final ArenaRoom arenaRoom = arenaRoomService.create(-1);
            Assert.assertNotNull(arenaRoom);
            Assert.assertEquals(arenaRoom.getQcount(), ArenaConfig.getConfig().getQuestionCount());
            Assert.assertTrue(arenaRoom.getCreateTime()>0);
            Assert.assertTrue(arenaRoom.getId()>0);
            Assert.assertNotNull(arenaRoom.getPracticePaper());
        }
    }

    @Test
    public void addArenaResultTest(){
        final int roomId = 52487248;
        //初始化
        ArenaRoom arenaRoom = arenaRoomService.findById(roomId);
        arenaRoom.setStatus(ArenaRoomStatus.RUNNING);
        arenaRoom.setResults(null);
        arenaRoom.setWinner(0);
        arenaRoomDao.save(arenaRoom);

        long practiceId = 1525088653376749568L;
        arenaRoomService.addArenaResult(practiceId);
        AnswerCard answerCard = practiceCardDubboService.findById(practiceId);
        arenaRoom = arenaRoomService.findById(roomId);
        ArenaResult arenaResult = arenaRoom.getResults().get(arenaRoom.getPractices().indexOf(answerCard.getId()));
        Assert.assertEquals(answerCard.getRcount(),arenaResult.getRcount());
        Assert.assertEquals(answerCard.getExpendTime(),arenaResult.getElapsedTime());
        Assert.assertEquals(answerCard.getUserId(),arenaResult.getUid());
        Assert.assertEquals(arenaRoom.getStatus(),ArenaRoomStatus.RUNNING);
        Assert.assertEquals(0,arenaRoom.getWinner());

        practiceId = 1525088653443858432L;
        arenaRoomService.addArenaResult(practiceId);
        answerCard = practiceCardDubboService.findById(practiceId);
        arenaRoom = arenaRoomService.findById(roomId);
        arenaResult = arenaRoom.getResults().get(arenaRoom.getPractices().indexOf(answerCard.getId()));
        Assert.assertEquals(answerCard.getRcount(),arenaResult.getRcount());
        Assert.assertEquals(answerCard.getExpendTime(),arenaResult.getElapsedTime());
        Assert.assertEquals(answerCard.getUserId(),arenaResult.getUid());
        Assert.assertEquals(arenaRoom.getStatus(),ArenaRoomStatus.RUNNING);
        Assert.assertEquals(0,arenaRoom.getWinner());

        practiceId = 1525088653485801472L;
        arenaRoomService.addArenaResult(practiceId);
        answerCard = practiceCardDubboService.findById(practiceId);
        arenaRoom = arenaRoomService.findById(roomId);
        arenaResult = arenaRoom.getResults().get(arenaRoom.getPractices().indexOf(answerCard.getId()));
        Assert.assertEquals(answerCard.getRcount(),arenaResult.getRcount());
        Assert.assertEquals(answerCard.getExpendTime(),arenaResult.getElapsedTime());
        Assert.assertEquals(answerCard.getUserId(),arenaResult.getUid());
        Assert.assertEquals(arenaRoom.getStatus(),ArenaRoomStatus.RUNNING);
        Assert.assertEquals(0,arenaRoom.getWinner());

        //最后一个
        practiceId = 1525088653527744512L;
        arenaRoomService.addArenaResult(practiceId);
        answerCard = practiceCardDubboService.findById(practiceId);
        arenaRoom = arenaRoomService.findById(roomId);
        arenaResult = arenaRoom.getResults().get(arenaRoom.getPractices().indexOf(answerCard.getId()));
        Assert.assertEquals(answerCard.getRcount(),arenaResult.getRcount());
        Assert.assertEquals(answerCard.getExpendTime(),arenaResult.getElapsedTime());
        Assert.assertEquals(answerCard.getUserId(),arenaResult.getUid());
        Assert.assertEquals(arenaRoom.getStatus(),ArenaRoomStatus.FINISHED);
        Assert.assertEquals(1525088653376749568L,arenaRoom.getWinner());



    }
}
