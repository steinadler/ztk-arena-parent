package com.huatu.ztk.arena.task;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;
import com.huatu.ztk.commons.Area;
import com.huatu.ztk.commons.AreaConstants;
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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;
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
    private static final Map<Integer,Integer> POINT_RATIO_MAP = Maps.newHashMap();
    private static final Map<Integer,PointHistogram> POINT_HISTOGRAM_MAP = Maps.newHashMap();
    static {
        InputStream input = AreaConstants.class.getClassLoader().getResourceAsStream("point_ratio.csv");
        try {
            //加载point_ratio.csv数据
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
            bufferedReader.lines().forEach(line ->{
                final String[] strs = line.split(",");
                if (strs.length == 4) {
                    int pointId = Integer.valueOf(strs[0]);
                    int ratio = new BigDecimal(strs[3]).multiply(BigDecimal.valueOf(100)).intValue();
                    POINT_RATIO_MAP.put(pointId,ratio);
                }
            });

            input = AreaConstants.class.getClassLoader().getResourceAsStream("point_histogram.csv");
            bufferedReader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
            bufferedReader.lines().forEach(line ->{
                final String[] strs = line.split(",");
                if (strs.length == 5) {
                    //775,32,57,101,157
                    final PointHistogram pointHistogram = new PointHistogram(Ints.tryParse(strs[0]), Ints.tryParse(strs[1]), Ints.tryParse(strs[2]), Ints.tryParse(strs[3]), Ints.tryParse(strs[4]));
                    POINT_HISTOGRAM_MAP.put(pointHistogram.getPointId(),pointHistogram);
                }
            });
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    static class PointHistogram{
        private int pointId;
        private int a25;
        private int a50;
        private int a75;
        private int a95;

        public PointHistogram(int pointId, int a25, int a50, int a75, int a95) {
            this.pointId = pointId;
            this.a25 = a25;
            this.a50 = a50;
            this.a75 = a75;
            this.a95 = a95;
        }

        public int getPointId() {
            return pointId;
        }

        public int getA25() {
            return a25;
        }

        public int getA50() {
            return a50;
        }

        public int getA75() {
            return a75;
        }

        public int getA95() {
            return a95;
        }
    }

    public void addNewRobotPractice(PracticeCard practiceCard){
        //此处没有用线程池,是为了防止机器人做题被阻塞,机器人本身数量不多,所以可以不用线程池
        new Thread(new Runnable() {
            @Override
            public void run() {
                final PracticePaper paper = practiceCard.getPaper();
                //遍历程序,提交随机答案
                for (Integer questionId : paper.getQuestions()) {
                    int answer = 0;
                    final GenericQuestion question = (GenericQuestion)questionDubboService.findById(questionId);
                    final Integer pointId = question.getPoints().get(2);
                    final Integer ratio = POINT_RATIO_MAP.getOrDefault(pointId, 50);
                    if (RandomUtils.nextInt(1,100)+15 < ratio) {//正确答案概率
                        answer = question.getAnswer();
                    }else {
                        answer = RandomUtils.nextInt(1,5);
                        while (answer == question.getAnswer()){//循环获取,知道获取到错误答案为止
                            answer = RandomUtils.nextInt(1,5);
                        }
                    }
                    //计算试题是否做对
                    int correct = answer == question.getAnswer()?QuestionCorrectType.RIGHT: QuestionCorrectType.WRONG;
                    final int i = RandomUtils.nextInt(0, 10);
                    int maxTime = 10;
                    int minTime = 50;
                    final PointHistogram pointHistogram = POINT_HISTOGRAM_MAP.get(pointId);
                    if (pointHistogram!=null) {
                        if (i < 3) {//25%
                            maxTime = pointHistogram.getA25();
                            minTime = Math.min(7,maxTime);
                        }else if (i >= 3 && i < 7) {
                            maxTime = pointHistogram.getA50();
                            minTime = pointHistogram.getA25();
                        }else {
                            maxTime = pointHistogram.getA75();
                            minTime = pointHistogram.getA50();
                        }
                    }
                    //随机答题时间
                    final int time = RandomUtils.nextInt(minTime*4/5, maxTime*3/5);
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
