package net.gddata.index.dao;

import net.gddata.index.model.Master201601;
import net.gddata.kw.tables.records.Master_201601Record;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static net.gddata.kw.tables.Master_201601.MASTER_201601;

/**
 * Created by zhangzf on 17/6/14.
 */
@Component
public class Master201601Dao extends JooqDao<Master_201601Record, Master201601, Integer> {
    protected Master201601Dao() {
        super(MASTER_201601, Master201601.class);
    }

    @Override
    protected Integer getId(Master201601 master201601) {
        return master201601.getId();
    }

    public List<Master201601> getDate() {
        Result<Record2<Integer, String>> fetch = create().select(MASTER_201601.ID, MASTER_201601.KEYWORDS2).from(MASTER_201601).where(MASTER_201601.REPLY_ON_ORGAN.like("%福州大学%")).fetch();
        return null != fetch ? fetch.into(Master201601.class) : new ArrayList<>();
    }

}
