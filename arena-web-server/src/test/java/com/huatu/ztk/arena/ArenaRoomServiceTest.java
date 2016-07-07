package com.huatu.ztk.arena;

import com.google.common.primitives.Longs;
import com.huatu.ztk.arena.bean.ArenaRoom;
import com.huatu.ztk.arena.bean.ArenaRoomStatus;
import com.huatu.ztk.arena.bean.ArenaRoomSummary;
import com.huatu.ztk.arena.common.ArenaErrors;
import com.huatu.ztk.arena.common.RedisArenaKeys;
import com.huatu.ztk.arena.service.ArenaRoomService;
import com.huatu.ztk.commons.spring.BizException;
import com.huatu.ztk.user.bean.UserDto;
import com.huatu.ztk.user.dubbo.UserDubboService;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.*;

import javax.annotation.Resource;
import javax.xml.bind.annotation.XmlAttribute;

import static com.huatu.ztk.arena.common.ArenaErrors.ROOM_NOT_EXIST;

/**
 * Created by shaojieyue
 * Created time 2016-07-05 12:46
 */
public class ArenaRoomServiceTest extends BaseTest{
    private static final Logger logger = LoggerFactory.getLogger(ArenaRoomServiceTest.class);

    @Autowired
    private ArenaRoomService arenaRoomService;

    @Resource(name = "arenaRedisTemplate")
    private RedisTemplate arenaRedisTemplate;


    @Autowired
    private UserDubboService userDubboService;

    long uid = 12252065;

    @Test
    public void createTest(){
        int[] counts = new int[]{2,4,8};
        for (int i = 0; i < 60; i++) {
            int count = counts[RandomUtils.nextInt(0,counts.length)];
            final ArenaRoom arenaRoom = arenaRoomService.create(count);
            Assert.assertNotNull(arenaRoom);
            Assert.assertEquals(arenaRoom.getQcount(),ArenaRoomService.ARENA_QCOUNT);
            Assert.assertTrue(arenaRoom.getCreateTime()>0);
            Assert.assertTrue(arenaRoom.getId()>0);
            Assert.assertEquals(arenaRoom.getMaxPlayerCount(),count);
            Assert.assertEquals(arenaRoom.getTime(),ArenaRoomService.ARENA_LIMIT_TIME);
            Assert.assertNotNull(arenaRoom.getPracticePaper());
        }
    }

    @Test
    public void joinRoom() throws BizException {

        Object room = arenaRedisTemplate.opsForValue().get(RedisArenaKeys.getUserRoomKey(uid));
        if (room != null) {
            arenaRoomService.quitRoom(Long.valueOf(room.toString()),uid);
        }

        long noExistRoomId = 1234;
        try {
            arenaRoomService.joinRoom(noExistRoomId,uid);
            Assert.assertFalse(true);
        }catch (BizException e){
            Assert.assertEquals(ROOM_NOT_EXIST.getCode(),e.getErrorResult().getCode());
        }

        final int existRoomId = 23449484;
        arenaRoomService.joinRoom(existRoomId,uid);
        room = arenaRedisTemplate.opsForValue().get(RedisArenaKeys.getUserRoomKey(uid));
        Assert.assertEquals(Integer.valueOf(room.toString()).intValue(), existRoomId);

        ArenaRoom arenaRoom = arenaRoomService.findById(existRoomId);
        Assert.assertTrue(arenaRoom.getPlayers().indexOf(uid)>=0);
        arenaRoomService.joinRoom(existRoomId,uid);

        try {
            arenaRoomService.joinRoom(23449483,uid);
            Assert.assertFalse(true);
        }catch (BizException e){
            Assert.assertEquals(ArenaErrors.USER_IN_ROOM.getCode(),e.getErrorResult().getCode());
        }
        arenaRoomService.quitRoom(existRoomId,uid);
    }

    @Test
    public void quitRoomTest() throws BizException {
        Object room = arenaRedisTemplate.opsForValue().get(RedisArenaKeys.getUserRoomKey(uid));
        if (room != null) {
            arenaRoomService.quitRoom(Long.valueOf(room.toString()),uid);
        }

        final int roomId = 23449128;
        try {
            arenaRoomService.joinRoom(roomId,uid);
        }catch (Exception e){
        }
        final String str = (String)arenaRedisTemplate.opsForValue().get(RedisArenaKeys.getArenaOnlineCount());
        Long onlineCount = Longs.tryParse(str);
        if (onlineCount == null) {
            onlineCount = 0L;
        }
        arenaRoomService.quitRoom(roomId,uid);
        final ArenaRoom arenaRoom = arenaRoomService.findById(roomId);
        Assert.assertTrue(arenaRoom.getPlayers().indexOf(uid)<0);
        Assert.assertFalse(arenaRedisTemplate.hasKey(RedisArenaKeys.getUserRoomKey(uid)));
        Assert.assertEquals(onlineCount-1,Longs.tryParse((String)arenaRedisTemplate.opsForValue().get(RedisArenaKeys.getArenaOnlineCount())).longValue());

    }

    @Test
    public void startPkTest() throws BizException {
        Object room = arenaRedisTemplate.opsForValue().get(RedisArenaKeys.getUserRoomKey(uid));
        if (room != null) {
            arenaRoomService.quitRoom(Long.valueOf(room.toString()),uid);
            arenaRoomService.quitRoom(Long.valueOf(room.toString()),12252066);
        }

        ArenaRoom arenaRoom = arenaRoomService.create(4);
        final long roomId = arenaRoom.getId();
        arenaRoomService.joinRoom(roomId,uid);
        arenaRoomService.joinRoom(roomId,12252066);
        arenaRoomService.startPk(roomId,uid,1);
        arenaRoom = arenaRoomService.findById(roomId);
        final int index = arenaRoom.getPlayers().indexOf(uid);
        Assert.assertTrue(index >=0);
        Assert.assertTrue(arenaRoom.getPractices().get(index)>0);
        Assert.assertEquals(arenaRoom.getStatus(), ArenaRoomStatus.RUNNING);
    }

    @Test
    public void summaryTest(){
//        final ValueOperations valueOperations = arenaRedisTemplate.opsForValue();
//        final int count = Integer.valueOf(valueOperations.get(RedisArenaKeys.ARENA_ONLINE_COUNT).toString());
//        final int ongoingRoomCount = RandomUtils.nextInt(1,roomCount);
//        arenaRedisTemplate.executePipelined(new SessionCallback<Object>() {
//            public Object execute(RedisOperations operations) throws DataAccessException {
//                operations.opsForValue().set(RedisArenaKeys.ARENA_ONLINE_COUNT,count+"");
//                final ListOperations listOperations = operations.opsForList();
//
//                for (int i = 0; i < roomCount; i++) {
//                    listOperations.leftPush(RedisArenaKeys.ONLINE_ROOM_LIST,i+"");
//                }
//
//                for (int i = 0; i < ongoingRoomCount; i++) {
//                    listOperations.leftPush(RedisArenaKeys.ONGOING_ROOM_LIST,i+"");
//                }
//
//                return null;
//            }
//        });
//
//        final ArenaRoomSummary summary = arenaRoomService.summary();
//        Assert.assertEquals(summary.getGoingCount(),ongoingRoomCount);
//        Assert.assertEquals(summary.getPlayerCount(),count);
//        Assert.assertEquals(summary.getRoomCount(),roomCount);
//        Assert.assertEquals(summary.getFreeCount(),roomCount-ongoingRoomCount);
    }

    @Test
    public void findByIdTest(){
        final UserDto userDto = userDubboService.findById(12252065);
        System.out.println(userDto);
    }
}
