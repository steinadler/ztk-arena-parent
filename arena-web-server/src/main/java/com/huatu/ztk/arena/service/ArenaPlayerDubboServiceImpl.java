package com.huatu.ztk.arena.service;

import com.huatu.ztk.arena.bean.ArenaUserSummary;
import com.huatu.ztk.arena.bean.Player;
import com.huatu.ztk.arena.dao.ArenaUserSummaryDao;
import com.huatu.ztk.arena.dubbo.ArenaPlayerDubboService;
import com.huatu.ztk.user.bean.UserDto;
import com.huatu.ztk.user.dubbo.UserDubboService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by shaojieyue
 * Created time 2016-10-13 14:47
 */
public class ArenaPlayerDubboServiceImpl implements ArenaPlayerDubboService {
    private static final Logger logger = LoggerFactory.getLogger(ArenaPlayerDubboServiceImpl.class);

    @Autowired
    private UserDubboService userDubboService;

    @Autowired
    private ArenaUserSummaryDao arenaUserSummarydao;

    /**
     * 根据id查询玩家
     *
     * @param uid
     * @return
     */
    @Override
    public Player findById(long uid) {
        final UserDto userDto = userDubboService.findById(uid);
        if (userDto == null) {
            return null;
        }
        final Player player = Player.builder()
                .uid(userDto.getId())
                .avatar(userDto.getAvatar())
                .nick(userDto.getNick())
                .build();
        return player;
    }

    /**
     * 根据uid查询该用户的竞技统计
     * @param uid
     * @return
     */
    @Override
    public ArenaUserSummary findSummaryById(long uid) {
        final ArenaUserSummary arenaUserSummary = arenaUserSummarydao.findById(getTotalSummaryId(uid));
        if (arenaUserSummary == null) {
            // TODO: 10/17/16 模拟数据,后期去掉
        }
        return arenaUserSummary;
    }

    private String getTotalSummaryId(long uid){
        return uid+"-1";
    }
}
