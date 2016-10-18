package com.huatu.ztk.arena;

import com.huatu.ztk.arena.bean.ArenaUserSummary;
import com.huatu.ztk.arena.dao.ArenaUserSummaryDao;
import com.huatu.ztk.commons.JsonUtil;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by renwenlong on 2016/10/17.
 */
public class ArenaUserSummaryDubboServiceImplTest extends BaseTest {
    private static final Logger logger = LoggerFactory.getLogger(ArenaUserSummaryDubboServiceImplTest.class);

    @Autowired
    private ArenaUserSummaryDao arenaUserSummarydao;

    @Test
    public void findSummaryByIdTest(){
        String  id = "10264614-1"; //用户胜负数据统计id格式：uid+"-1"
        final ArenaUserSummary arenaUserSummary = arenaUserSummarydao.findById(id);
//        Assert.assertNotNull(arenaUserSummary);
//        Assert.assertEquals(arenaUserSummary.getFailCount(),0);
//        Assert.assertEquals(arenaUserSummary.getWinCount(),2);
//        logger.info("arenaUserSummary={}", JsonUtil.toJson(arenaUserSummary));
    }

}