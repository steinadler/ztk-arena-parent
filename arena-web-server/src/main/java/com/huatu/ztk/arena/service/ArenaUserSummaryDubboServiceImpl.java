package com.huatu.ztk.arena.service;

import com.huatu.ztk.arena.bean.ArenaUserSummary;
import com.huatu.ztk.arena.dao.ArenaUserSummaryDao;
import com.huatu.ztk.arena.dubbo.ArenaUserSummaryDubboService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by renwenlong on 2016/10/17.
 */
public class ArenaUserSummaryDubboServiceImpl implements ArenaUserSummaryDubboService {
    private static final Logger logger = LoggerFactory.getLogger(ArenaUserSummaryDubboServiceImpl.class);

    @Autowired
    private ArenaUserSummaryDao arenaUserSummarydao;

    /**
     * 根据uid查询该用户的竞技统计
     *
     * @param uid
     * @return
     */
    @Override
    public ArenaUserSummary findSummaryById(long uid) {
        final ArenaUserSummary arenaUserSummary = arenaUserSummarydao.findById(getTotalSummaryId(uid));
        // TODO: 2016/10/18  上线放开代码
/*        if (arenaUserSummary == null) {
            logger.info("find arenaUserSummary is null，uid={}",uid);
            return arenaUserSummary;
        }*/

        if (arenaUserSummary == null) {
            final ArenaUserSummary userSummary = ArenaUserSummary.builder()
                    .failCount(18)
                    .winCount(200)
                    .uid(uid)
                    .build();
            return userSummary;
        }
        return arenaUserSummary;
    }

    /**
     * 获取用户在mongo中竞技统计的id
     * @param uid
     * @return
     */
    private String getTotalSummaryId(long uid) {
        return uid + "-1";
    }
}
