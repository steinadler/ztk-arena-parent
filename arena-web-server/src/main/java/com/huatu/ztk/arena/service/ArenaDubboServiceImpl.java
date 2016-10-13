package com.huatu.ztk.arena.service;

import com.huatu.ztk.arena.bean.ArenaRoom;
import com.huatu.ztk.arena.dao.ArenaRoomDao;
import com.huatu.ztk.arena.dubbo.ArenaDubboService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Update;

/**
 * Created by shaojieyue
 * Created time 2016-10-09 13:37
 */
public class ArenaDubboServiceImpl implements ArenaDubboService {
    private static final Logger logger = LoggerFactory.getLogger(ArenaDubboServiceImpl.class);

    @Autowired
    private ArenaRoomDao arenaRoomDao;

    /**
     * 根据id查询房间
     *
     * @param id
     * @return
     */
    @Override
    public ArenaRoom findById(long id) {
        return arenaRoomDao.findById(id);
    }

    /**
     * 根据id 更新指定房间的数据
     *
     * @param id
     * @param update
     */
    @Override
    public void updateById(long id, Update update) {
        arenaRoomDao.updateById(id,update);
        logger.info("roomId={} update info,data={}",id,update);
    }
}
