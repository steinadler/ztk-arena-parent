package com.huatu.ztk.arena.service;

import com.google.common.collect.Lists;
import com.huatu.ztk.arena.bean.ArenaUserSummary;
import com.huatu.ztk.arena.bean.Player;
import com.huatu.ztk.arena.dao.ArenaUserSummaryDao;
import com.huatu.ztk.arena.dubbo.ArenaPlayerDubboService;
import com.huatu.ztk.user.bean.UserDto;
import com.huatu.ztk.user.dubbo.UserDubboService;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by shaojieyue
 * Created time 2016-10-13 14:47
 */
public class ArenaPlayerDubboServiceImpl implements ArenaPlayerDubboService {
    private static final Logger logger = LoggerFactory.getLogger(ArenaPlayerDubboServiceImpl.class);

    @Autowired
    private UserDubboService userDubboService;

    /**
     * 根据id查询玩家
     *
     * @param uid
     * @return
     */
    @Override
    public Player findById(long uid) {
        final UserDto userDto = userDubboService.findById(uid);
        // TODO: 10/24/16 如果此处性能有问题,可以考虑玩家信息暂时写入缓存
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
     * 批量查询用户id
     * id 列表和返回的结果集一一对应
     *
     * @param uids
     * @return
     */
    @Override
    public List<Player> findBatch(List<Long> uids) {
        if (CollectionUtils.isEmpty(uids)) {
            return Lists.newArrayList();
        }
        //stream 可以保证顺序
        List<Player> players = uids.stream().map(uid -> findById(uid) ).collect(Collectors.toList());
        return players;
    }
}
