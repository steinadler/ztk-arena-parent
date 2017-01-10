package com.huatu.ztk.arena;

import com.huatu.ztk.arena.task.RobotSubmitTask;
import com.huatu.ztk.paper.api.PracticeCardDubboService;
import com.huatu.ztk.paper.bean.AnswerCard;
import com.huatu.ztk.paper.bean.PracticeCard;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

/**
 * Created by shaojieyue
 * Created time 2017-01-10 17:42
 */
public class RobotSubmitTaskTest extends BaseTest {

    @Autowired
    private RobotSubmitTask robotSubmitTask;

    @Autowired
    private PracticeCardDubboService practiceCardDubboService;

    @Test
    public void addNewRobotPracticeTest(){
        long id = 24330228L;
        final AnswerCard answerCard = practiceCardDubboService.findById(id);
        robotSubmitTask.addNewRobotPractice((PracticeCard)answerCard);
        try {
            TimeUnit.SECONDS.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
