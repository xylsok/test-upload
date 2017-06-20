package net.gddata.index.dao;

import net.gddata.index.model.View;
import net.gddata.kw.tables.records.View5Record;
import net.gddata.kw.tables.records.ViewRecord;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static net.gddata.kw.tables.View5.VIEW5;

/**
 * Created by zhangzf on 17/6/15.
 */
@Component
public class View5Dao extends JooqDao<View5Record, View, Integer> {
    protected View5Dao() {
        super(VIEW5, View.class);
    }

    @Override
    protected Integer getId(View view) {
        return view.getId();
    }

    public Integer getTotal() {
        return create().select(DSL.count()).from(VIEW5).fetchOne().value1();
    }

    public Integer getItemCount(String t) {
        String sql = "select count(*) from kw.view5 where " + t + " >0;";
        Integer into = create().fetchOne(sql).into(Integer.class);
        return into;
    }

    public List<View> getRetrieve3(Integer num) {
        Result<View5Record> fetch = create().selectFrom(VIEW5).limit(num).fetch();
        if (null != fetch) {
            return fetch.into(View.class);
        } else {
            return new ArrayList<>();
        }
    }
}
