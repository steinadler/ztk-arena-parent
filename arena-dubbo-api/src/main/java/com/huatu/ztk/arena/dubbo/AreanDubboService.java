package com.huatu.ztk.arena.dubbo;
import com.huatu.ztk.arena.bean.ArenaRoom;
import org.springframework.data.mongodb.core.query.Update;

/**
 * Created by shaojieyue
 * Created time 2016-10-08 19:57
 */
public interface AreanDubboService {

    /**
     * 根据id查询房间
     * @param id
     * @return
     */
    ArenaRoom findById(long id);

    /**
     * 根据id 更新指定房间的数据
     * @param id
     * @param update
     */
    void updateById(long id, Update update);
}
