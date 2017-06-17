package net.gddata.index.dao;

import net.gddata.index.model.View;
import net.gddata.kw.tables.records.ViewRecord;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static net.gddata.kw.tables.Master_201601.MASTER_201601;
import static net.gddata.kw.tables.View.VIEW;

/**
 * Created by zhangzf on 17/6/15.
 */
@Component
public class ViewDao extends JooqDao<ViewRecord, View, Integer> {
    protected ViewDao() {
        super(VIEW, View.class);
    }

    @Override
    protected Integer getId(View view) {
        return view.getId();
    }

    public Integer getTotal() {
        return create().select(DSL.count()).from(VIEW).fetchOne().value1();
    }

    public Integer getItemCount(String t) {
        String sql = "select count(*) from kw.view where " + t + " >0;";
        Integer into = create().fetchOne(sql).into(Integer.class);
        return into;
    }

    public List<View> getRetrieve3(Integer num) {
        Result<ViewRecord> fetch = create().selectFrom(VIEW).limit(num).fetch();
        if (null != fetch) {
            return fetch.into(View.class);
        } else {
            return new ArrayList<>();
        }
    }
}
