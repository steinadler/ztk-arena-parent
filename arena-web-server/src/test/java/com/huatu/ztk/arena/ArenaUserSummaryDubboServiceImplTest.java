package com.huatu.ztk.arena;

import com.huatu.ztk.arena.dao.ArenaUserSummaryDao;
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

//    @Test
//    public void findSummaryByIdTest(){
//        String  id = "13117013-1"; //用户胜负数据统计id格式：uid+"-1"
//        final ArenaUserSummary arenaUserSummary = arenaUserSummarydao.findById(id);
//        Assert.assertNotNull(arenaUserSummary);
//        logger.info("arenaUserSummary={}", JsonUtil.toJson(arenaUserSummary));
//    }

}