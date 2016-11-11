package com.huatu.ztk.arena.task;

import com.google.common.collect.Lists;
import com.huatu.ztk.commons.exception.BizException;
import com.huatu.ztk.paper.api.PracticeCardDubboService;
import com.huatu.ztk.paper.bean.Answer;
import com.huatu.ztk.paper.bean.PracticeCard;
import com.huatu.ztk.paper.bean.PracticePaper;
import com.huatu.ztk.paper.bean.UserAnswers;
import com.huatu.ztk.question.api.QuestionDubboService;
import com.huatu.ztk.question.bean.GenericQuestion;
import com.huatu.ztk.question.bean.Question;
import com.huatu.ztk.question.common.QuestionCorrectType;
import com.huatu.ztk.question.common.QuestionType;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 *
 * robot 自动提交答案任务
 * Created by shaojieyue
 * Created time 2016-11-09 14:12
 */

@Component
@Scope("singleton")
public class RobotSubmitTask {
    private static final Logger logger = LoggerFactory.getLogger(RobotSubmitTask.class);

    @Autowired
    private QuestionDubboService questionDubboService;

    @Autowired
    private PracticeCardDubboService practiceCardDubboService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void addNewRobotPractice(PracticeCard practiceCard){
        //此处没有用线程池,是为了防止机器人做题被阻塞,机器人本身数量不多,所以可以不用线程池
        new Thread(new Runnable() {
            @Override
            public void run() {
                final PracticePaper paper = practiceCard.getPaper();
                //遍历程序,提交随机答案
                for (Integer questionId : paper.getQuestions()) {
                    //随机答题时间
                    final int time = RandomUtils.nextInt(10, 50);
                    int answer = 0;
                    final GenericQuestion question = (GenericQuestion)questionDubboService.findById(questionId);
                    if (RandomUtils.nextInt(1,100)%2 != 0) {//正确答案概率 66%
                        answer = question.getAnswer();
                    }else {
                        answer = RandomUtils.nextInt(1,5);
                        while (answer == question.getAnswer()){//循环获取,知道获取到错误答案为止
                            answer = RandomUtils.nextInt(1,5);
                        }
                    }
                    //计算试题是否做对
                    int correct = answer == question.getAnswer()?QuestionCorrectType.RIGHT: QuestionCorrectType.WRONG;

                    final Answer answer1 = new Answer();
                    answer1.setQuestionId(questionId);
                    answer1.setTime(time);
                    answer1.setCorrect(correct);
                    answer1.setAnswer(answer);
                    try {
                        //sleep 一段时间,表示做题时间
                        TimeUnit.SECONDS.sleep(time);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    final ArrayList<Answer> answers = Lists.newArrayList(answer1);
                    try {
                        practiceCardDubboService.submitAnswers(practiceCard.getId(),practiceCard.getUserId(),answers,false,-9);
                        //此处只发送新增加的答案,防止统计重复
                        final UserAnswers userAnswers = UserAnswers.builder()
                                .uid(practiceCard.getUserId())
                                .practiceId(practiceCard.getId())
                                .area(-9)
                                .subject(practiceCard.getSubject())
                                .submitTime(System.currentTimeMillis())
                                .answers(answers)
                                .build();
                        //发送提交答案的事件,exchange="" 说明将数据发送到队列
                        rabbitTemplate.convertAndSend("","submit_answers_queue_arena",userAnswers);
                    } catch (BizException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    //提交试卷
                    practiceCardDubboService.submitAnswers(practiceCard.getId(),practiceCard.getUserId(),Lists.newArrayList(),true,-9);
                } catch (BizException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
