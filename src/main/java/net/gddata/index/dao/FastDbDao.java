package net.gddata.index.dao;

import net.gddata.index.model.FastDb;
import net.gddata.yoga.tables.records.FastDbRecord;
import org.springframework.stereotype.Component;

/**
 * Created by zhangzf on 17/6/26.
 */
@Component
public class FastDbDao extends JooqDao<FastDbRecord,FastDb,Integer>{
    protected FastDbDao() {
        super(net.gddata.yoga.tables.FastDb.FAST_DB, FastDb.class);
    }

    @Override
    protected Integer getId(FastDb fastDb) {
        return fastDb.getId();
    }
}
