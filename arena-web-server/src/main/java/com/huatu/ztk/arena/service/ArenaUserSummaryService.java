package com.huatu.ztk.arena.service;

import com.huatu.ztk.arena.bean.ArenaUserSummary;
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
     * @param isWinner 用户是否是胜者
     */
    public void updateUserSummary(long uid,boolean isWinner){
        String key = isWinner ? "winCount" : "failCount";
        Update update = new Update().inc(key,1);

        //更新用户当天统计
        updateUserSummary(uid, isWinner, update, arenaUserSummaryDao.getTodaySummaryId(uid));

        //更新用户总记录统计
        updateUserSummary(uid, isWinner, update, arenaUserSummaryDao.getTotalSummaryId(uid));
    }

    private void updateUserSummary(long uid, boolean isWinner, Update update, String summaryId) {
        logger.info("update summary,uid={},isWinner={},summaryId={}", uid, isWinner, summaryId);
        final boolean isExist = arenaUserSummaryDao.updateSummary(summaryId, update);

        //如果不存在，新建记录
        if (!isExist) {
            ArenaUserSummary todaySummary = ArenaUserSummary.builder()
                    .id(summaryId)
                    .uid(uid)
                    .winCount(isWinner ? 1 : 0)
                    .failCount(isWinner ? 0 : 1)
                    .build();
            arenaUserSummaryDao.insertSummary(todaySummary);
        }
    }
}
