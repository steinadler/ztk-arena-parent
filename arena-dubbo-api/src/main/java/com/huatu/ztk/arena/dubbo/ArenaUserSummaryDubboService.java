package com.huatu.ztk.arena.dubbo;

import com.huatu.ztk.arena.bean.ArenaUserSummary;

/**
 * 用户竞技场统计信息dubbo服务
 * Created by renwenlong on 2016/10/17.
 */
public interface ArenaUserSummaryDubboService {

    /**
     * 根据uid查询用户竞技场统计(胜负场次)
     *
     * @param uid
     * @return
     */
    public ArenaUserSummary findSummaryById(long uid);
}
