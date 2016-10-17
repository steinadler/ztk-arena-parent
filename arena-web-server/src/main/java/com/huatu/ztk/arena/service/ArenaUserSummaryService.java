package com.huatu.ztk.arena.service;

import com.huatu.ztk.arena.dao.ArenaUserSummaryDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

/**
 * Created by shaojieyue
 * Created time 2016-10-17 17:03
 */

@Service
public class ArenaUserSummaryService {
    private static final Logger logger = LoggerFactory.getLogger(ArenaUserSummaryService.class);

    @Autowired
    private ArenaUserSummaryDao arenaUserSummaryDao;

    /**
     * 更新用户竞技统计
     * @param uid 用户id
     * @param winner 用户是否是胜者
     */
    public void updateUserSummary(long uid,boolean winner){
        // TODO: 10/17/16 是否存在
        String key = null;
        if (winner) {
            key = "winCount";
        }else {
            key = "failCount";
        }
        Update update = new Update().inc(key,1);
        //更新用户当天统计
        final boolean b = arenaUserSummaryDao.updateSummary(arenaUserSummaryDao.getTodaySummaryId(uid), update);
        if (!b) {
            //init
        }
        //更新用户总记录统计
        arenaUserSummaryDao.updateSummary(arenaUserSummaryDao.getTotalSummaryId(uid),update);
    }
}
