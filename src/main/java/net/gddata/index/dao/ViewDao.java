package net.gddata.index.dao;

import net.gddata.index.model.View;
import net.gddata.kw.tables.records.ViewRecord;
import org.springframework.stereotype.Component;

import static net.gddata.kw.tables.View.VIEW;

/**
 * Created by zhangzf on 17/6/15.
 */
@Component
public class ViewDao extends JooqDao<ViewRecord,View,Integer>{
    protected ViewDao() {
        super(VIEW, View.class);
    }

    @Override
    protected Integer getId(View view) {
        return view.getId();
    }
}
