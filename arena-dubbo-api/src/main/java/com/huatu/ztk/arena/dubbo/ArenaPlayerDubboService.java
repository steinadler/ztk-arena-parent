package com.huatu.ztk.arena.dubbo;

import com.huatu.ztk.arena.bean.ArenaUserSummary;
import com.huatu.ztk.arena.bean.Player;

import java.util.List;

/**
 * 玩家dubbo服务
 * Created by shaojieyue
 * Created time 2016-10-13 14:46
 */
public interface ArenaPlayerDubboService {

    /**
     * 根据uid查询玩家
     * @param uid
     * @return
     */
    public Player findById(long uid);

    /**
     * 批量查询用户id
     * id 列表和返回的结果集一一对应
     * @param uids
     * @return
     */
    public List<Player> findBatch(List<Long> uids);

}
