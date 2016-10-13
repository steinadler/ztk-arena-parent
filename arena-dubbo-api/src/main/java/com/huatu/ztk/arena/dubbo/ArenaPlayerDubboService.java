package com.huatu.ztk.arena.dubbo;

import com.huatu.ztk.arena.bean.Player;

/**
 * 玩家dubbo服务
 * Created by shaojieyue
 * Created time 2016-10-13 14:46
 */
public interface ArenaPlayerDubboService {
    /**
     * 根据id查询玩家
     * @param uid
     * @return
     */
    public Player findById(long uid);
}
