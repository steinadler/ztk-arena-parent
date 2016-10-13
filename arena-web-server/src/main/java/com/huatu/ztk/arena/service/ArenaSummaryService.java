package com.huatu.ztk.arena.service;

import com.huatu.ztk.arena.bean.ArenaUserSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 竞技场统计
 * Created by shaojieyue
 * Created time 2016-10-13 21:26
 */

@Service
public class ArenaSummaryService {
    private static final Logger logger = LoggerFactory.getLogger(ArenaSummaryService.class);

    /**
     * 根据用户id查询该用户的竞技统计
     * @param uid
     * @return
     */
    public ArenaUserSummary findByUid(long uid) {
        final ArenaUserSummary userSummary = ArenaUserSummary.builder()
                .failCount(100)
                .winCount(236)
                .uid(uid)
                .build();

        return userSummary;
    }
}
